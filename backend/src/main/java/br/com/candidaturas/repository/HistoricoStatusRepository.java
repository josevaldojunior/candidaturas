package br.com.candidaturas.repository;

import br.com.candidaturas.domain.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    List<HistoricoStatus> findByCandidaturaUsuarioId(Long usuarioId);
}
