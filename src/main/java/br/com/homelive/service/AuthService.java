package br.com.homelive.service;

import br.com.homelive.dto.AuthDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Usuario;
import br.com.homelive.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria só a conta. Nasce como {@link Morador} sem casa; se a pessoa fundar uma casa,
     * {@link CasaService#criar} troca esse perfil pelo de síndico.
     */
    @Transactional
    public AuthDTO.Perfil cadastrar(AuthDTO.CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o email: " + request.getEmail());
        }
        Morador novo = new Morador(request.getNome(), request.getEmail(),
                passwordEncoder.encode(request.getSenha()), request.getTelefone());
        return toPerfil(usuarioRepository.save(novo));
    }

    @Transactional(readOnly = true)
    public AuthDTO.Perfil perfilDe(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        return toPerfil(usuario);
    }

    static AuthDTO.Perfil toPerfil(Usuario u) {
        return new AuthDTO.Perfil(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getTipoUsuario(),
                u.getStatusMembro(),
                u.getCasa() != null ? u.getCasa().getId() : null,
                u.getCasa() != null ? u.getCasa().getNome() : null
        );
    }
}
