package br.com.homelive.service;

import br.com.homelive.dto.PenalidadeDTO;
import br.com.homelive.event.MoradorNaoCompareceu;
import br.com.homelive.event.ReservaCanceladaPeloMorador;
import br.com.homelive.event.TarefaMarcadaComoAtrasada;
import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.Morador;
import br.com.homelive.model.Penalidade;
import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.enums.TipoInfracao;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.PenalidadeRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Aplica penalidades. Reage a fatos do domínio (Observer) em vez de ser chamado por quem os causa:
 * tarefas e reservas não sabem que existem penalidades.
 */
@Service
@RequiredArgsConstructor
public class PenalidadeService {

    private final PenalidadeRepository penalidadeRepository;
    private final RegraPenalidadeRepository regraRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorService moradorService;
    private final SindicoService sindicoService;

    // ---------- Reações automáticas ----------

    /**
     * Com regra ativa para tarefa atrasada, a regra manda. Sem regra, vale o valor de penalidade que a
     * própria tarefa carrega (comportamento anterior às regras configuráveis).
     */
    @EventListener
    @Transactional
    public void aoTarefaAtrasar(TarefaMarcadaComoAtrasada evento) {
        TarefaDomestica tarefa = evento.tarefa();
        Morador responsavel = tarefa.getResponsavel();
        if (responsavel == null) {
            return;
        }
        String motivo = "Tarefa '" + tarefa.getTitulo() + "' não cumprida até " + tarefa.getDataPrazo() + ".";

        Optional<RegraPenalidade> regra = regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.TAREFA_ATRASADA);
        if (regra.isPresent()) {
            aplicarRegra(responsavel, regra.get(), motivo);
        } else if (tarefa.getPontosPenalidade() != null && tarefa.getPontosPenalidade() > 0) {
            registrar(responsavel, null, TipoInfracao.TAREFA_ATRASADA, tarefa.getPontosPenalidade(), motivo, null);
        }
    }

    /** Sem regra ativa, cancelar em cima da hora não gera penalidade. */
    @EventListener
    @Transactional
    public void aoReservaCancelada(ReservaCanceladaPeloMorador evento) {
        AgendamentoEspaco reserva = evento.agendamento();
        regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.CANCELAMENTO_TARDIO)
                .filter(regra -> regra.cancelamentoEhTardio(reserva.getDataHoraInicio(), evento.quando()))
                .ifPresent(regra -> aplicarRegra(reserva.getMorador(), regra,
                        "Cancelamento tardio da reserva de '" + reserva.getEspacoComum().getNome() + "' (início previsto: "
                                + reserva.getDataHoraInicio() + ")."));
    }

    @EventListener
    @Transactional
    public void aoNaoComparecer(MoradorNaoCompareceu evento) {
        AgendamentoEspaco reserva = evento.agendamento();
        regraRepository.findByTipoInfracaoAndAtivaTrue(TipoInfracao.NAO_COMPARECIMENTO)
                .ifPresent(regra -> aplicarRegra(reserva.getMorador(), regra,
                        "Não comparecimento à reserva de '" + reserva.getEspacoComum().getNome() + "' (início: "
                                + reserva.getDataHoraInicio() + ")."));
    }

    // ---------- Aplicação manual e consultas ----------

    @Transactional
    public PenalidadeDTO.Response aplicarManual(PenalidadeDTO.ManualRequest request, Long sindicoId) {
        Morador morador = moradorService.buscarEntidade(request.getMoradorId());
        Sindico sindico = sindicoService.buscarEntidade(sindicoId);
        return toResponse(registrar(morador, null, TipoInfracao.MANUAL, request.getPontos(), request.getMotivo(), sindico));
    }

    public List<PenalidadeDTO.Response> listarTodas() {
        return penalidadeRepository.findAllByOrderByDataAplicacaoDescIdDesc().stream().map(this::toResponse).toList();
    }

    public List<PenalidadeDTO.Response> listarPorMorador(Long moradorId, UsuarioAutenticado ator) {
        ator.exigirSindicoOuDono(moradorId);
        moradorService.buscarEntidade(moradorId);
        return penalidadeRepository.findByMoradorIdOrderByDataAplicacaoDescIdDesc(moradorId).stream()
                .map(this::toResponse).toList();
    }

    // ---------- Núcleo ----------

    /** Conta as reincidências na janela da regra e deixa o modo de cálculo decidir os pontos. */
    private void aplicarRegra(Morador morador, RegraPenalidade regra, String motivo) {
        LocalDateTime desde = LocalDateTime.now().minusDays(regra.getJanelaReincidenciaDias());
        long reincidencias = penalidadeRepository.countByMoradorIdAndTipoInfracaoAndDataAplicacaoGreaterThanEqual(
                morador.getId(), regra.getTipoInfracao(), desde);
        registrar(morador, regra, regra.getTipoInfracao(), regra.pontosPara(reincidencias), motivo, null);
    }

    private Penalidade registrar(Morador morador, RegraPenalidade regra, TipoInfracao tipo, int pontos,
                                 String motivo, Sindico aplicadaPor) {
        Penalidade penalidade = Penalidade.aplicar(morador, regra, tipo, pontos, motivo, aplicadaPor);
        moradorRepository.save(morador);
        return penalidadeRepository.save(penalidade);
    }

    public PenalidadeDTO.Response toResponse(Penalidade p) {
        return new PenalidadeDTO.Response(
                p.getId(),
                p.getMorador().getId(),
                p.getMorador().getNome(),
                p.getRegra() != null ? p.getRegra().getId() : null,
                p.getRegra() != null ? p.getRegra().getNome() : null,
                p.getTipoInfracao(),
                p.getPontos(),
                p.getMotivo(),
                p.getDataAplicacao(),
                p.getAplicadaPor() != null ? p.getAplicadaPor().getNome() : "Sistema Automático"
        );
    }
}
