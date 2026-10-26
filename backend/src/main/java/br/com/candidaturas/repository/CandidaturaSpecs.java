package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.HistoricoStatus;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.dto.FiltroCandidaturas;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Filtros da listagem de candidaturas. Parâmetros nulos ou vazios são ignorados. */
public final class CandidaturaSpecs {

    private CandidaturaSpecs() {
    }

    public static Specification<Candidatura> filtrar(Long usuarioId, FiltroCandidaturas f) {
        // valida antes de montar a consulta: um erro aqui vira 400, e não uma falha dentro do JPA
        if (f.etapaMinima() != null && !f.etapaMinima().isEtapaDoFunil()) {
            throw new IllegalArgumentException(
                    "etapaMinima deve ser uma etapa do funil (INTERESSE a ACEITA): " + f.etapaMinima());
        }
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("usuarioId"), usuarioId));
            if (f.status() != null && !f.status().isEmpty()) {
                p.add(root.get("status").in(f.status()));
            }
            if (temTexto(f.empresa())) {
                p.add(cb.like(cb.lower(root.get("empresa")), like(f.empresa())));
            }
            if (f.plataforma() != null && !f.plataforma().isEmpty()) {
                p.add(root.get("plataforma").in(f.plataforma()));
            }
            if (f.prioridade() != null && !f.prioridade().isEmpty()) {
                p.add(root.get("prioridade").in(f.prioridade()));
            }
            if (temTexto(f.stack())) {
                Join<Candidatura, String> tags = root.join("stack");
                p.add(cb.equal(cb.lower(tags), f.stack().trim().toLowerCase(Locale.ROOT)));
                query.distinct(true);
            }
            if (f.de() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dataCandidatura"), f.de()));
            }
            if (f.ate() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("dataCandidatura"), f.ate()));
            }
            if (Boolean.TRUE.equals(f.pendentes())) {
                p.add(cb.isNotNull(root.get("pendencia")));
            }
            if (f.etapaMinima() != null) {
                p.add(alcancou(f.etapaMinima(), root, query, cb));
            }
            if (Boolean.TRUE.equals(f.comResposta())) {
                // mesma regra da taxa de resposta do dashboard
                p.add(cb.or(alcancou(Status.TRIAGEM_RH, root, query, cb), cb.equal(root.get("status"), Status.RECUSADA)));
            }
            if (temTexto(f.q())) {
                String t = like(f.q());
                p.add(cb.or(
                        cb.like(cb.lower(root.get("empresa")), t),
                        cb.like(cb.lower(root.get("cargo")), t),
                        cb.like(cb.lower(cb.coalesce(root.get("observacoes"), "")), t),
                        cb.like(cb.lower(cb.coalesce(root.get("linkVaga"), "")), t)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /**
     * A candidatura chegou pelo menos à etapa informada em algum momento: pelo status atual ou pelo histórico.
     * É a mesma regra do funil do dashboard, em que RECUSADA e SEM_RETORNO contam como "enviada" (CANDIDATADO).
     */
    private static Predicate alcancou(Status minima, Root<Candidatura> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Set<Status> etapas = Arrays.stream(Status.values())
                .filter(s -> s.isEtapaDoFunil() ? s.getEtapa() >= minima.getEtapa()
                        : Status.CANDIDATADO.getEtapa() >= minima.getEtapa())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Status.class)));
        Subquery<Long> historico = query.subquery(Long.class);
        Root<HistoricoStatus> h = historico.from(HistoricoStatus.class);
        historico.select(h.get("id")).where(cb.equal(h.get("candidatura"), root), h.get("statusNovo").in(etapas));
        return cb.or(root.get("status").in(etapas), cb.exists(historico));
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
