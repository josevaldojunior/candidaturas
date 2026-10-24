package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidaturaRepository extends JpaRepository<Candidatura, Long>, JpaSpecificationExecutor<Candidatura> {

    Optional<Candidatura> findByChaveExterna(String chaveExterna);

    Optional<Candidatura> findFirstByLinkVagaOrderByIdAsc(String linkVaga);

    Optional<Candidatura> findFirstByEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(String empresa, String cargo);

    List<Candidatura> findByStatusIn(Collection<Status> status);

    @Query("select distinct t from Candidatura c join c.stack t order by t")
    List<String> listarStacks();

    @Query("select distinct c.empresa from Candidatura c order by c.empresa")
    List<String> listarEmpresas();
}
