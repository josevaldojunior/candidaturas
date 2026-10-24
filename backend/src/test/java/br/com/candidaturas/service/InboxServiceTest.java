package br.com.candidaturas.service;

import br.com.candidaturas.domain.Origem;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoInbox;
import br.com.candidaturas.support.RelogioFixo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @TempDir
    Path inbox;

    @Mock
    private IntegracaoService integracaoService;

    private InboxService service;

    @BeforeEach
    void setUp() {
        service = new InboxService(inbox.toString(), new CsvService(), integracaoService, RelogioFixo.clock());
    }

    @Test
    void importaEMoveParaProcessados() throws Exception {
        Files.writeString(inbox.resolve("lote.CSV"), "empresa,cargo\nACME,Java\n", StandardCharsets.UTF_8);
        Files.writeString(inbox.resolve("leia-me.txt"), "não é csv");
        when(integracaoService.processar(any(), eq(Origem.AGENTE))).thenReturn(new ResultadoImportacao(1, 0, 0, List.of()));

        ResultadoInbox r = service.processar();

        assertThat(r.arquivos()).singleElement().satisfies(a -> {
            assertThat(a.arquivo()).isEqualTo("lote.CSV");
            assertThat(a.destino()).isEqualTo("processados/20261024-100000_lote.CSV");
        });
        assertThat(inbox.resolve("lote.CSV")).doesNotExist();
        assertThat(inbox.resolve("processados/20261024-100000_lote.CSV")).exists();
        assertThat(inbox.resolve("leia-me.txt")).exists();
        verify(integracaoService).processar(argThat(itens -> itens.size() == 1 && "ACME".equals(itens.get(0).empresa())),
                eq(Origem.AGENTE));
    }

    @Test
    void comErrosVaiParaPastaErrosComRelatorio() throws Exception {
        Files.writeString(inbox.resolve("parcial.csv"), "empresa,cargo\nACME,Java\n,Sem empresa\n");
        when(integracaoService.processar(any(), any()))
                .thenReturn(new ResultadoImportacao(1, 0, 0, List.of("Item 2: empresa é obrigatória")));

        service.processar();

        Path destino = inbox.resolve("erros/20261024-100000_parcial.csv");
        assertThat(destino).exists();
        assertThat(inbox.resolve("erros/20261024-100000_parcial.csv.erros.txt"))
                .content(StandardCharsets.UTF_8).contains("Item 2: empresa é obrigatória");
    }

    @Test
    void arquivoInvalidoTambemVaiParaErros() throws Exception {
        Files.writeString(inbox.resolve("ruim.csv"), "nome;link\nx;y\n");

        ResultadoInbox r = service.processar();

        assertThat(r.arquivos()).singleElement()
                .satisfies(a -> assertThat(a.resultado().erros()).singleElement().asString().startsWith("Arquivo: Cabeçalho inválido"));
        assertThat(inbox.resolve("erros/20261024-100000_ruim.csv")).exists();
        verifyNoInteractions(integracaoService);
    }

    @Test
    void pastaVaziaNaoFazNada() {
        assertThat(service.processar().arquivos()).isEmpty();
        assertThat(inbox.resolve("processados")).isDirectory();
        assertThat(inbox.resolve("erros")).isDirectory();
    }
}
