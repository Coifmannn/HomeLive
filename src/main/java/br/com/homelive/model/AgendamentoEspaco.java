package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.StatusAgendamento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos_espacos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgendamentoEspaco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O espaço comum é obrigatório")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "espaco_id", nullable = false)
    private EspacoComum espacoComum;

    @NotNull(message = "O morador solicitante é obrigatório")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "morador_id", nullable = false)
    private Morador morador;

    @NotNull(message = "A data e hora de início são obrigatórias")
    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "A data e hora de término são obrigatórias")
    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    private String motivoReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    private LocalDateTime dataCriacao = LocalDateTime.now();

    /**
     * Cria uma reserva válida ou falha. As invariantes da reserva vivem aqui, e não no service:
     * não existe {@code AgendamentoEspaco} em estado inválido.
     * <p>
     * A regra "não pode ser no passado" é checada só na criação (e não como anotação do campo),
     * senão cancelar uma reserva que já começou falharia na validação do Hibernate.
     */
    public AgendamentoEspaco(EspacoComum espacoComum, Morador morador, LocalDateTime dataHoraInicio,
                             LocalDateTime dataHoraFim, String motivoReserva) {
        if (espacoComum == null) {
            throw new RegraNegocioException("O espaço comum é obrigatório.");
        }
        if (morador == null) {
            throw new RegraNegocioException("O morador solicitante é obrigatório.");
        }
        if (dataHoraInicio == null || dataHoraFim == null) {
            throw new RegraNegocioException("As datas e horários de início e término são obrigatórios.");
        }
        if (!dataHoraFim.isAfter(dataHoraInicio)) {
            throw new RegraNegocioException("O horário de término deve ser posterior ao horário de início.");
        }
        if (dataHoraInicio.isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é permitido agendar reservas em horários passados.");
        }
        if (!espacoComum.isAtivo()) {
            throw new RegraNegocioException("O espaço '" + espacoComum.getNome() + "' está temporariamente inativo para reservas.");
        }
        if (!morador.isAtivo()) {
            throw new RegraNegocioException("O morador " + morador.getNome() + " está inativo no sistema.");
        }

        this.espacoComum = espacoComum;
        this.morador = morador;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.motivoReserva = motivoReserva;
        this.status = StatusAgendamento.CONFIRMADO;
        this.dataCriacao = LocalDateTime.now();
    }

    public void cancelar() {
        if (this.status == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Este agendamento já se encontra cancelado.");
        }
        if (this.status != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Esta reserva já foi encerrada e não pode ser cancelada (Status atual: " + this.status + ").");
        }
        this.status = StatusAgendamento.CANCELADO;
    }

    /** O morador não usou o espaço. Só vale para reserva confirmada que já começou. */
    public void registrarNaoComparecimento(LocalDateTime agora) {
        if (this.status != StatusAgendamento.CONFIRMADO) {
            throw new RegraNegocioException("Só é possível registrar não comparecimento de uma reserva confirmada (Status atual: " + this.status + ").");
        }
        if (agora.isBefore(this.dataHoraInicio)) {
            throw new RegraNegocioException("A reserva ainda não começou; não dá para registrar não comparecimento.");
        }
        this.status = StatusAgendamento.NAO_COMPARECEU;
    }

    /**
     * Verifica conflito temporal entre este agendamento e outro intervalo
     */
    public boolean conflitaCom(LocalDateTime outroInicio, LocalDateTime outroFim) {
        if (this.status != StatusAgendamento.CONFIRMADO) {
            return false;
        }
        // Conflito ocorre quando há sobreposição: inícioA < fimB E fimA > inícioB
        return this.dataHoraInicio.isBefore(outroFim) && this.dataHoraFim.isAfter(outroInicio);
    }
}
