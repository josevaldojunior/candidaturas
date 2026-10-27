package br.com.candidaturas.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Pipeline da candidatura. {@code etapa} é a posição no funil; os status finais
 * RECUSADA e SEM_RETORNO não são etapas (encerram o funil onde ele parou).
 */
public enum Status {
    INTERESSE(0, "Interesse"),
    CANDIDATADO(1, "Candidatado"),
    TRIAGEM_RH(2, "Triagem RH"),
    TESTE_TECNICO(3, "Teste técnico"),
    ENTREVISTA_TECNICA(4, "Entrevista técnica"),
    ENTREVISTA_FINAL(5, "Entrevista final"),
    PROPOSTA(6, "Proposta"),
    ACEITA(7, "Aceita"),
    RECUSADA(-1, "Recusada"),
    SEM_RETORNO(-1, "Sem retorno");

    /** Status que o job de "sem retorno" pode encerrar. */
    public static final Set<Status> AGUARDANDO_RETORNO = EnumSet.of(
            CANDIDATADO, TRIAGEM_RH, TESTE_TECNICO, ENTREVISTA_TECNICA, ENTREVISTA_FINAL);

    private final int etapa;
    private final String rotulo;

    Status(int etapa, String rotulo) {
        this.etapa = etapa;
        this.rotulo = rotulo;
    }

    public int getEtapa() {
        return etapa;
    }

    /** Nome para pessoas (o mesmo da interface). */
    public String getRotulo() {
        return rotulo;
    }

    public boolean isEtapaDoFunil() {
        return etapa >= 0;
    }

    public static Status[] etapasDoFunil() {
        return new Status[]{INTERESSE, CANDIDATADO, TRIAGEM_RH, TESTE_TECNICO,
                ENTREVISTA_TECNICA, ENTREVISTA_FINAL, PROPOSTA, ACEITA};
    }
}
