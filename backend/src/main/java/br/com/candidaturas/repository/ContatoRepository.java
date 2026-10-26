package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

    Optional<Contato> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Contato> findByUsuarioIdAndIdIn(Long usuarioId, Collection<Long> ids);

    Optional<Contato> findFirstByUsuarioIdAndEmailIgnoreCaseOrderByIdAsc(Long usuarioId, String email);

    List<Contato> findByUsuarioIdAndTelefoneIsNotNull(Long usuarioId);

    List<Contato> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    @Modifying
    @Query("update Contato c set c.usuarioId = :usuarioId where c.usuarioId is null")
    int assumirSemDono(Long usuarioId);
}
