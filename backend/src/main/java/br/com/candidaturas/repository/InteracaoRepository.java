package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    Optional<Interacao> findByIdAndCandidaturaUsuarioId(Long id, Long usuarioId);

    @Query("""
            select i from Interacao i join fetch i.candidatura c
            where c.usuarioId = :usuarioId
              and i.dataFollowUp is not null and i.followUpConcluido = false and i.dataFollowUp <= :limite
            order by i.dataFollowUp asc, i.id asc
            """)
    List<Interacao> followUpsPendentesAte(Long usuarioId, LocalDate limite);
}
