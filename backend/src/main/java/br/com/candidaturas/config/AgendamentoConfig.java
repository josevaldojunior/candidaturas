package br.com.candidaturas.config;

import br.com.candidaturas.service.InboxService;
import br.com.candidaturas.service.SemRetornoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Jobs agendados. Desligados nos testes com app.agendamento.habilitado=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.agendamento.habilitado", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoConfig.class);

    private final SemRetornoService semRetornoService;
    private final InboxService inboxService;

    public AgendamentoConfig(SemRetornoService semRetornoService, InboxService inboxService) {
        this.semRetornoService = semRetornoService;
        this.inboxService = inboxService;
    }

    @Scheduled(cron = "${app.sem-retorno.cron:0 0 6 * * *}", zone = "${app.zona:America/Sao_Paulo}")
    public void marcarSemRetorno() {
        try {
            semRetornoService.executar();
        } catch (RuntimeException e) {
            log.error("Falha no job de sem retorno", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.inbox.intervalo-ms:300000}", initialDelayString = "${app.inbox.atraso-inicial-ms:30000}")
    public void lerInbox() {
        try {
            inboxService.processar();
        } catch (RuntimeException e) {
            log.error("Falha ao processar a pasta inbox", e);
        }
    }
}
