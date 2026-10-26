package br.com.candidaturas.service;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Status;
import br.com.candidaturas.domain.Usuario;
import br.com.candidaturas.dto.SemRetornoDtos.ResultadoSemRetorno;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.UsuarioRepository;
import br.com.candidaturas.seguranca.UsuarioAtual;
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
 * há mais de X dias sem interação. Cada conta tem o seu X (padrão 30).
 */
@Service
public class SemRetornoService {

    public static final int DIAS_PADRAO = 30;

    private static final Logger log = LoggerFactory.getLogger(SemRetornoService.class);

    private final CandidaturaRepository candidaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CandidaturaService candidaturaService;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public SemRetornoService(CandidaturaRepository candidaturaRepository, UsuarioRepository usuarioRepository,
                             CandidaturaService candidaturaService, UsuarioAtual usuarioAtual, Clock clock) {
        this.candidaturaRepository = candidaturaRepository;
        this.usuarioRepository = usuarioRepository;
        this.candidaturaService = candidaturaService;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public int dias() {
        return usuarioRepository.findById(usuarioAtual.id()).map(Usuario::getSemRetornoDias).orElse(DIAS_PADRAO);
    }

    @Transactional
    public int salvarDias(int dias) {
        if (dias < 1 || dias > 365) {
            throw new RegraNegocioException("Dias deve estar entre 1 e 365");
        }
        Usuario u = usuarioRepository.findById(usuarioAtual.id())
                .orElseThrow(() -> new NaoAutorizadoException("Conta não encontrada"));
        u.setSemRetornoDias(dias);
        return dias;
    }

    /** Marca as candidaturas paradas do usuário atual. */
    @Transactional
    public ResultadoSemRetorno executar() {
        Long usuarioId = usuarioAtual.id();
        int dias = dias();
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limite = agora.minusDays(dias);
        List<Long> marcadas = new ArrayList<>();
        for (Candidatura c : candidaturaRepository.findByUsuarioIdAndStatusIn(usuarioId, Status.AGUARDANDO_RETORNO)) {
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
            log.info("Sem retorno (usuário {}): {} candidatura(s) marcadas (limite {} dias): {}",
                    usuarioId, marcadas.size(), dias, marcadas);
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
