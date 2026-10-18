package br.com.homelive.service;

import br.com.homelive.dto.EspacoComumDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.EspacoComum;
import br.com.homelive.repository.EspacoComumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspacoComumService {

    private final EspacoComumRepository espacoComumRepository;

    @Transactional
    public EspacoComumDTO.Response cadastrar(EspacoComumDTO.Request request) {
        EspacoComum espaco = new EspacoComum(
                request.getNome(),
                request.getDescricao(),
                request.getCapacidadeMaxima() != null ? request.getCapacidadeMaxima() : 4,
                request.getRegrasUso()
        );
        return toResponse(espacoComumRepository.save(espaco));
    }

    public List<EspacoComumDTO.Response> listarTodos() {
        return espacoComumRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<EspacoComumDTO.Response> listarAtivos() {
        return espacoComumRepository.findByAtivoTrue().stream().map(this::toResponse).toList();
    }

    public EspacoComumDTO.Response obterPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public void alternarStatus(Long id) {
        EspacoComum espaco = buscarEntidade(id);
        espaco.setAtivo(!espaco.isAtivo());
        espacoComumRepository.save(espaco);
    }

    public EspacoComum buscarEntidade(Long id) {
        return espacoComumRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Espaço comum não encontrado com o ID: " + id));
    }

    public EspacoComumDTO.Response toResponse(EspacoComum e) {
        return new EspacoComumDTO.Response(
                e.getId(),
                e.getNome(),
                e.getDescricao(),
                e.getCapacidadeMaxima(),
                e.getRegrasUso(),
                e.isAtivo()
        );
    }
}
