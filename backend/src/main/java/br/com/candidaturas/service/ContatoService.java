package br.com.candidaturas.service;

import br.com.candidaturas.domain.CargoContato;
import br.com.candidaturas.domain.Contato;
import br.com.candidaturas.dto.ContatoDtos;
import br.com.candidaturas.dto.ContatoDtos.ContatoRequest;
import br.com.candidaturas.dto.ContatoDtos.ContatoResponse;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.util.Normalizacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ContatoService {

    private final ContatoRepository repository;

    public ContatoService(ContatoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ContatoResponse> listar() {
        return repository.findAllByOrderByNomeAsc().stream().map(ContatoDtos::response).toList();
    }

    @Transactional(readOnly = true)
    public ContatoResponse buscar(Long id) {
        return ContatoDtos.response(obter(id));
    }

    @Transactional
    public ContatoResponse criar(ContatoRequest req) {
        Contato c = new Contato();
        preencher(c, req);
        return ContatoDtos.response(repository.save(c));
    }

    @Transactional
    public ContatoResponse atualizar(Long id, ContatoRequest req) {
        Contato c = obter(id);
        preencher(c, req);
        return ContatoDtos.response(c);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    /** Procura um contato existente pelo e-mail (sem caixa) ou pelo telefone (só dígitos). */
    @Transactional(readOnly = true)
    public Optional<Contato> localizar(String email, String telefone) {
        if (!Normalizacao.vazio(email)) {
            Optional<Contato> porEmail = repository.findFirstByEmailIgnoreCaseOrderByIdAsc(email.trim());
            if (porEmail.isPresent()) {
                return porEmail;
            }
        }
        String digitos = Normalizacao.digitos(telefone);
        if (digitos.length() >= 8) {
            return repository.findByTelefoneIsNotNull().stream()
                    .filter(c -> mesmoTelefone(Normalizacao.digitos(c.getTelefone()), digitos))
                    .findFirst();
        }
        return Optional.empty();
    }

    /** Compara telefones tolerando DDI/DDD ausente: um termina com o outro (mínimo 8 dígitos). */
    static boolean mesmoTelefone(String a, String b) {
        if (a.length() < 8 || b.length() < 8) {
            return false;
        }
        return a.endsWith(b) || b.endsWith(a);
    }

    private Contato obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Contato", id));
    }

    private void preencher(Contato c, ContatoRequest req) {
        c.setNome(req.nome().trim());
        c.setCargo(req.cargo() != null ? req.cargo() : CargoContato.RECRUTADOR);
        c.setEmail(Normalizacao.texto(req.email()));
        c.setLinkedin(Normalizacao.texto(req.linkedin()));
        c.setTelefone(Normalizacao.texto(req.telefone()));
        c.setEmpresa(Normalizacao.texto(req.empresa()));
    }
}
