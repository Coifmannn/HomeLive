package br.com.homelive.service;

import br.com.homelive.dto.MoradorDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Morador;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoradorService {

    private final MoradorRepository moradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MoradorDTO.Response cadastrar(MoradorDTO.Request request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o email: " + request.getEmail());
        }

        Morador morador = new Morador(
                request.getNome(),
                request.getEmail(),
                passwordEncoder.encode(request.getSenha()),
                request.getTelefone(),
                request.getNumeroQuarto(),
                request.getDiasEstadia()
        );

        Morador salvo = moradorRepository.save(morador);
        return toResponse(salvo);
    }

    public List<MoradorDTO.Response> listarTodos() {
        return moradorRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<MoradorDTO.Response> listarAtivos() {
        return moradorRepository.findByAtivoTrue().stream().map(this::toResponse).toList();
    }

    public MoradorDTO.Response obterPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    public List<MoradorDTO.Response> obterRankingGamificacao() {
        return moradorRepository.findAllByOrderByPontosConvivenciaDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MoradorDTO.Response atualizarEstadia(Long id, Integer dias) {
        Morador morador = buscarEntidade(id);
        morador.atualizarEstadia(dias);
        return toResponse(moradorRepository.save(morador));
    }

    @Transactional
    public void desativar(Long id) {
        Morador morador = buscarEntidade(id);
        morador.desativar();
        moradorRepository.save(morador);
    }

    public Morador buscarEntidade(Long id) {
        return moradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado com o ID: " + id));
    }

    public MoradorDTO.Response toResponse(Morador m) {
        return new MoradorDTO.Response(
                m.getId(),
                m.getNome(),
                m.getEmail(),
                m.getTelefone(),
                m.getNumeroQuarto(),
                m.getDiasEstadia(),
                m.getPontosConvivencia(),
                m.getDataEntrada(),
                m.isAtivo()
        );
    }
}
