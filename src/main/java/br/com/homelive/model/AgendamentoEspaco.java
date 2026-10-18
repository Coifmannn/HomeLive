package br.com.homelive.model;

import br.com.homelive.model.enums.StatusAgendamento;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos_espacos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    @FutureOrPresent(message = "O agendamento não pode ser realizado no passado")
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

    public AgendamentoEspaco(EspacoComum espacoComum, Morador morador, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, String motivoReserva) {
        this.espacoComum = espacoComum;
        this.morador = morador;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.motivoReserva = motivoReserva;
        this.status = StatusAgendamento.CONFIRMADO;
        this.dataCriacao = LocalDateTime.now();
    }

    public void cancelar() {
        this.status = StatusAgendamento.CANCELADO;
    }

    /**
     * Verifica conflito temporal entre este agendamento e outro intervalo
     */
    public boolean conflitaCom(LocalDateTime outroInicio, LocalDateTime outroFim) {
        if (this.status == StatusAgendamento.CANCELADO) {
            return false;
        }
        // Conflito ocorre quando há sobreposição: inícioA < fimB E fimA > inícioB
        return this.dataHoraInicio.isBefore(outroFim) && this.dataHoraFim.isAfter(outroInicio);
    }
}
