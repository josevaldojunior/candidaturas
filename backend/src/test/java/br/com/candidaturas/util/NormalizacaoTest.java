package br.com.candidaturas.util;

import br.com.candidaturas.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class NormalizacaoTest {

    @ParameterizedTest
    @CsvSource({"Híbrido,HIBRIDO", "remoto,REMOTO", "  Presencial ,PRESENCIAL", "remote,REMOTO"})
    void normalizaModalidade(String entrada, Modalidade esperado) {
        assertThat(Normalizacao.paraEnum(Modalidade.class, entrada, "modalidade")).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"Triagem RH,TRIAGEM_RH", "triagem-rh,TRIAGEM_RH", "ENTREVISTA_TECNICA,ENTREVISTA_TECNICA",
            "Entrevista técnica,ENTREVISTA_TECNICA", "sem retorno,SEM_RETORNO", "Candidatado,CANDIDATADO"})
    void normalizaStatus(String entrada, Status esperado) {
        assertThat(Normalizacao.paraEnum(Status.class, entrada, "status")).isEqualTo(esperado);
    }

    @Test
    void normalizaOutrosEnums() {
        assertThat(Normalizacao.paraEnum(TipoInteracao.class, "e-mail", "tipo")).isEqualTo(TipoInteracao.EMAIL);
        assertThat(Normalizacao.paraEnum(TipoInteracao.class, "Ligação", "tipo")).isEqualTo(TipoInteracao.LIGACAO);
        assertThat(Normalizacao.paraEnum(Plataforma.class, "linkedin", "plataforma")).isEqualTo(Plataforma.LINKEDIN);
        assertThat(Normalizacao.paraEnum(Plataforma.class, "Indicação", "plataforma")).isEqualTo(Plataforma.INDICACAO);
        assertThat(Normalizacao.paraEnum(Prioridade.class, "Média", "prioridade")).isEqualTo(Prioridade.MEDIA);
    }

    @Test
    void enumVazioEhNuloEInvalidoLancaErro() {
        assertThat(Normalizacao.paraEnum(Status.class, "  ", "status")).isNull();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Normalizacao.paraEnum(Status.class, "contratado amanhã", "status"))
                .withMessageContaining("status");
        assertThat(Normalizacao.paraEnumOu(Plataforma.class, "Catho", "plataforma", Plataforma.OUTRO))
                .isEqualTo(Plataforma.OUTRO);
    }

    @Test
    void aceitaDatasIsoEBrasileiras() {
        assertThat(Normalizacao.data("2026-10-24", "data")).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(Normalizacao.data("24/10/2026", "data")).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(Normalizacao.data("", "data")).isNull();
        assertThatIllegalArgumentException().isThrownBy(() -> Normalizacao.data("24-10-2026", "data"));
    }

    @Test
    void validaNota() {
        assertThat(Normalizacao.nota("8")).isEqualTo(8);
        assertThat(Normalizacao.nota("7,6")).isEqualTo(8);
        assertThatIllegalArgumentException().isThrownBy(() -> Normalizacao.nota("11"));
        assertThatIllegalArgumentException().isThrownBy(() -> Normalizacao.nota("alta"));
    }

    @Test
    void separaStackSemDuplicar() {
        assertThat(Normalizacao.stack(List.of("Java;Spring", "java", " Angular ", "Docker,AWS")))
                .containsExactly("Java", "Spring", "Angular", "Docker", "AWS");
    }
}
