package br.com.coliving.service;

import br.com.coliving.dto.DespesaDTO;
import br.com.coliving.exception.RecursoNaoEncontradoException;
import br.com.coliving.exception.RegraNegocioException;
import br.com.coliving.model.Despesa;
import br.com.coliving.model.Morador;
import br.com.coliving.model.RateioItem;
import br.com.coliving.model.Sindico;
import br.com.coliving.model.enums.StatusDespesa;
import br.com.coliving.model.enums.StatusPagamento;
import br.com.coliving.repository.DespesaRepository;
import br.com.coliving.repository.MoradorRepository;
import br.com.coliving.repository.RateioItemRepository;
import br.com.coliving.strategy.CalculoRateioStrategy;
import br.com.coliving.strategy.RateioStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final RateioItemRepository rateioItemRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorService moradorService;
    private final SindicoService sindicoService;
    private final RateioStrategyFactory rateioStrategyFactory;

    @Transactional
    public DespesaDTO.Response lancarDespesa(DespesaDTO.Request request) {
        Morador solicitante = null;
        if (request.getSolicitanteId() != null) {
            solicitante = moradorService.buscarEntidade(request.getSolicitanteId());
        }

        Despesa despesa = new Despesa(
                request.getTitulo(),
                request.getDescricao(),
                request.getValorTotal(),
                request.getDataVencimento(),
                request.getCategoria(),
                request.getTipoDespesa(),
                request.getTipoRateio(),
                solicitante
        );

        Despesa salva = despesaRepository.save(despesa);
        return toResponse(salva);
    }

    @Transactional
    public DespesaDTO.Response avaliarDespesa(Long despesaId, DespesaDTO.AprovacaoRequest request) {
        Despesa despesa = buscarEntidade(despesaId);
        Sindico sindico = sindicoService.buscarEntidade(request.getSindicoId());

        if (despesa.getStatus() != StatusDespesa.PENDENTE_APROVACAO) {
            throw new RegraNegocioException("Esta despesa já foi avaliada anteriormente (Status atual: " + despesa.getStatus() + ").");
        }

        if (Boolean.TRUE.equals(request.getAprovado())) {
            despesa.aprovar(sindico);

            // Executa o rateio imediatamente com a Strategy apropriada
            List<Morador> moradoresAtivos = moradorRepository.findByAtivoTrue();
            if (moradoresAtivos.isEmpty()) {
                throw new RegraNegocioException("Não há moradores ativos no sistema para calcular o rateio.");
            }

            CalculoRateioStrategy strategy = rateioStrategyFactory.obterStrategy(despesa.getTipoRateio());
            List<RateioItem> itensRateio = strategy.calcular(despesa, moradoresAtivos);

            despesa.marcarComoRateada(itensRateio);
        } else {
            despesa.rejeitar(sindico, request.getMotivoRejeicao());
        }

        Despesa atualizada = despesaRepository.save(despesa);
        return toResponse(atualizada);
    }

    @Transactional
    public DespesaDTO.RateioItemResponse quitarItemRateio(Long itemId) {
        RateioItem item = rateioItemRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de rateio não encontrado com ID: " + itemId));

        if (item.getStatusPagamento() == StatusPagamento.PAGO) {
            throw new RegraNegocioException("Este item de rateio já foi quitado em " + item.getDataPagamento());
        }

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
