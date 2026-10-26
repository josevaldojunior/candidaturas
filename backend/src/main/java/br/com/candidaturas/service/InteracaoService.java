package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Interacao;
import br.com.candidaturas.dto.InteracaoDtos;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoRequest;
import br.com.candidaturas.dto.InteracaoDtos.InteracaoResponse;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.InteracaoRepository;
import br.com.candidaturas.seguranca.UsuarioAtual;
import br.com.candidaturas.util.Normalizacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

@Service
public class InteracaoService {

    private final InteracaoRepository repository;
    private final CandidaturaRepository candidaturaRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public InteracaoService(InteracaoRepository repository, CandidaturaRepository candidaturaRepository,
                            UsuarioAtual usuarioAtual, Clock clock) {
        this.repository = repository;
        this.candidaturaRepository = candidaturaRepository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional
    public InteracaoResponse adicionar(Long candidaturaId, InteracaoRequest req) {
        Candidatura c = candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura", candidaturaId));
        LocalDateTime agora = LocalDateTime.now(clock);
        Interacao i = new Interacao(req.data() != null ? req.data() : agora, req.tipo(),
                Normalizacao.texto(req.descricao()));
        i.setProximoPasso(Normalizacao.texto(req.proximoPasso()));
        i.setDataFollowUp(req.dataFollowUp());
        i.setFollowUpConcluido(Boolean.TRUE.equals(req.followUpConcluido()));
        c.adicionarInteracao(i);
        c.setAtualizadoEm(agora);
        return InteracaoDtos.response(repository.save(i));
    }

    @Transactional
    public InteracaoResponse atualizar(Long id, InteracaoRequest req) {
        Interacao i = obter(id);
        if (req.data() != null) {
            i.setData(req.data());
        }
        i.setTipo(req.tipo());
        i.setDescricao(Normalizacao.texto(req.descricao()));
        i.setProximoPasso(Normalizacao.texto(req.proximoPasso()));
        i.setDataFollowUp(req.dataFollowUp());
        if (req.followUpConcluido() != null) {
            i.setFollowUpConcluido(req.followUpConcluido());
        }
        recalcularUltimaInteracao(i.getCandidatura());
        return InteracaoDtos.response(i);
    }

    @Transactional
    public void excluir(Long id) {
        Interacao i = obter(id);
        Candidatura c = i.getCandidatura();
        c.getInteracoes().remove(i);
        recalcularUltimaInteracao(c);
    }

    @Transactional
    public InteracaoResponse concluirFollowUp(Long id, boolean concluido) {
        Interacao i = obter(id);
        i.setFollowUpConcluido(concluido);
        return InteracaoDtos.response(i);
    }

    private Interacao obter(Long id) {
        return repository.findByIdAndCandidaturaUsuarioId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Interação", id));
    }

    private void recalcularUltimaInteracao(Candidatura c) {
        c.setUltimaInteracaoEm(c.getInteracoes().stream()
                .map(Interacao::getData)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        c.setAtualizadoEm(LocalDateTime.now(clock));
    }
}
