package br.com.candidaturas.config;

import br.com.candidaturas.domain.Usuario;
import br.com.candidaturas.repository.UsuarioRepository;
import br.com.candidaturas.seguranca.UsuarioAtual;
import br.com.candidaturas.service.InboxService;
import br.com.candidaturas.service.SemRetornoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Locale;

/**
 * Jobs agendados. Rodam fora de uma requisição, então cada execução acontece "em nome" de uma conta.
 * Desligados nos testes com app.agendamento.habilitado=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.agendamento.habilitado", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoConfig.class);

    private final SemRetornoService semRetornoService;
    private final InboxService inboxService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;
    private final String emailDonoInbox;

    public AgendamentoConfig(SemRetornoService semRetornoService, InboxService inboxService,
                             UsuarioRepository usuarioRepository, UsuarioAtual usuarioAtual,
                             @Value("${app.inbox.usuario:}") String emailDonoInbox) {
        this.semRetornoService = semRetornoService;
        this.inboxService = inboxService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
        this.emailDonoInbox = emailDonoInbox == null ? "" : emailDonoInbox.trim().toLowerCase(Locale.ROOT);
    }

    /** Todo dia: aplica o prazo de "sem retorno" de cada conta; a falha de uma conta não para as outras. */
    @Scheduled(cron = "${app.sem-retorno.cron:0 0 18 * * *}", zone = "${app.zona:America/Sao_Paulo}")
    public void marcarSemRetorno() {
        for (Usuario u : usuarioRepository.findAll()) {
            try {
                usuarioAtual.como(u.getId(), u.getEmail(), semRetornoService::executar);
            } catch (RuntimeException e) {
                log.error("Falha no job de sem retorno do usuário {}", u.getId(), e);
            }
        }
    }

    /** A pasta inbox é de uma conta só (app.inbox.usuario); sem ela configurada, o job não faz nada. */
    @Scheduled(fixedDelayString = "${app.inbox.intervalo-ms:300000}", initialDelayString = "${app.inbox.atraso-inicial-ms:30000}")
    public void lerInbox() {
        if (emailDonoInbox.isEmpty()) {
            return;
        }
        var dono = usuarioRepository.findByEmail(emailDonoInbox);
        if (dono.isEmpty()) {
            log.warn("Pasta inbox ignorada: nenhuma conta com o e-mail {} (INBOX_USUARIO)", emailDonoInbox);
            return;
        }
        try {
            usuarioAtual.como(dono.get().getId(), dono.get().getEmail(), inboxService::processar);
        } catch (RuntimeException e) {
            log.error("Falha ao processar a pasta inbox", e);
        }
    }
}
