package br.com.homelive.service;

import br.com.homelive.repository.CasaRepository;
import br.com.homelive.dto.RegraPenalidadeDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.repository.RegraPenalidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegraPenalidadeService {

    private final RegraPenalidadeRepository regraRepository;
    private final CasaRepository casaRepository;

    @Transactional
    public RegraPenalidadeDTO.Response criar(Long casaId, RegraPenalidadeDTO.Request request) {
        RegraPenalidade regra = new RegraPenalidade(
                casaRepository.getReferenceById(casaId),
                request.getNome(),
                request.getTipoInfracao(),
                request.getModoCalculo(),
                request.getPontosBase() != null ? request.getPontosBase() : 0,
                request.getIncrementoPorReincidencia(),
                request.getTetoPontos(),
                request.getJanelaReincidenciaDias(),
                request.getLimiteHorasAntecedencia()
        );
        exigirSemOutraAtiva(regra);
        return toResponse(regraRepository.save(regra));
    }

    public List<RegraPenalidadeDTO.Response> listarTodas(Long casaId) {
        return regraRepository.findByCasaId(casaId).stream().map(this::toResponse).toList();
    }

    public RegraPenalidadeDTO.Response obterPorId(Long casaId, Long id) {
        return toResponse(buscarEntidade(casaId, id));
    }

    @Transactional
    public RegraPenalidadeDTO.Response ativar(Long casaId, Long id) {
        RegraPenalidade regra = buscarEntidade(casaId, id);
        if (!regra.isAtiva()) {
            exigirSemOutraAtiva(regra);
            regra.ativar();
        }
        return toResponse(regra);
    }

    @Transactional
    public RegraPenalidadeDTO.Response desativar(Long casaId, Long id) {
        RegraPenalidade regra = buscarEntidade(casaId, id);
        regra.desativar();
        return toResponse(regra);
    }

    /** Duas regras ativas para a mesma infração tornariam ambíguo qual vale, então só uma por tipo. */
    private void exigirSemOutraAtiva(RegraPenalidade regra) {
        if (regraRepository.existsByCasaIdAndTipoInfracaoAndAtivaTrue(regra.getCasa().getId(), regra.getTipoInfracao())) {
            throw new RegraNegocioException("Já existe uma regra ativa para " + regra.getTipoInfracao()
                    + ". Desative-a antes de ativar ou criar outra.");
        }
    }

    public RegraPenalidade buscarEntidade(Long casaId, Long id) {
        return regraRepository.findByIdAndCasaId(id, casaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Regra de penalidade não encontrada com o ID: " + id));
    }

    public RegraPenalidadeDTO.Response toResponse(RegraPenalidade r) {
        return new RegraPenalidadeDTO.Response(
                r.getId(),
                r.getNome(),
                r.getTipoInfracao(),
                r.getModoCalculo(),
                r.getPontosBase(),
                r.getIncrementoPorReincidencia(),
                r.getTetoPontos(),
                r.getJanelaReincidenciaDias(),
                r.getLimiteHorasAntecedencia(),
                r.isAtiva()
        );
    }
}
