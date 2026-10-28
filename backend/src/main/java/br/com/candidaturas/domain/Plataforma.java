package br.com.candidaturas.domain;

import jakarta.persistence.*;

/** Catálogo de plataformas de uma conta. O código é estável para integração e CSV. */
@Entity
@Table(name = "plataforma", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "codigo"}),
    @UniqueConstraint(columnNames = {"usuario_id", "nome_chave"})})
public class Plataforma {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(nullable = false, length = 40) private String codigo;
    @Column(nullable = false, length = 120) private String nome;
    @Column(name = "nome_chave", nullable = false, length = 120) private String nomeChave;
    @Column(nullable = false) private int ordem;
    @Column(nullable = false) private boolean ativa = true;
    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long v) { usuarioId = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { codigo = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { nome = v; }
    public String getNomeChave() { return nomeChave; }
    public void setNomeChave(String v) { nomeChave = v; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int v) { ordem = v; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean v) { ativa = v; }
}
