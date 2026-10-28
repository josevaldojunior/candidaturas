package br.com.candidaturas.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "candidatura")
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono da candidatura. */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 200)
    private String empresa;

    @Column(nullable = false, length = 200)
    private String cargo;

    @Column(name = "link_vaga", length = 1000)
    private String linkVaga;

    @Column(length = 40)
    private String plataforma;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Modalidade modalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", length = 20)
    private TipoContrato tipoContrato;

    @Column(name = "faixa_salarial", length = 100)
    private String faixaSalarial;

    @Column(name = "pretensao_salarial", length = 100)
    private String pretensaoSalarial;

    private Integer nota;

    @Column(name = "data_candidatura")
    private LocalDate dataCandidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Column(name = "curriculo_versao", length = 100)
    private String curriculoVersao;

    @Column(length = 4000)
    private String observacoes;

    @Column(length = 1000)
    private String pendencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origem origem;

    @Column(name = "chave_externa", length = 300)
    private String chaveExterna;

    @Column(name = "ultima_interacao_em")
    private LocalDateTime ultimaInteracaoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ElementCollection
    @CollectionTable(name = "candidatura_stack", joinColumns = @JoinColumn(name = "candidatura_id"))
    @Column(name = "tag", length = 60, nullable = false)
    @OrderBy
    private Set<String> stack = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "candidatura_contato",
            joinColumns = @JoinColumn(name = "candidatura_id"),
            inverseJoinColumns = @JoinColumn(name = "contato_id"))
    private Set<Contato> contatos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data DESC, id DESC")
    private List<Interacao> interacoes = new ArrayList<>();

    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data ASC, id ASC")
    private List<HistoricoStatus> historico = new ArrayList<>();

    public void adicionarInteracao(Interacao interacao) {
        interacao.setCandidatura(this);
        interacoes.add(interacao);
        if (ultimaInteracaoEm == null || interacao.getData().isAfter(ultimaInteracaoEm)) {
            ultimaInteracaoEm = interacao.getData();
        }
    }

    public void adicionarHistorico(HistoricoStatus h) {
        h.setCandidatura(this);
        historico.add(h);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getLinkVaga() { return linkVaga; }
    public void setLinkVaga(String linkVaga) { this.linkVaga = linkVaga; }
    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }
    public Modalidade getModalidade() { return modalidade; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }
    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }
    public String getFaixaSalarial() { return faixaSalarial; }
    public void setFaixaSalarial(String faixaSalarial) { this.faixaSalarial = faixaSalarial; }
    public String getPretensaoSalarial() { return pretensaoSalarial; }
    public void setPretensaoSalarial(String pretensaoSalarial) { this.pretensaoSalarial = pretensaoSalarial; }
    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }
    public LocalDate getDataCandidatura() { return dataCandidatura; }
    public void setDataCandidatura(LocalDate dataCandidatura) { this.dataCandidatura = dataCandidatura; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }
    public String getCurriculoVersao() { return curriculoVersao; }
    public void setCurriculoVersao(String curriculoVersao) { this.curriculoVersao = curriculoVersao; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getPendencia() { return pendencia; }
    public void setPendencia(String pendencia) { this.pendencia = pendencia; }
    public Origem getOrigem() { return origem; }
    public void setOrigem(Origem origem) { this.origem = origem; }
    public String getChaveExterna() { return chaveExterna; }
    public void setChaveExterna(String chaveExterna) { this.chaveExterna = chaveExterna; }
    public LocalDateTime getUltimaInteracaoEm() { return ultimaInteracaoEm; }
    public void setUltimaInteracaoEm(LocalDateTime ultimaInteracaoEm) { this.ultimaInteracaoEm = ultimaInteracaoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public Set<String> getStack() { return stack; }
    public void setStack(Set<String> stack) { this.stack = stack; }
    public Set<Contato> getContatos() { return contatos; }
    public void setContatos(Set<Contato> contatos) { this.contatos = contatos; }
    public List<Interacao> getInteracoes() { return interacoes; }
    public List<HistoricoStatus> getHistorico() { return historico; }
}
