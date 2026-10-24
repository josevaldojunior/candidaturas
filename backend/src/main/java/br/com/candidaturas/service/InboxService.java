package br.com.candidaturas.service;

import br.com.candidaturas.domain.Origem;
import br.com.candidaturas.dto.IntegracaoDtos.ArquivoProcessado;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Importa os *.csv deixados pelo agente na pasta inbox e move cada arquivo para
 * inbox/processados (sem erros) ou inbox/erros (com um .erros.txt ao lado).
 */
@Service
public class InboxService {

    private static final Logger log = LoggerFactory.getLogger(InboxService.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path pasta;
    private final CsvService csvService;
    private final IntegracaoService integracaoService;
    private final Clock clock;

    public InboxService(@Value("${app.inbox.dir:./inbox}") String pasta, CsvService csvService,
                        IntegracaoService integracaoService, Clock clock) {
        this.pasta = Path.of(pasta).toAbsolutePath().normalize();
        this.csvService = csvService;
        this.integracaoService = integracaoService;
        this.clock = clock;
    }

    public synchronized ResultadoInbox processar() {
        List<ArquivoProcessado> processados = new ArrayList<>();
        Path ok = pasta.resolve("processados");
        Path falha = pasta.resolve("erros");
        try {
            Files.createDirectories(ok);
            Files.createDirectories(falha);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível preparar a pasta inbox em " + pasta, e);
        }
        for (Path arquivo : arquivosCsv()) {
            String nome = arquivo.getFileName().toString();
            ResultadoImportacao resultado;
            try (InputStream in = Files.newInputStream(arquivo)) {
                resultado = integracaoService.processar(csvService.ler(in), Origem.AGENTE);
            } catch (IOException e) {
                // provavelmente o agente ainda está escrevendo: tenta de novo na próxima rodada
                log.warn("Inbox: não foi possível ler {} agora: {}", nome, e.getMessage());
                continue;
            } catch (RuntimeException e) {
                resultado = new ResultadoImportacao(0, 0, 0, List.of("Arquivo: " + e.getMessage()));
            }
            boolean semErros = resultado.erros().isEmpty();
            Path destino = (semErros ? ok : falha).resolve(LocalDateTime.now(clock).format(CARIMBO) + "_" + nome);
            try {
                Files.move(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
                if (!semErros) {
                    Files.writeString(destino.resolveSibling(destino.getFileName() + ".erros.txt"),
                            String.join(System.lineSeparator(), resultado.erros()), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                log.warn("Inbox: importei {} mas não consegui movê-lo: {}", nome, e.getMessage());
                continue;
            }
            String relativo = pasta.relativize(destino).toString().replace('\\', '/');
            log.info("Inbox: {} -> {} ({} criadas, {} atualizadas, {} ignoradas, {} erros)", nome, relativo,
                    resultado.criadas(), resultado.atualizadas(), resultado.ignoradas(), resultado.erros().size());
            processados.add(new ArquivoProcessado(nome, relativo, resultado));
        }
        return new ResultadoInbox(processados);
    }

    public Path pasta() {
        return pasta;
    }

    private List<Path> arquivosCsv() {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível listar a pasta inbox " + pasta, e);
        }
    }
}
