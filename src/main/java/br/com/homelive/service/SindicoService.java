package br.com.homelive.service;

import br.com.homelive.dto.SindicoDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Sindico;
import br.com.homelive.repository.SindicoRepository;
import br.com.homelive.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SindicoService {

    private final SindicoRepository sindicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SindicoDTO.Response cadastrar(SindicoDTO.Request request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o email: " + request.getEmail());
        }

        Sindico sindico = new Sindico(
                request.getNome(),
                request.getEmail(),
                passwordEncoder.encode(request.getSenha()),
                request.getTelefone(),
                request.getDepartamento()
        );

        Sindico salvo = sindicoRepository.save(sindico);
        return toResponse(salvo);
    }

    public List<SindicoDTO.Response> listarTodos() {
        return sindicoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public SindicoDTO.Response obterPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    public Sindico buscarEntidade(Long id) {
        return sindicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Síndico não encontrado com o ID: " + id));
    }

    public SindicoDTO.Response toResponse(Sindico s) {
        return new SindicoDTO.Response(
                s.getId(),
                s.getNome(),
                s.getEmail(),
                s.getTelefone(),
                s.getDepartamento(),
                s.getInicioMandato(),
                s.getFimMandato(),
                s.isAtivo()
        );
    }
}
