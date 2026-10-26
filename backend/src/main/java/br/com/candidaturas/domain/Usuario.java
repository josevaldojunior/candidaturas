package br.com.candidaturas.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Sempre em minúsculas. */
    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    /** SHA-256 do token do agente; o token em si só é mostrado uma vez, ao ser gerado. */
    @Column(name = "token_integracao_hash", length = 64, unique = true)
    private String tokenIntegracaoHash;

    @Column(name = "token_integracao_criado_em")
    private LocalDateTime tokenIntegracaoCriadoEm;

    @Column(name = "sem_retorno_dias", nullable = false)
    private int semRetornoDias = 30;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getTokenIntegracaoHash() { return tokenIntegracaoHash; }
    public void setTokenIntegracaoHash(String tokenIntegracaoHash) { this.tokenIntegracaoHash = tokenIntegracaoHash; }
    public LocalDateTime getTokenIntegracaoCriadoEm() { return tokenIntegracaoCriadoEm; }
    public void setTokenIntegracaoCriadoEm(LocalDateTime tokenIntegracaoCriadoEm) { this.tokenIntegracaoCriadoEm = tokenIntegracaoCriadoEm; }
    public int getSemRetornoDias() { return semRetornoDias; }
    public void setSemRetornoDias(int semRetornoDias) { this.semRetornoDias = semRetornoDias; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
