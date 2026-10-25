package br.com.candidaturas.service;

import br.com.candidaturas.domain.*;
import br.com.candidaturas.dto.IntegracaoDtos.ExisteResponse;
import br.com.candidaturas.dto.IntegracaoDtos.ResultadoImportacao;
import br.com.candidaturas.dto.IntegracaoDtos.ResumoExistente;
import br.com.candidaturas.dto.IntegracaoItem;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.util.Normalizacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static br.com.candidaturas.util.Normalizacao.*;

/**
 * Upsert de candidaturas vindas do agente (JSON), da pasta inbox ou da importação de CSV.
 * Cada item roda na sua própria transação: um erro num item não desfaz os outros.
 */
@Service
public class IntegracaoService {

    private static final Logger log = LoggerFactory.getLogger(IntegracaoService.class);

    enum Resultado { CRIADA, ATUALIZADA, IGNORADA }

    static final String PREFIXO_PENDENCIA = "PENDENTE:";

    private final CandidaturaRepository repository;
    private final ContatoRepository contatoRepository;
    private final CandidaturaService candidaturaService;
    private final TransactionTemplate transacaoPorItem;
    private final Clock clock;

    public IntegracaoService(CandidaturaRepository repository, ContatoRepository contatoRepository,
                             CandidaturaService candidaturaService, PlatformTransactionManager transactionManager,
                             Clock clock) {
        this.repository = repository;
        this.contatoRepository = contatoRepository;
        this.candidaturaService = candidaturaService;
        this.transacaoPorItem = new TransactionTemplate(transactionManager);
        this.transacaoPorItem.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    public ResultadoImportacao processar(List<IntegracaoItem> itens, Origem origem) {
        int criadas = 0;
        int atualizadas = 0;
        int ignoradas = 0;
        List<String> erros = new ArrayList<>();
        List<IntegracaoItem> lista = itens == null ? List.of() : itens;
        for (int i = 0; i < lista.size(); i++) {
            IntegracaoItem item = lista.get(i);
            try {
                if (item == null) {
                    throw new IllegalArgumentException("item vazio");
                }
                Resultado r = transacaoPorItem.execute(status -> upsert(item, origem));
                switch (Objects.requireNonNull(r)) {
                    case CRIADA -> criadas++;
                    case ATUALIZADA -> atualizadas++;
                    case IGNORADA -> ignoradas++;
                }
            } catch (RuntimeException e) {
                String msg = "Item " + (i + 1) + ": " + mensagem(e);
                log.warn("Integração ({}): {}", origem, msg);
                erros.add(msg);
            }
        }
        return new ResultadoImportacao(criadas, atualizadas, ignoradas, erros);
    }

    @Transactional(readOnly = true)
    public ExisteResponse existe(String link, String empresa, String cargo) {
        Optional<Candidatura> encontrada = Optional.empty();
        String l = texto(link);
        if (l != null) {
            encontrada = repository.findByChaveExterna(l).or(() -> repository.findFirstByLinkVagaOrderByIdAsc(l));
        } else if (!vazio(empresa) && !vazio(cargo)) {
            encontrada = repository.findFirstByEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(empresa.trim(), cargo.trim());
        }
        return encontrada.map(c -> new ExisteResponse(true, ResumoExistente.de(c)))
                .orElseGet(() -> new ExisteResponse(false, null));
    }

    Resultado upsert(IntegracaoItem item, Origem origem) {
        Dados d = Dados.de(item);
        return localizar(d).map(c -> atualizar(c, d)).orElseGet(() -> criar(d, origem));
    }

    /** Chave = chaveExterna; se vazia, linkVaga. Sem nenhuma das duas, empresa + cargo. */
    private Optional<Candidatura> localizar(Dados d) {
        if (d.chave() != null) {
            Optional<Candidatura> porChave = repository.findByChaveExterna(d.chave());
            if (porChave.isPresent()) {
                return porChave;
            }
        }
        if (d.link() != null) {
            return repository.findByChaveExterna(d.link())
                    .or(() -> repository.findFirstByLinkVagaOrderByIdAsc(d.link()));
        }
        if (d.chave() == null && d.empresa() != null && d.cargo() != null) {
            return repository.findFirstByEmpresaIgnoreCaseAndCargoIgnoreCaseOrderByIdAsc(d.empresa(), d.cargo());
        }
        return Optional.empty();
    }

    private Resultado criar(Dados d, Origem origem) {
        if (d.empresa() == null) {
            throw new IllegalArgumentException("empresa é obrigatória");
        }
        if (d.cargo() == null) {
            throw new IllegalArgumentException("cargo é obrigatório");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        Candidatura c = new Candidatura();
        c.setOrigem(origem);
        c.setChaveExterna(d.chave());
        c.setEmpresa(d.empresa());
        c.setCargo(d.cargo());
        c.setLinkVaga(d.link());
        c.setPlataforma(d.plataforma());
        c.setModalidade(d.modalidade());
        c.setTipoContrato(d.tipoContrato());
        c.setFaixaSalarial(d.faixaSalarial());
        c.setPretensaoSalarial(d.pretensaoSalarial());
        c.setNota(d.nota());
        c.getStack().addAll(d.stack());
        c.setStatus(d.status() != null ? d.status() : Status.CANDIDATADO);
        c.setPrioridade(d.prioridade() != null ? d.prioridade() : Prioridade.MEDIA);
        c.setDataCandidatura(d.dataCandidatura() != null ? d.dataCandidatura()
                : c.getStatus() == Status.INTERESSE ? null : agora.toLocalDate());
        c.setCurriculoVersao(d.curriculoVersao());
        c.setObservacoes(d.observacoes());
        c.setPendencia(d.pendencia());
        c.setCriadoEm(agora);
        c.setAtualizadoEm(agora);
        c.adicionarHistorico(new HistoricoStatus(null, c.getStatus(), agora));
        vincularContato(c, d);
        registrarInteracao(c, d, agora);
        repository.save(c);
        return Resultado.CRIADA;
    }

    /** Atualiza só os campos enviados; devolve IGNORADA quando nada mudou. */
    private Resultado atualizar(Candidatura c, Dados d) {
        LocalDateTime agora = LocalDateTime.now(clock);
        boolean mudou = false;
        if (d.chave() != null && c.getChaveExterna() == null) {
            c.setChaveExterna(d.chave());
            mudou = true;
        }
        mudou |= aplicar(d.empresa(), c::getEmpresa, c::setEmpresa);
        mudou |= aplicar(d.cargo(), c::getCargo, c::setCargo);
        mudou |= aplicar(d.link(), c::getLinkVaga, c::setLinkVaga);
        mudou |= aplicar(d.plataforma(), c::getPlataforma, c::setPlataforma);
        mudou |= aplicar(d.modalidade(), c::getModalidade, c::setModalidade);
        mudou |= aplicar(d.tipoContrato(), c::getTipoContrato, c::setTipoContrato);
        mudou |= aplicar(d.faixaSalarial(), c::getFaixaSalarial, c::setFaixaSalarial);
        mudou |= aplicar(d.pretensaoSalarial(), c::getPretensaoSalarial, c::setPretensaoSalarial);
        mudou |= aplicar(d.nota(), c::getNota, c::setNota);
        mudou |= aplicar(d.dataCandidatura(), c::getDataCandidatura, c::setDataCandidatura);
        mudou |= aplicar(d.prioridade(), c::getPrioridade, c::setPrioridade);
        mudou |= aplicar(d.curriculoVersao(), c::getCurriculoVersao, c::setCurriculoVersao);
        boolean observacoesMudaram = aplicar(d.observacoes(), c::getObservacoes, c::setObservacoes);
        mudou |= observacoesMudaram;
        // só recria a pendência quando chega uma observação nova: reenviar o mesmo item não
        // ressuscita uma pendência que foi limpa na tela
        if (observacoesMudaram) {
            mudou |= aplicar(d.pendencia(), c::getPendencia, c::setPendencia);
        }
        if (!d.stack().isEmpty() && !minusculas(d.stack()).equals(minusculas(c.getStack()))) {
            c.getStack().clear();
            c.getStack().addAll(d.stack());
            mudou = true;
        }
        if (d.status() != null && d.status() != c.getStatus()) {
            candidaturaService.aplicarStatus(c, d.status(), "atualizado pelo agente");
            mudou = true;
        }
        mudou |= vincularContato(c, d);
        mudou |= registrarInteracao(c, d, agora);
        if (!mudou) {
            return Resultado.IGNORADA;
        }
        c.setAtualizadoEm(agora);
        repository.save(c);
        return Resultado.ATUALIZADA;
    }

    /** Reaproveita contato por e-mail ou telefone; senão cria como RECRUTADOR. */
    private boolean vincularContato(Candidatura c, Dados d) {
        if (d.contatoNome() == null && d.contatoEmail() == null && d.contatoTelefone() == null) {
            return false;
        }
        Optional<Contato> jaVinculado = c.getContatos().stream().filter(ct -> mesmoContato(ct, d)).findFirst();
        if (jaVinculado.isPresent()) {
            return completar(jaVinculado.get(), d);
        }
        Contato contato = localizarContato(d).orElseGet(() -> {
            Contato novo = new Contato();
            novo.setNome(d.contatoNome() != null ? d.contatoNome()
                    : d.contatoEmail() != null ? d.contatoEmail() : d.contatoTelefone());
            novo.setCargo(CargoContato.RECRUTADOR);
            novo.setEmail(d.contatoEmail());
            novo.setTelefone(d.contatoTelefone());
            novo.setEmpresa(c.getEmpresa());
            return contatoRepository.save(novo);
        });
        completar(contato, d);
        c.getContatos().add(contato);
        return true;
    }

    private Optional<Contato> localizarContato(Dados d) {
        if (d.contatoEmail() != null) {
            Optional<Contato> porEmail = contatoRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(d.contatoEmail());
            if (porEmail.isPresent()) {
                return porEmail;
            }
        }
        String fone = digitos(d.contatoTelefone());
        if (fone.length() >= 8) {
            return contatoRepository.findByTelefoneIsNotNull().stream()
                    .filter(ct -> ContatoService.mesmoTelefone(digitos(ct.getTelefone()), fone))
                    .findFirst();
        }
        return Optional.empty();
    }

    private static boolean mesmoContato(Contato ct, Dados d) {
        if (d.contatoEmail() != null && d.contatoEmail().equalsIgnoreCase(ct.getEmail())) {
            return true;
        }
        if (d.contatoTelefone() != null && ContatoService.mesmoTelefone(digitos(ct.getTelefone()), digitos(d.contatoTelefone()))) {
            return true;
        }
        return d.contatoEmail() == null && d.contatoTelefone() == null && d.contatoNome().equalsIgnoreCase(ct.getNome());
    }

    /** Preenche e-mail/telefone que o contato existente ainda não tinha. */
    private static boolean completar(Contato ct, Dados d) {
        boolean mudou = false;
        if (ct.getEmail() == null && d.contatoEmail() != null) {
            ct.setEmail(d.contatoEmail());
            mudou = true;
        }
        if (ct.getTelefone() == null && d.contatoTelefone() != null) {
            ct.setTelefone(d.contatoTelefone());
            mudou = true;
        }
        return mudou;
    }

    /** Cria interação se vier descrição ou follow-up, sem repetir uma igual já registrada. */
    private static boolean registrarInteracao(Candidatura c, Dados d, LocalDateTime agora) {
        if (d.descricaoInteracao() == null && d.dataFollowUp() == null) {
            return false;
        }
        TipoInteracao tipo = d.tipoInteracao() != null ? d.tipoInteracao() : TipoInteracao.OUTRO;
        boolean repetida = c.getInteracoes().stream().anyMatch(i -> i.getTipo() == tipo
                && Objects.equals(i.getDescricao(), d.descricaoInteracao())
                && Objects.equals(i.getDataFollowUp(), d.dataFollowUp()));
        if (repetida) {
            return false;
        }
        Interacao i = new Interacao(agora, tipo, d.descricaoInteracao());
        i.setProximoPasso(d.proximoPasso());
        i.setDataFollowUp(d.dataFollowUp());
        c.adicionarInteracao(i);
        return true;
    }

    private static <T> boolean aplicar(T novo, Supplier<T> atual, Consumer<T> setter) {
        if (novo == null || novo.equals(atual.get())) {
            return false;
        }
        setter.accept(novo);
        return true;
    }

    private static Set<String> minusculas(Collection<String> tags) {
        return tags.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private static String mensagem(RuntimeException e) {
        if (e instanceof IllegalArgumentException || e instanceof RegraNegocioException) {
            return e.getMessage();
        }
        if (e instanceof DataIntegrityViolationException) {
            return "conflito ao gravar no banco (registro duplicado ou valor inválido)";
        }
        return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }

    /** Item já normalizado e validado. */
    record Dados(String chave, String empresa, String cargo, String link, Plataforma plataforma,
                 Modalidade modalidade, TipoContrato tipoContrato, String faixaSalarial, String pretensaoSalarial,
                 Integer nota, LocalDate dataCandidatura, Status status, Prioridade prioridade,
                 String curriculoVersao, String observacoes, String pendencia, Set<String> stack, String contatoNome,
                 String contatoEmail, String contatoTelefone, TipoInteracao tipoInteracao,
                 String descricaoInteracao, String proximoPasso, LocalDate dataFollowUp) {

        static Dados de(IntegracaoItem i) {
            return new Dados(
                    limite(i.chaveExterna(), 300, "chaveExterna"),
                    limite(i.empresa(), 200, "empresa"),
                    limite(i.cargo(), 200, "cargo"),
                    limite(i.linkVaga(), 1000, "linkVaga"),
                    Normalizacao.plataforma(i.plataforma()),
                    paraEnum(Modalidade.class, i.modalidade(), "modalidade"),
                    paraEnumOu(TipoContrato.class, i.tipoContrato(), "tipoContrato", TipoContrato.OUTRO),
                    limite(i.faixaSalarial(), 100, "faixaSalarial"),
                    limite(i.pretensaoSalarial(), 100, "pretensaoSalarial"),
                    Normalizacao.nota(i.nota()),
                    data(i.dataCandidatura(), "dataCandidatura"),
                    paraEnum(Status.class, i.status(), "status"),
                    paraEnum(Prioridade.class, i.prioridade(), "prioridade"),
                    limite(i.curriculoVersao(), 100, "curriculoVersao"),
                    limite(i.observacoes(), 4000, "observacoes"),
                    pendenciaDe(i.observacoes()),
                    Normalizacao.stack(i.stack()),
                    limite(i.contatoNome(), 200, "contatoNome"),
                    limite(i.contatoEmail(), 200, "contatoEmail"),
                    limite(i.contatoTelefone(), 50, "contatoTelefone"),
                    paraEnumOu(TipoInteracao.class, i.tipoInteracao(), "tipoInteracao", TipoInteracao.OUTRO),
                    limite(i.descricaoInteracao(), 4000, "descricaoInteracao"),
                    limite(i.proximoPasso(), 1000, "proximoPasso"),
                    data(i.dataFollowUp(), "dataFollowUp"));
        }

        /** Observações que começam com "PENDENTE:" viram pendência (o texto depois do prefixo). */
        static String pendenciaDe(String observacoes) {
            String t = texto(observacoes);
            if (t == null || !t.regionMatches(true, 0, PREFIXO_PENDENCIA, 0, PREFIXO_PENDENCIA.length())) {
                return null;
            }
            String resto = t.substring(PREFIXO_PENDENCIA.length()).strip();
            if (resto.isEmpty()) {
                return null;
            }
            return resto.length() > 1000 ? resto.substring(0, 1000) : resto;
        }

        private static String limite(String valor, int max, String campo) {
            String v = texto(valor);
            if (v != null && v.length() > max) {
                throw new IllegalArgumentException(campo + " excede " + max + " caracteres");
            }
            return v;
        }
    }
}
