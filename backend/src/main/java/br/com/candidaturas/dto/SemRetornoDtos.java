package br.com.candidaturas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class SemRetornoDtos {

    private SemRetornoDtos() {
    }

    public record ConfiguracaoSemRetorno(@NotNull @Min(1) @Max(365) Integer dias) {
    }

    public record ResultadoSemRetorno(int dias, int marcadas, List<Long> ids, LocalDateTime executadoEm) {
    }
}
