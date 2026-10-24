package br.com.candidaturas.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página estável para serialização (evita expor PageImpl na API). */
public record Pagina<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapper) {
        return new Pagina<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
