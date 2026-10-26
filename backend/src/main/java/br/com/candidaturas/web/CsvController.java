package br.com.candidaturas.web;

import br.com.candidaturas.domain.Origem;
import br.com.candidaturas.dto.FiltroCandidaturas;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import org.springdoc.core.annotations.ParameterObject;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoInbox;
import br.com.candidaturas.service.CandidaturaService;
import br.com.candidaturas.service.CsvService;
import br.com.candidaturas.service.InboxService;
import br.com.candidaturas.service.IntegracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

@RestController
@Tag(name = "CSV e inbox")
public class CsvController {

    private final CsvService csvService;
    private final CandidaturaService candidaturaService;
    private final IntegracaoService integracaoService;
    private final InboxService inboxService;
    private final Clock clock;

    public CsvController(CsvService csvService, CandidaturaService candidaturaService,
                         IntegracaoService integracaoService, InboxService inboxService, Clock clock) {
        this.csvService = csvService;
        this.candidaturaService = candidaturaService;
        this.integracaoService = integracaoService;
        this.inboxService = inboxService;
        this.clock = clock;
    }

    @GetMapping("/api/csv/exportar")
    @Operation(summary = "Exporta as candidaturas (aceita os mesmos filtros da listagem) no formato de importação")
    public void exportar(@ParameterObject FiltroCandidaturas filtro, HttpServletResponse response) throws IOException {
        var candidaturas = candidaturaService.listarTodas(filtro);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"candidaturas-" + LocalDate.now(clock) + ".csv\"");
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        csvService.escrever(writer, candidaturas);
    }

    @PostMapping(value = "/api/csv/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa um CSV (origem IMPORTACAO) com a mesma regra de upsert da integração")
    public ResultadoImportacao importar(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream()) {
            return integracaoService.processar(csvService.ler(in), Origem.IMPORTACAO);
        }
    }

    @PostMapping("/api/inbox/processar")
    @Operation(summary = "Processa agora os CSVs da pasta inbox (o job roda a cada 5 minutos)")
    public ResultadoInbox processarInbox() {
        return inboxService.processar();
    }
}
