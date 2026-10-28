package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Status;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Toda consulta recebe o dono (usuarioId): um usuário nunca enxerga dados de outro. */
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long>, JpaSpecificationExecutor<Candidatura> {

    boolean existsByUsuarioIdAndPlataforma(Long usuarioId, String plataforma);

    Optional<Candidatura> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Candidatura> findByUsuarioId(Long usuarioId);

    List<Candidatura> findByUsuarioId(Long usuarioId, Sort sort);

    Optional<Candidatura> findByUsuarioIdAndChaveExterna(Long usuarioId, String chaveExterna);

    Optional<Candidatura> findFirstByUsuarioIdAndLinkVagaOrderByIdAsc(Long usuarioId, String linkVaga);

    Optional<Candidatura> findFirstByUsuarioIdAndEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(
            Long usuarioId, String empresa, String cargo);

    List<Candidatura> findByUsuarioIdAndStatusIn(Long usuarioId, Collection<Status> status);

    @Query("select distinct t from Candidatura c join c.stack t where c.usuarioId = :usuarioId order by t")
    List<String> listarStacks(Long usuarioId);

    @Query("select distinct c.empresa from Candidatura c where c.usuarioId = :usuarioId order by c.empresa")
    List<String> listarEmpresas(Long usuarioId);

    /** Registros criados antes das contas existirem passam para a primeira conta. */
    @Modifying
    @Query("update Candidatura c set c.usuarioId = :usuarioId where c.usuarioId is null")
    int assumirSemDono(Long usuarioId);
}
