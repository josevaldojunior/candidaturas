package br.com.candidaturas.seguranca;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SegurancaConfigTest {

    private final SegurancaConfig config = new SegurancaConfig();

    @Test
    void emProducaoExigeSegredoForte() {
        assertThatIllegalStateException().isThrownBy(() -> config.chaveJwt("", true))
                .withMessageContaining("JWT_SEGREDO");
        assertThatIllegalStateException().isThrownBy(() -> config.chaveJwt("curto", true));
        assertThatIllegalStateException()
                .isThrownBy(() -> config.chaveJwt("troque-por-um-valor-aleatorio-de-pelo-menos-32-caracteres", true));

        assertThat(config.chaveJwt("um-segredo-aleatorio-com-mais-de-32-caracteres", true).getEncoded()).hasSize(32);
    }

    @Test
    void foraDeProducaoGeraChaveTemporariaSemSegredo() {
        assertThat(config.chaveJwt("", false).getEncoded()).hasSize(32);
        // a mesma configuração sempre gera a mesma chave (logins continuam válidos após reiniciar)
        assertThat(config.chaveJwt("abc", false).getEncoded()).isEqualTo(config.chaveJwt("abc", false).getEncoded());
    }
}
