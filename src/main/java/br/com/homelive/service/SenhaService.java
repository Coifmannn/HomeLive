package br.com.homelive.service;

import br.com.homelive.dto.SenhaDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Usuario;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.UsuarioRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Senhas depois do cadastro. Não há e-mail no sistema, então "esqueci a senha" funciona assim: o síndico gera uma
 * senha temporária para o morador, que entra com ela e troca por uma própria.
 */
@Service
@RequiredArgsConstructor
public class SenhaService {

    /** Sem I, O, l, 0 e 1, que se confundem quando o síndico passa a senha para o morador. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TAMANHO_TEMPORARIA = 8;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final MoradorRepository moradorRepository;
    private final PasswordEncoder passwordEncoder;

    /** A própria pessoa troca a senha, provando que sabe a atual. */
    @Transactional
    public void alterarPropria(UsuarioAutenticado ator, SenhaDTO.AlterarRequest request) {
        Usuario usuario = usuarioRepository.findById(ator.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        // 400 e não 401: um 401 faria o front achar que a sessão acabou e deslogar a pessoa.
        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        if (request.getSenhaAtual().equals(request.getNovaSenha())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        usuario.trocarSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
    }

    /** O síndico gera uma senha temporária para um morador. Devolve o texto uma única vez; no banco fica só o hash. */
    @Transactional
    public SenhaDTO.RedefinicaoResponse redefinirDoMorador(Long moradorId) {
        Morador morador = moradorRepository.findById(moradorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado com o ID: " + moradorId));

        String temporaria = gerarSenhaTemporaria();
        morador.trocarSenhaHash(passwordEncoder.encode(temporaria));
        moradorRepository.save(morador);
        return new SenhaDTO.RedefinicaoResponse(temporaria);
    }

    private String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < TAMANHO_TEMPORARIA; i++) {
            senha.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
