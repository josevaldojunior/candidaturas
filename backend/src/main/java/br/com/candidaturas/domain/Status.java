package br.com.candidaturas.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pipeline da candidatura. {@code etapa} é a posição no funil; os status finais
 * RECUSADA e SEM_RETORNO não são etapas (encerram o funil onde ele parou).
 */
public enum Status {
    INTERESSE(0),
    CANDIDATADO(1),
    TRIAGEM_RH(2),
    TESTE_TECNICO(3),
    ENTREVISTA_TECNICA(4),
    ENTREVISTA_FINAL(5),
    PROPOSTA(6),
    ACEITA(7),
    RECUSADA(-1),
    SEM_RETORNO(-1);

    /** Status que o job de "sem retorno" pode encerrar. */
    public static final Set<Status> AGUARDANDO_RETORNO = EnumSet.of(
            CANDIDATADO, TRIAGEM_RH, TESTE_TECNICO, ENTREVISTA_TECNICA, ENTREVISTA_FINAL);

    private final int etapa;

    Status(int etapa) {
        this.etapa = etapa;
    }

    public int getEtapa() {
        return etapa;
    }

    public boolean isEtapaDoFunil() {
        return etapa >= 0;
    }

    public static Status[] etapasDoFunil() {
        return new Status[]{INTERESSE, CANDIDATADO, TRIAGEM_RH, TESTE_TECNICO,
                ENTREVISTA_TECNICA, ENTREVISTA_FINAL, PROPOSTA, ACEITA};
    }
}
