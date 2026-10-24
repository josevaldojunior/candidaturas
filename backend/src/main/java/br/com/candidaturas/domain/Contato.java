package br.com.candidaturas.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "contato")
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CargoContato cargo;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String linkedin;

    @Column(length = 50)
    private String telefone;

    @Column(length = 200)
    private String empresa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public CargoContato getCargo() { return cargo; }
    public void setCargo(CargoContato cargo) { this.cargo = cargo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Contato c && id != null && id.equals(c.id));
    }

    @Override
    public int hashCode() {
        return Contato.class.hashCode();
    }
}
