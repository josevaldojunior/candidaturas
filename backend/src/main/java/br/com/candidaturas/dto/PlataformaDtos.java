package br.com.candidaturas.dto;
import br.com.candidaturas.domain.Plataforma;
import jakarta.validation.constraints.*;
public final class PlataformaDtos {
    private PlataformaDtos() {}
    public record Request(@NotBlank @Size(max=120) String nome, @NotNull @Min(0) @Max(9999) Integer ordem,
                          @NotNull Boolean ativa) {}
    public record Response(Long id, String codigo, String nome, int ordem, boolean ativa) {}
    public static Response response(Plataforma p) {
        return new Response(p.getId(),p.getCodigo(),p.getNome(),p.getOrdem(),p.isAtiva());
    }
}
