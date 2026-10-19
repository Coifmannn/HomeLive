package br.com.homelive.service;

import br.com.homelive.dto.DespesaDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Despesa;
import br.com.homelive.model.LeituraConsumo;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RateioItem;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.enums.StatusDespesa;
import br.com.homelive.repository.DespesaRepository;
import br.com.homelive.repository.LeituraConsumoRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.RateioItemRepository;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.strategy.CalculoRateioStrategy;
import br.com.homelive.strategy.ContextoRateio;
import br.com.homelive.strategy.RateioStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final RateioItemRepository rateioItemRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorService moradorService;
    private final SindicoService sindicoService;
    private final RateioStrategyFactory rateioStrategyFactory;
    private final LeituraConsumoRepository leituraConsumoRepository;

    @Transactional
    public DespesaDTO.Response lancarDespesa(DespesaDTO.Request request, UsuarioAutenticado ator) {
        // Morador lança em nome próprio; lançamento do síndico é administrativo (sem solicitante).
        Morador solicitante = ator.isSindico() ? null : moradorService.buscarEntidade(ator.getId());

        Despesa despesa = new Despesa(
                request.getTitulo(),
                request.getDescricao(),
                request.getValorTotal(),
                request.getDataVencimento(),
                request.getCategoria(),
                request.getTipoDespesa(),
                request.getTipoRateio(),
                solicitante,
                request.getCompetencia() != null ? YearMonth.parse(request.getCompetencia()) : null
        );

        Despesa salva = despesaRepository.save(despesa);
        return toResponse(salva);
    }

    @Transactional
    public DespesaDTO.Response avaliarDespesa(Long despesaId, DespesaDTO.AprovacaoRequest request, Long sindicoId) {
        Despesa despesa = buscarEntidade(despesaId);
        Sindico sindico = sindicoService.buscarEntidade(sindicoId);

        if (Boolean.TRUE.equals(request.getAprovado())) {
            despesa.aprovar(sindico);

            // Executa o rateio imediatamente com a Strategy apropriada
            List<Morador> moradoresAtivos = moradorRepository.findByAtivoTrue();
            if (moradoresAtivos.isEmpty()) {
                throw new RegraNegocioException("Não há moradores ativos no sistema para calcular o rateio.");
            }

            CalculoRateioStrategy strategy = rateioStrategyFactory.obterStrategy(despesa.getTipoRateio());
            List<RateioItem> itensRateio = strategy.calcular(despesa, moradoresAtivos, contextoDoMes(despesa));

            despesa.marcarComoRateada(itensRateio);
        } else {
            despesa.rejeitar(sindico, request.getMotivoRejeicao());
        }

        Despesa atualizada = despesaRepository.save(despesa);
        return toResponse(atualizada);
    }

    @Transactional
    public DespesaDTO.RateioItemResponse quitarItemRateio(Long itemId, UsuarioAutenticado ator) {
        RateioItem item = rateioItemRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de rateio não encontrado com ID: " + itemId));

        ator.exigirSindicoOuDono(item.getMorador().getId());

        item.quitar();
        RateioItem salvo = rateioItemRepository.save(item);
        return toRateioItemResponse(salvo);
    }

    public List<DespesaDTO.Response> listarTodas() {
        return despesaRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<DespesaDTO.Response> listarPorStatus(StatusDespesa status) {
        return despesaRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    public DespesaDTO.Response obterPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    public List<DespesaDTO.RateioItemResponse> listarRateiosPorMorador(Long moradorId) {
        return rateioItemRepository.findByMoradorId(moradorId).stream()
                .map(this::toRateioItemResponse)
                .toList();
    }

    /** Leituras de consumo do mês da despesa, no formato que as estratégias esperam. */
    private ContextoRateio contextoDoMes(Despesa despesa) {
        Map<Long, BigDecimal> consumo = leituraConsumoRepository.findByCompetencia(despesa.getCompetencia()).stream()
                .collect(Collectors.toMap(l -> l.getMorador().getId(), LeituraConsumo::getKwh));
        return new ContextoRateio(consumo);
    }

    public Despesa buscarEntidade(Long id) {
        return despesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com o ID: " + id));
    }

    public DespesaDTO.Response toResponse(Despesa d) {
        List<DespesaDTO.RateioItemResponse> itens = d.getItensRateio() != null ?
                d.getItensRateio().stream().map(this::toRateioItemResponse).toList() : List.of();

        return new DespesaDTO.Response(
                d.getId(),
                d.getTitulo(),
                d.getDescricao(),
                d.getValorTotal(),
                d.getDataVencimento(),
                d.getDataLancamento(),
                d.getMesCompetencia().toString(),
                d.getCategoria(),
                d.getTipoDespesa(),
                d.getTipoRateio(),
                d.getStatus(),
                d.getSolicitante() != null ? d.getSolicitante().getNome() : "Lançamento Administrativo",
                d.getAprovador() != null ? d.getAprovador().getNome() : null,
                d.getMotivoRejeicao(),
                itens
        );
    }

    public DespesaDTO.RateioItemResponse toRateioItemResponse(RateioItem item) {
        return new DespesaDTO.RateioItemResponse(
                item.getId(),
                item.getMorador().getId(),
                item.getMorador().getNome(),
                item.getMorador().getNumeroQuarto(),
                item.getValorCalculado(),
                item.getStatusPagamento(),
                item.getDataPagamento()
        );
    }
}
