package br.com.candidaturas.repository;

import br.com.candidaturas.domain.Contato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

    Optional<Contato> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    List<Contato> findByTelefoneIsNotNull();

    List<Contato> findAllByOrderByNomeAsc();
}
