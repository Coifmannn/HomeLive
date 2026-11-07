package br.com.homelive.service;

import br.com.homelive.repository.CasaRepository;
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
    private final CasaRepository casaRepository;

    @Transactional
    public EspacoComumDTO.Response cadastrar(Long casaId, EspacoComumDTO.Request request) {
        EspacoComum espaco = new EspacoComum(
                casaRepository.getReferenceById(casaId),
                request.getNome(),
                request.getDescricao(),
                request.getCapacidadeMaxima() != null ? request.getCapacidadeMaxima() : 4,
                request.getRegrasUso()
        );
        return toResponse(espacoComumRepository.save(espaco));
    }

    public List<EspacoComumDTO.Response> listarTodos(Long casaId) {
        return espacoComumRepository.findByCasaId(casaId).stream().map(this::toResponse).toList();
    }

    public List<EspacoComumDTO.Response> listarAtivos(Long casaId) {
        return espacoComumRepository.findByCasaIdAndAtivoTrue(casaId).stream().map(this::toResponse).toList();
    }

    public EspacoComumDTO.Response obterPorId(Long casaId, Long id) {
        return toResponse(buscarEntidade(casaId, id));
    }

    @Transactional
    public void alternarStatus(Long casaId, Long id) {
        EspacoComum espaco = buscarEntidade(casaId, id);
        espaco.alternarStatus();
        espacoComumRepository.save(espaco);
    }

    public EspacoComum buscarEntidade(Long casaId, Long id) {
        return espacoComumRepository.findByIdAndCasaId(id, casaId)
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
