package br.com.candidaturas.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private Status statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private Status statusNovo;

    @Column(nullable = false)
    private LocalDateTime data;

    public HistoricoStatus() {
    }

    public HistoricoStatus(Status statusAnterior, Status statusNovo, LocalDateTime data) {
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.data = data;
    }

    public Long getId() { return id; }
    public Candidatura getCandidatura() { return candidatura; }
    public void setCandidatura(Candidatura candidatura) { this.candidatura = candidatura; }
    public Status getStatusAnterior() { return statusAnterior; }
    public Status getStatusNovo() { return statusNovo; }
    public LocalDateTime getData() { return data; }
}
