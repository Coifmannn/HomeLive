package br.com.homelive.service;

import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.dto.MoradorDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.Morador;
import br.com.homelive.repository.MoradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoradorService {

    private final MoradorRepository moradorRepository;

    public List<MoradorDTO.Response> listarTodos(Long casaId) {
        return moradorRepository.findByCasaIdAndStatusMembro(casaId, StatusMembro.ATIVO).stream().map(this::toResponse).toList();
    }

    public List<MoradorDTO.Response> listarAtivos(Long casaId) {
        return moradorRepository.findByCasaIdAndStatusMembroAndAtivoTrue(casaId, StatusMembro.ATIVO).stream().map(this::toResponse).toList();
    }

    public MoradorDTO.Response obterPorId(Long casaId, Long id) {
        return toResponse(buscarEntidade(casaId, id));
    }

    public List<MoradorDTO.Response> obterRankingGamificacao(Long casaId) {
        return moradorRepository.findByCasaIdAndStatusMembroOrderByPontosConvivenciaDesc(casaId, StatusMembro.ATIVO).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MoradorDTO.Response atualizarEstadia(Long casaId, Long id, Integer dias) {
        Morador morador = buscarEntidade(casaId, id);
        morador.atualizarEstadia(dias);
        return toResponse(moradorRepository.save(morador));
    }

    @Transactional
    public void desativar(Long casaId, Long id) {
        Morador morador = buscarEntidade(casaId, id);
        morador.desativar();
        moradorRepository.save(morador);
    }

    /** Só acha moradores ATIVOS da casa informada; de outra casa (ou pendente), responde 404 como se não existisse. */
    public Morador buscarEntidade(Long casaId, Long id) {
        return moradorRepository.findByIdAndCasaIdAndStatusMembro(id, casaId, StatusMembro.ATIVO)
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
