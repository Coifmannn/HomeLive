package br.com.homelive.service;

import br.com.homelive.dto.AuthDTO;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.Usuario;
import br.com.homelive.repository.UsuarioRepository;
import br.com.homelive.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final String MENSAGEM_PADRAO = "E-mail ou senha incorretos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MoradorService moradorService;
    private final SindicoService sindicoService;
    /** Hash de uma senha qualquer: gasta o mesmo tempo de BCrypt quando o e-mail não existe. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       MoradorService moradorService, SindicoService sindicoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.moradorService = moradorService;
        this.sindicoService = sindicoService;
        this.hashFalso = passwordEncoder.encode("senha-que-ninguem-usa");
    }

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(request.getEmail().trim());

        // Confere a senha mesmo sem usuário, para não revelar pelo tempo de resposta se o e-mail existe.
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = passwordEncoder.matches(request.getSenha(), hash);

        // Mesma resposta para e-mail inexistente, senha errada e conta desativada.
        if (encontrado.isEmpty() || !senhaConfere || !encontrado.get().isAtivo()) {
            throw new BadCredentialsException(MENSAGEM_PADRAO);
        }

        Usuario usuario = encontrado.get();
        var token = tokenService.gerar(usuario.getEmail(), usuario.getTipoUsuario().name(), request.querLembrar());
        return new AuthDTO.LoginResponse(token.valor(), token.expiraEm(), usuario.getTipoUsuario(), perfilDe(usuario));
    }

    private Object perfilDe(Usuario usuario) {
        if (usuario instanceof Morador morador) {
            return moradorService.toResponse(morador);
        }
        return sindicoService.toResponse((Sindico) usuario);
    }
}
