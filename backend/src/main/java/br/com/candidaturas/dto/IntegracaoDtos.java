package br.com.candidaturas.dto;

import br.com.candidaturas.domain.Candidatura;
import br.com.candidaturas.domain.Plataforma;
import br.com.candidaturas.domain.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class IntegracaoDtos {

    private IntegracaoDtos() {
    }

    public record ResultadoImportacao(int criadas, int atualizadas, int ignoradas, List<String> erros) {
    }

    public record ExisteResponse(boolean existe, ResumoExistente candidatura) {
    }

    public record ResumoExistente(Long id, String empresa, String cargo, String linkVaga, String chaveExterna,
                                  Status status, Plataforma plataforma, LocalDate dataCandidatura,
                                  LocalDateTime ultimaInteracaoEm) {

        public static ResumoExistente de(Candidatura c) {
            return new ResumoExistente(c.getId(), c.getEmpresa(), c.getCargo(), c.getLinkVaga(), c.getChaveExterna(),
                    c.getStatus(), c.getPlataforma(), c.getDataCandidatura(), c.getUltimaInteracaoEm());
        }
    }

    public record ArquivoProcessado(String arquivo, String destino, ResultadoImportacao resultado) {
    }

    public record ResultadoInbox(List<ArquivoProcessado> arquivos) {
    }
}
