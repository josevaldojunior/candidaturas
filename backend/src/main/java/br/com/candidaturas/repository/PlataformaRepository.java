package br.com.candidaturas.repository;
import br.com.candidaturas.domain.Plataforma;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PlataformaRepository extends JpaRepository<Plataforma,Long> {
    List<Plataforma> findByUsuarioIdOrderByOrdemAscIdAsc(Long usuarioId);
    Optional<Plataforma> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndNomeChaveAndIdNot(Long usuarioId, String chave, Long id);
    boolean existsByUsuarioIdAndCodigo(Long usuarioId, String codigo);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Plataforma> findByUsuarioIdAndCodigo(Long usuarioId, String codigo);
}
