package br.com.candidaturas.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "interacao")
public class Interacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @Column(nullable = false)
    private LocalDateTime data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoInteracao tipo;

    @Column(length = 4000)
    private String descricao;

    @Column(name = "proximo_passo", length = 1000)
    private String proximoPasso;

    @Column(name = "data_follow_up")
    private LocalDate dataFollowUp;

    @Column(name = "follow_up_concluido", nullable = false)
    private boolean followUpConcluido;

    public Interacao() {
    }

    public Interacao(LocalDateTime data, TipoInteracao tipo, String descricao) {
        this.data = data;
        this.tipo = tipo;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Candidatura getCandidatura() { return candidatura; }
    public void setCandidatura(Candidatura candidatura) { this.candidatura = candidatura; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
    public TipoInteracao getTipo() { return tipo; }
    public void setTipo(TipoInteracao tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getProximoPasso() { return proximoPasso; }
    public void setProximoPasso(String proximoPasso) { this.proximoPasso = proximoPasso; }
    public LocalDate getDataFollowUp() { return dataFollowUp; }
    public void setDataFollowUp(LocalDate dataFollowUp) { this.dataFollowUp = dataFollowUp; }
    public boolean isFollowUpConcluido() { return followUpConcluido; }
    public void setFollowUpConcluido(boolean followUpConcluido) { this.followUpConcluido = followUpConcluido; }
}
