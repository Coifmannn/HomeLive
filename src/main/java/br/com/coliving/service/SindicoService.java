package br.com.coliving.service;

import br.com.coliving.dto.SindicoDTO;
import br.com.coliving.exception.RecursoNaoEncontradoException;
import br.com.coliving.exception.RegraNegocioException;
import br.com.coliving.model.Sindico;
import br.com.coliving.repository.SindicoRepository;
import br.com.coliving.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SindicoService {

    private final SindicoRepository sindicoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public SindicoDTO.Response cadastrar(SindicoDTO.Request request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o email: " + request.getEmail());
        }

        Sindico sindico = new Sindico(
                request.getNome(),
                request.getEmail(),
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
