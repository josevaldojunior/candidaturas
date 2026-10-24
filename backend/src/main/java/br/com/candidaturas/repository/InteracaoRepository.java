package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    @Query("""
            select i from Interacao i join fetch i.candidatura c
            where i.dataFollowUp is not null and i.followUpConcluido = false and i.dataFollowUp <= :limite
            order by i.dataFollowUp asc, i.id asc
            """)
    List<Interacao> followUpsPendentesAte(LocalDate limite);
}
