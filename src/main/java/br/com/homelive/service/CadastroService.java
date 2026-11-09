package br.com.homelive.service;

import br.com.homelive.config.SugestoesIniciaisCasa;
import br.com.homelive.dto.AuthDTO;
import br.com.homelive.dto.CadastroDTO;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Casa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.Usuario;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro público, com dois caminhos que dependem de a residência já existir:
 * <ul>
 *   <li><b>Não existe</b>: a pessoa cria a residência e vira o <b>síndico</b>;</li>
 *   <li><b>Já existe</b>: a pessoa entra com o código de convite e vira <b>morador</b>.</li>
 * </ul>
 * Nos dois casos já sai logada (token), sem precisar passar pela tela de login.
 */
@Service
@RequiredArgsConstructor
public class CadastroService {

    private final UsuarioRepository usuarioRepository;
    private final SindicoRepository sindicoRepository;
    private final MoradorRepository moradorRepository;
    private final CasaService casaService;
    private final SugestoesIniciaisCasa sugestoes;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional
    public AuthDTO.LoginResponse cadastrar(CadastroDTO.Request request) {
        // O e-mail é guardado em minúsculas: o login do front também envia em minúsculas.
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }

        Usuario novo = casaService.buscarCasa().isPresent()
                ? entrarComoMorador(request, email, casaService.casaAtual())
                : criarCasaComoSindico(request, email);

        return authService.respostaDeLogin(novo, request.querLembrar());
    }

    private Sindico criarCasaComoSindico(CadastroDTO.Request request, String email) {
        if (request.getNomeCasa() == null || request.getNomeCasa().isBlank()) {
            throw new RegraNegocioException("Informe o nome da residência que você vai criar.");
        }
        casaService.criar(request.getNomeCasa());

        Sindico sindico = sindicoRepository.save(new Sindico(
                request.getNome().trim(), email, passwordEncoder.encode(request.getSenha()), request.getTelefone(), null));

        if (request.querSugestoes()) {
            sugestoes.aplicar();
        }
        return sindico;
    }

    private Morador entrarComoMorador(CadastroDTO.Request request, String email, Casa casa) {
        if (request.getCodigoConvite() == null || request.getCodigoConvite().isBlank()) {
            throw new RegraNegocioException("A residência já existe. Informe o código de convite que o síndico passou.");
        }
        if (!casa.aceitaCodigo(request.getCodigoConvite())) {
            throw new RegraNegocioException("Código de convite inválido.");
        }
        if (request.getNumeroQuarto() == null || request.getNumeroQuarto().isBlank()) {
            throw new RegraNegocioException("Informe o seu quarto.");
        }
        return moradorRepository.save(new Morador(
                request.getNome().trim(), email, passwordEncoder.encode(request.getSenha()), request.getTelefone(),
                request.getNumeroQuarto().trim(), null));
    }
}
