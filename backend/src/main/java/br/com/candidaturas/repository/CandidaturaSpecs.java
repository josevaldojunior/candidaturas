package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Plataforma;
import br.com.candidaturas.domain.Prioridade;
import br.com.candidaturas.domain.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Filtros da listagem de candidaturas. Parâmetros nulos ou vazios são ignorados. */
public final class CandidaturaSpecs {

    private CandidaturaSpecs() {
    }

    public static Specification<Candidatura> filtrar(List<Status> status, String empresa, List<Plataforma> plataformas,
                                                     String stack, List<Prioridade> prioridades,
                                                     LocalDate de, LocalDate ate, String texto,
                                                     Boolean soPendentes) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null && !status.isEmpty()) {
                p.add(root.get("status").in(status));
            }
            if (temTexto(empresa)) {
                p.add(cb.like(cb.lower(root.get("empresa")), like(empresa)));
            }
            if (plataformas != null && !plataformas.isEmpty()) {
                p.add(root.get("plataforma").in(plataformas));
            }
            if (prioridades != null && !prioridades.isEmpty()) {
                p.add(root.get("prioridade").in(prioridades));
            }
            if (temTexto(stack)) {
                Join<Candidatura, String> tags = root.join("stack");
                p.add(cb.equal(cb.lower(tags), stack.trim().toLowerCase(Locale.ROOT)));
                query.distinct(true);
            }
            if (de != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dataCandidatura"), de));
            }
            if (ate != null) {
                p.add(cb.lessThanOrEqualTo(root.get("dataCandidatura"), ate));
            }
            if (Boolean.TRUE.equals(soPendentes)) {
                p.add(cb.isNotNull(root.get("pendencia")));
            }
            if (temTexto(texto)) {
                String t = like(texto);
                p.add(cb.or(
                        cb.like(cb.lower(root.get("empresa")), t),
                        cb.like(cb.lower(root.get("cargo")), t),
                        cb.like(cb.lower(cb.coalesce(root.get("observacoes"), "")), t),
                        cb.like(cb.lower(cb.coalesce(root.get("linkVaga"), "")), t)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
