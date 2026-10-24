package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Configuracao;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.dto.SemRetornoDtos.ResultadoSemRetorno;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ConfiguracaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encerra como SEM_RETORNO as candidaturas paradas (CANDIDATADO até ENTREVISTA_FINAL)
 * há mais de X dias sem interação. X fica na tabela configuracao.
 */
@Service
public class SemRetornoService {

    public static final String CHAVE_DIAS = "sem_retorno.dias";
    public static final int DIAS_PADRAO = 21;

    private static final Logger log = LoggerFactory.getLogger(SemRetornoService.class);

    private final CandidaturaRepository candidaturaRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final CandidaturaService candidaturaService;
    private final Clock clock;

    public SemRetornoService(CandidaturaRepository candidaturaRepository, ConfiguracaoRepository configuracaoRepository,
                             CandidaturaService candidaturaService, Clock clock) {
        this.candidaturaRepository = candidaturaRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.candidaturaService = candidaturaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public int dias() {
        return configuracaoRepository.findById(CHAVE_DIAS)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValor().trim());
                    } catch (NumberFormatException e) {
                        return DIAS_PADRAO;
                    }
                })
                .orElse(DIAS_PADRAO);
    }

    @Transactional
    public int salvarDias(int dias) {
        if (dias < 1 || dias > 365) {
            throw new RegraNegocioException("Dias deve estar entre 1 e 365");
        }
        Configuracao c = configuracaoRepository.findById(CHAVE_DIAS).orElse(new Configuracao(CHAVE_DIAS, ""));
        c.setValor(String.valueOf(dias));
        configuracaoRepository.save(c);
        return dias;
    }

    @Transactional
    public ResultadoSemRetorno executar() {
        int dias = dias();
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusDays(dias);
        List<Long> marcadas = new ArrayList<>();
        for (Candidatura c : candidaturaRepository.findByStatusIn(Status.AGUARDANDO_RETORNO)) {
            LocalDateTime referencia = referencia(c);
            if (referencia != null && referencia.isBefore(limite)) {
                candidaturaService.aplicarStatus(c, Status.SEM_RETORNO,
                        "marcada automaticamente após " + dias + " dias sem interação");
                c.setAtualizadoEm(agora);
                candidaturaRepository.save(c);
                marcadas.add(c.getId());
            }
        }
        if (!marcadas.isEmpty()) {
            log.info("Sem retorno: {} candidatura(s) marcadas (limite {} dias): {}", marcadas.size(), dias, marcadas);
        }
        return new ResultadoSemRetorno(dias, marcadas.size(), marcadas, agora);
    }

    /** Última interação; se não houver, a data da candidatura; por fim, a criação do registro. */
    static LocalDateTime referencia(Candidatura c) {
        if (c.getUltimaInteracaoEm() != null) {
            return c.getUltimaInteracaoEm();
        }
        if (c.getDataCandidatura() != null) {
            return c.getDataCandidatura().atStartOfDay();
        }
        return c.getCriadoEm();
    }
}
