package br.com.homelive.service;

import br.com.homelive.dto.LeituraConsumoDTO;
import br.com.homelive.model.LeituraConsumo;
import br.com.homelive.model.Morador;
import br.com.homelive.repository.LeituraConsumoRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeituraConsumoService {

    private final LeituraConsumoRepository leituraRepository;
    private final MoradorService moradorService;

    /** Registra a leitura do mês; se já existir uma para aquele morador e mês, ela é corrigida. */
    @Transactional
    public LeituraConsumoDTO.Response registrar(LeituraConsumoDTO.Request request, UsuarioAutenticado ator) {
        Long moradorId = ator.moradorAlvo(request.getMoradorId());
        Morador morador = moradorService.buscarEntidade(moradorId);
        YearMonth mes = request.getCompetencia() != null ? YearMonth.parse(request.getCompetencia()) : YearMonth.now();

        LeituraConsumo leitura = leituraRepository.findByMoradorIdAndCompetencia(moradorId, mes.atDay(1))
                .map(existente -> {
                    existente.corrigir(request.getKwh());
                    return existente;
                })
                .orElseGet(() -> leituraRepository.save(new LeituraConsumo(morador, mes, request.getKwh())));
        return toResponse(leitura);
    }

    public List<LeituraConsumoDTO.Response> listarPorCompetencia(YearMonth mes) {
        return leituraRepository.findByCompetencia(mes.atDay(1)).stream().map(this::toResponse).toList();
    }

    public List<LeituraConsumoDTO.Response> listarPorMorador(Long moradorId) {
        moradorService.buscarEntidade(moradorId);
        return leituraRepository.findByMoradorIdOrderByCompetenciaDesc(moradorId).stream().map(this::toResponse).toList();
    }

    public LeituraConsumoDTO.Response toResponse(LeituraConsumo l) {
        return new LeituraConsumoDTO.Response(
                l.getId(),
                l.getMorador().getId(),
                l.getMorador().getNome(),
                l.getMes().toString(),
                l.getKwh(),
                l.getRegistradaEm()
        );
    }
}
