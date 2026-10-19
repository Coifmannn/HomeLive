package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.StatusTarefa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarefas_domesticas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TarefaDomestica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título da tarefa é obrigatório")
    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @NotNull(message = "O prazo da tarefa é obrigatório")
    @Column(nullable = false)
    private LocalDate dataPrazo;

    private Integer pontosRecompensa = 25;

    private Integer pontosPenalidade = 15;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTarefa status = StatusTarefa.PENDENTE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private Morador responsavel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "atribuida_por_id")
    private Sindico atribuidaPor;

    private LocalDateTime dataConclusao;

    public TarefaDomestica(String titulo, String descricao, LocalDate dataPrazo, Integer pontosRecompensa,
                           Integer pontosPenalidade, Morador responsavel, Sindico atribuidaPor) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataPrazo = dataPrazo;
        this.pontosRecompensa = pontosRecompensa != null ? pontosRecompensa : 25;
        this.pontosPenalidade = pontosPenalidade != null ? pontosPenalidade : 15;
        this.responsavel = responsavel;
        this.atribuidaPor = atribuidaPor;
        this.status = StatusTarefa.PENDENTE;
    }

    public void concluir() {
        if (this.status == StatusTarefa.CONCLUIDA) {
            throw new RegraNegocioException("Esta tarefa já foi concluída anteriormente.");
        }
        if (this.status == StatusTarefa.CANCELADA) {
            throw new RegraNegocioException("Uma tarefa cancelada não pode ser concluída.");
        }
        this.status = StatusTarefa.CONCLUIDA;
        this.dataConclusao = LocalDateTime.now();
        if (this.responsavel != null) {
            this.responsavel.adicionarPontos(this.pontosRecompensa);
        }
    }

    public void penalizarAtraso() {
        if (this.status == StatusTarefa.PENDENTE && LocalDate.now().isAfter(this.dataPrazo)) {
            this.status = StatusTarefa.ATRASADA;
            if (this.responsavel != null) {
                this.responsavel.deduzirPontos(this.pontosPenalidade);
            }
        }
    }
}
