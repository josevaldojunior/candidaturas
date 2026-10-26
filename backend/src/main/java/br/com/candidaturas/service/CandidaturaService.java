package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.CandidaturaDtos.*;
import br.com.candidaturas.dto.CandidaturaDtos;
import br.com.candidaturas.dto.Pagina;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.CandidaturaSpecs;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.seguranca.UsuarioAtual;
import br.com.candidaturas.util.Normalizacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CandidaturaService {

    private final CandidaturaRepository repository;
    private final ContatoRepository contatoRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public CandidaturaService(CandidaturaRepository repository, ContatoRepository contatoRepository,
                              UsuarioAtual usuarioAtual, Clock clock) {
        this.repository = repository;
        this.contatoRepository = contatoRepository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Pagina<CandidaturaResumo> listar(List<Status> status, String empresa, List<Plataforma> plataformas,
                                           String stack, List<Prioridade> prioridades, LocalDate de, LocalDate ate,
                                           String texto, Boolean soPendentes, Pageable pageable) {
        var spec = CandidaturaSpecs.filtrar(usuarioAtual.id(), status, empresa, plataformas, stack, prioridades,
                de, ate, texto, soPendentes);
        return Pagina.de(repository.findAll(spec, pageable), CandidaturaDtos::resumo);
    }

    @Transactional(readOnly = true)
    public List<Candidatura> listarTodas(List<Status> status, String empresa, List<Plataforma> plataformas,
                                         String stack, List<Prioridade> prioridades, LocalDate de, LocalDate ate,
                                         String texto, Boolean soPendentes) {
        var spec = CandidaturaSpecs.filtrar(usuarioAtual.id(), status, empresa, plataformas, stack, prioridades,
                de, ate, texto, soPendentes);
        List<Candidatura> todas = repository.findAll(spec, Sort.by("id"));
        // inicializa coleções usadas na exportação enquanto a sessão está aberta
        todas.forEach(c -> {
            c.getStack().size();
            c.getContatos().size();
            c.getInteracoes().size();
        });
        return todas;
    }

    @Transactional(readOnly = true)
    public CandidaturaDetalhe buscar(Long id) {
        return CandidaturaDtos.detalhe(obter(id));
    }

    @Transactional(readOnly = true)
    public List<ColunaKanban> kanban() {
        Map<Status, List<CandidaturaResumo>> porStatus = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            porStatus.put(s, new ArrayList<>());
        }
        repository.findByUsuarioId(usuarioAtual.id(), Sort.by(Sort.Order.desc("atualizadoEm")))
                .forEach(c -> porStatus.get(c.getStatus()).add(CandidaturaDtos.resumo(c)));
        // prioridade é gravada como texto: ordenar no Java (ALTA > MEDIA > BAIXA); o sort é estável
        porStatus.values().forEach(l -> l.sort(Comparator.comparing(CandidaturaResumo::prioridade)));
        return porStatus.entrySet().stream().map(e -> new ColunaKanban(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<String> stacks() {
        return repository.listarStacks(usuarioAtual.id());
    }

    @Transactional(readOnly = true)
    public List<String> empresas() {
        return repository.listarEmpresas(usuarioAtual.id());
    }

    @Transactional
    public CandidaturaDetalhe criar(CandidaturaRequest req) {
        LocalDateTime agora = agora();
        validarChaveExterna(req.chaveExterna(), null);
        Candidatura c = new Candidatura();
        c.setOrigem(Origem.MANUAL);
        c.setUsuarioId(usuarioAtual.id());
        c.setCriadoEm(agora);
        preencher(c, req);
        c.setStatus(req.status() != null ? req.status() : Status.CANDIDATADO);
        if (c.getDataCandidatura() == null && c.getStatus() != Status.INTERESSE) {
            c.setDataCandidatura(agora.toLocalDate());
        }
        c.setAtualizadoEm(agora);
        c.adicionarHistorico(new HistoricoStatus(null, c.getStatus(), agora));
        return CandidaturaDtos.detalhe(repository.saveAndFlush(c));
    }

    @Transactional
    public CandidaturaDetalhe atualizar(Long id, CandidaturaRequest req) {
        Candidatura c = obter(id);
        validarChaveExterna(req.chaveExterna(), id);
        String pendenciaAntes = c.getPendencia();
        preencher(c, req);
        if (req.status() != null && req.status() != c.getStatus()) {
            // a tela já manda o campo vazio ao sair de Interesse; a pendência anterior volta por
            // um instante para que aplicarStatus a registre no histórico como concluída
            String pendenciaPedida = c.getPendencia();
            c.setPendencia(pendenciaAntes);
            aplicarStatus(c, req.status(), null);
            boolean concluida = pendenciaAntes != null && c.getPendencia() == null;
            // fora desse caso vale o que foi editado; uma pendência nova escrita na mesma edição também
            if (!concluida || (pendenciaPedida != null && !pendenciaPedida.equals(pendenciaAntes))) {
                c.setPendencia(pendenciaPedida);
            }
        }
        c.setAtualizadoEm(agora());
        return CandidaturaDtos.detalhe(repository.saveAndFlush(c));
    }

    @Transactional
    public CandidaturaDetalhe mudarStatus(Long id, MudancaStatusRequest req) {
        Candidatura c = obter(id);
        if (req.status() != c.getStatus()) {
            aplicarStatus(c, req.status(), req.observacao());
            c.setAtualizadoEm(agora());
        }
        return CandidaturaDtos.detalhe(repository.saveAndFlush(c));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    @Transactional
    public CandidaturaDetalhe vincularContato(Long id, Long contatoId) {
        Candidatura c = obter(id);
        Contato contato = contatoRepository.findByIdAndUsuarioId(contatoId, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contato", contatoId));
        c.getContatos().add(contato);
        c.setAtualizadoEm(agora());
        return CandidaturaDtos.detalhe(repository.saveAndFlush(c));
    }

    @Transactional
    public CandidaturaDetalhe desvincularContato(Long id, Long contatoId) {
        Candidatura c = obter(id);
        c.getContatos().removeIf(ct -> ct.getId().equals(contatoId));
        c.setAtualizadoEm(agora());
        return CandidaturaDtos.detalhe(repository.saveAndFlush(c));
    }

    /**
     * Troca o status gravando o histórico e uma interação MUDANCA_STATUS.
     * Não salva: a entidade deve estar gerenciada (ou ser salva pelo chamador).
     */
    public void aplicarStatus(Candidatura c, Status novo, String descricao) {
        Status anterior = c.getStatus();
        if (anterior == novo) {
            return;
        }
        LocalDateTime agora = agora();
        c.setStatus(novo);
        c.adicionarHistorico(new HistoricoStatus(anterior, novo, agora));
        String texto = "Status alterado de " + anterior + " para " + novo
                + (Normalizacao.vazio(descricao) ? "" : ": " + descricao.trim());
        // Sair de INTERESSE significa que o que faltava para enviar foi feito: a pendência é
        // concluída e o texto fica registrado na interação, para não se perder.
        if (anterior == Status.INTERESSE && c.getPendencia() != null) {
            texto += ". Pendência concluída: " + c.getPendencia();
            c.setPendencia(null);
        }
        c.adicionarInteracao(new Interacao(agora, TipoInteracao.MUDANCA_STATUS, limitar(texto, 4000)));
        if (c.getDataCandidatura() == null && anterior == Status.INTERESSE) {
            c.setDataCandidatura(agora.toLocalDate());
        }
    }

    Candidatura obter(Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura", id));
    }

    private void preencher(Candidatura c, CandidaturaRequest req) {
        c.setEmpresa(req.empresa().trim());
        c.setCargo(req.cargo().trim());
        c.setLinkVaga(Normalizacao.texto(req.linkVaga()));
        c.setPlataforma(req.plataforma());
        c.setModalidade(req.modalidade());
        c.setTipoContrato(req.tipoContrato());
        c.setFaixaSalarial(Normalizacao.texto(req.faixaSalarial()));
        c.setPretensaoSalarial(Normalizacao.texto(req.pretensaoSalarial()));
        c.setNota(req.nota());
        c.getStack().clear();
        c.getStack().addAll(Normalizacao.stack(req.stack() == null ? List.of() : List.copyOf(req.stack())));
        c.setDataCandidatura(req.dataCandidatura());
        c.setPrioridade(req.prioridade() != null ? req.prioridade() : Prioridade.MEDIA);
        c.setCurriculoVersao(Normalizacao.texto(req.curriculoVersao()));
        c.setObservacoes(Normalizacao.texto(req.observacoes()));
        // PUT substitui tudo: campo vazio limpa a pendência
        c.setPendencia(Normalizacao.texto(req.pendencia()));
        c.setChaveExterna(Normalizacao.texto(req.chaveExterna()));
        if (req.contatoIds() != null) {
            c.getContatos().clear();
            c.getContatos().addAll(req.contatoIds().isEmpty() ? List.of()
                    : contatoRepository.findByUsuarioIdAndIdIn(usuarioAtual.id(), req.contatoIds()));
        }
    }

    private void validarChaveExterna(String chave, Long idAtual) {
        String k = Normalizacao.texto(chave);
        if (k == null) {
            return;
        }
        repository.findByUsuarioIdAndChaveExterna(usuarioAtual.id(), k)
                .filter(outra -> !outra.getId().equals(idAtual))
                .ifPresent(outra -> {
                    throw new RegraNegocioException("Já existe a candidatura " + outra.getId()
                            + " com a chave externa '" + k + "'");
                });
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    static String limitar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
