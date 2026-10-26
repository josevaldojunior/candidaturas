package br.com.candidaturas.service;

import br.com.candidaturas.domain.Usuario;
import br.com.candidaturas.dto.ContaDtos.CadastroRequest;
import br.com.candidaturas.dto.ContaDtos.SessaoResponse;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.repository.UsuarioRepository;
import br.com.candidaturas.support.RelogioFixo;
import br.com.candidaturas.support.UsuarioFixo;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CandidaturaRepository candidaturaRepository;
    @Mock
    private ContatoRepository contatoRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final NimbusJwtEncoder jwtEncoder =
            new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(new byte[32], "HmacSHA256")));

    @BeforeEach
    void setUp() {
        lenient().when(usuarioRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
    }

    private ContaService service(boolean cadastroAberto) {
        return new ContaService(usuarioRepository, candidaturaRepository, contatoRepository, encoder, jwtEncoder,
                new UsuarioFixo(), RelogioFixo.clock(), cadastroAberto, 12);
    }

    @Test
    void primeiraContaAssumeOsDadosQueJaExistiam() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(candidaturaRepository.assumirSemDono(7L)).thenReturn(14);

        SessaoResponse s = service(false).cadastrar(new CadastroRequest(" Josevaldo ", " Eu@Exemplo.com ", "senha-forte-1"));

        verify(candidaturaRepository).assumirSemDono(7L);
        verify(contatoRepository).assumirSemDono(7L);
        assertThat(s.usuario().email()).isEqualTo("eu@exemplo.com");
        assertThat(s.usuario().nome()).isEqualTo("Josevaldo");
        assertThat(s.token()).isNotBlank();
        assertThat(s.expiraEm()).isEqualTo(RelogioFixo.AGORA.plusSeconds(12 * 3600));
    }

    @Test
    void contasSeguintesNaoMexemNosDadosDeNinguem() {
        when(usuarioRepository.count()).thenReturn(1L);

        service(true).cadastrar(new CadastroRequest("Outra", "outra@exemplo.com", "senha-forte-1"));

        verify(candidaturaRepository, never()).assumirSemDono(any());
        verify(contatoRepository, never()).assumirSemDono(any());
    }

    @Test
    void cadastroFechadoSoPermiteAPrimeiraConta() {
        when(usuarioRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> service(false).cadastrar(new CadastroRequest("X", "x@exemplo.com", "senha-forte-1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fechado");
    }

    @Test
    void senhaFicaSoComoHash() {
        when(usuarioRepository.count()).thenReturn(1L);

        service(true).cadastrar(new CadastroRequest("Ana", "ana@exemplo.com", "senha-forte-1"));

        verify(usuarioRepository).saveAndFlush(argThat(u ->
                !u.getSenhaHash().contains("senha-forte-1") && encoder.matches("senha-forte-1", u.getSenhaHash())));
    }
}
