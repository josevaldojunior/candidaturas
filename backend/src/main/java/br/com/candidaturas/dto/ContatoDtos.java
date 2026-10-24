package br.com.candidaturas.dto;

import br.com.candidaturas.domain.CargoContato;
import br.com.candidaturas.domain.Contato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ContatoDtos {

    private ContatoDtos() {
    }

    public record ContatoRequest(
            @NotBlank @Size(max = 200) String nome,
            CargoContato cargo,
            @Email @Size(max = 200) String email,
            @Size(max = 500) String linkedin,
            @Size(max = 50) String telefone,
            @Size(max = 200) String empresa) {
    }

    public record ContatoResponse(Long id, String nome, CargoContato cargo, String email, String linkedin,
                                  String telefone, String empresa) {
    }

    public static ContatoResponse response(Contato c) {
        return new ContatoResponse(c.getId(), c.getNome(), c.getCargo(), c.getEmail(), c.getLinkedin(),
                c.getTelefone(), c.getEmpresa());
    }
}
