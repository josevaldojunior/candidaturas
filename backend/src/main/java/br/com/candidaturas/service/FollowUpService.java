package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.dto.FollowUpDtos.FollowUpItem;
import br.com.candidaturas.dto.FollowUpDtos.FollowUpsResponse;
import br.com.candidaturas.repository.InteracaoRepository;
import br.com.candidaturas.seguranca.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class FollowUpService {

    static final int DIAS_A_FRENTE = 7;

    private final InteracaoRepository repository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public FollowUpService(InteracaoRepository repository, UsuarioAtual usuarioAtual, Clock clock) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    /** Follow-ups não concluídos: vencidos (antes de hoje) e de hoje até daqui a 7 dias. */
    @Transactional(readOnly = true)
    public FollowUpsResponse pendentes() {
        LocalDate hoje = LocalDate.now(clock);
        List<FollowUpItem> vencidos = new ArrayList<>();
        List<FollowUpItem> proximos = new ArrayList<>();
        for (Interacao i : repository.followUpsPendentesAte(usuarioAtual.id(), hoje.plusDays(DIAS_A_FRENTE))) {
            FollowUpItem item = item(i, hoje);
            if (i.getDataFollowUp().isBefore(hoje)) {
                vencidos.add(item);
            } else {
                proximos.add(item);
            }
        }
        return new FollowUpsResponse(vencidos, proximos);
    }

    private static FollowUpItem item(Interacao i, LocalDate hoje) {
        Candidatura c = i.getCandidatura();
        return new FollowUpItem(i.getId(), c.getId(), c.getEmpresa(), c.getCargo(), c.getStatus(), i.getTipo(),
                i.getDescricao(), i.getProximoPasso(), i.getDataFollowUp(),
                ChronoUnit.DAYS.between(hoje, i.getDataFollowUp()));
    }
}
