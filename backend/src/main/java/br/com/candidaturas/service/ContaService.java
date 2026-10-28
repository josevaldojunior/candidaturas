package br.com.candidaturas.service;

import br.com.candidaturas.domain.Usuario;
import br.com.candidaturas.dto.ContaDtos.*;
import br.com.candidaturas.repository.CandidaturaRepository;
import br.com.candidaturas.repository.ContatoRepository;
import br.com.candidaturas.repository.UsuarioRepository;
import br.com.candidaturas.seguranca.TokenIntegracao;
import br.com.candidaturas.seguranca.UsuarioAtual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

/** Cadastro, login (JWT), dados da conta e token de integração do agente. */
@Service
public class ContaService {

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);
    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final ContatoRepository contatoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final UsuarioAtual usuarioAtual;
    private final PlataformaService plataformas;
    private final Clock clock;
    private final boolean cadastroAberto;
    private final Duration validadeSessao;
    /** Hash usado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos. */
    private final String hashFalso;

    public ContaService(UsuarioRepository usuarioRepository, CandidaturaRepository candidaturaRepository,
                        ContatoRepository contatoRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                        UsuarioAtual usuarioAtual, Clock clock, PlataformaService plataformas,
                        @Value("${app.seguranca.cadastro-aberto:true}") boolean cadastroAberto,
                        @Value("${app.seguranca.sessao-horas:12}") long sessaoHoras) {
        this.usuarioRepository = usuarioRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.contatoRepository = contatoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
        this.plataformas = plataformas;
        this.cadastroAberto = cadastroAberto;
        this.validadeSessao = Duration.ofHours(sessaoHoras);
        this.hashFalso = passwordEncoder.encode("senha-que-nao-existe");
    }

    public ConfigPublica config() {
        // a primeira conta sempre pode ser criada, mesmo com o cadastro fechado
        return new ConfigPublica(cadastroAberto || usuarioRepository.count() == 0);
    }

    /**
     * Cria a conta. A primeira conta do sistema assume os registros criados antes de existirem
     * contas (instalações antigas, de um usuário só).
     */
    @Transactional
    public SessaoResponse cadastrar(CadastroRequest req) {
        boolean primeira = usuarioRepository.count() == 0;
        if (!cadastroAberto && !primeira) {
            throw new RegraNegocioException("O cadastro de novas contas está fechado");
        }
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setCriadoEm(LocalDateTime.now(clock));
        usuarioRepository.saveAndFlush(u);
        plataformas.inicializar(u.getId());
        if (primeira) {
            int candidaturas = candidaturaRepository.assumirSemDono(u.getId());
            int contatos = contatoRepository.assumirSemDono(u.getId());
            if (candidaturas + contatos > 0) {
                log.info("Primeira conta ({}) assumiu {} candidatura(s) e {} contato(s) já existentes",
                        email, candidaturas, contatos);
            }
        }
        return sessao(u);
    }

    @Transactional(readOnly = true)
    public SessaoResponse login(LoginRequest req) {
        var usuario = usuarioRepository.findByEmail(normalizarEmail(req.email()));
        boolean senhaOk = passwordEncoder.matches(req.senha(), usuario.map(Usuario::getSenhaHash).orElse(hashFalso));
        if (usuario.isEmpty() || !senhaOk) {
            throw new NaoAutorizadoException(CREDENCIAIS_INVALIDAS);
        }
        return sessao(usuario.get());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse eu() {
        return response(atual());
    }

    @Transactional
    public void trocarSenha(TrocaSenhaRequest req) {
        Usuario u = atual();
        if (!passwordEncoder.matches(req.senhaAtual(), u.getSenhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta");
        }
        u.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
    }

    @Transactional(readOnly = true)
    public TokenIntegracaoStatus statusToken() {
        Usuario u = atual();
        return new TokenIntegracaoStatus(u.getTokenIntegracaoHash() != null, u.getTokenIntegracaoCriadoEm());
    }

    /** Gera um token novo (o anterior para de funcionar). O valor só é devolvido agora. */
    @Transactional
    public TokenIntegracaoGerado gerarToken() {
        Usuario u = atual();
        String token = TokenIntegracao.gerar();
        u.setTokenIntegracaoHash(TokenIntegracao.hash(token));
        u.setTokenIntegracaoCriadoEm(LocalDateTime.now(clock));
        return new TokenIntegracaoGerado(token, u.getTokenIntegracaoCriadoEm());
    }

    @Transactional
    public void revogarToken() {
        Usuario u = atual();
        u.setTokenIntegracaoHash(null);
        u.setTokenIntegracaoCriadoEm(null);
    }

    private Usuario atual() {
        Long id = usuarioAtual.id();
        return usuarioRepository.findById(id).orElseThrow(() -> new NaoAutorizadoException("Conta não encontrada"));
    }

    private SessaoResponse sessao(Usuario u) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(validadeSessao);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("candidaturas")
                .subject(String.valueOf(u.getId()))
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("email", u.getEmail())
                .claim("nome", u.getNome())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new SessaoResponse(token, expira, response(u));
    }

    private static UsuarioResponse response(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getCriadoEm());
    }

    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
