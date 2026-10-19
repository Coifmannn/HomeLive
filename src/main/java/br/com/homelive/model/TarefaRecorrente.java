package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoRodizio;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Modelo de uma tarefa que se repete ("tirar o lixo, toda semana"). Não é uma tarefa a cumprir:
 * a cada vez que fica devida, gera uma {@link TarefaDomestica} concreta para um morador.
 * A escolha do morador é do {@code RodizioStrategy} indicado em {@link #tipoRodizio}.
 */
@Entity
@Table(name = "tarefas_recorrentes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TarefaRecorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título da tarefa recorrente é obrigatório")
    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Periodicidade periodicidade;

    /** Quantos dias o morador tem para cumprir cada tarefa gerada. */
    @Column(nullable = false)
    private int prazoEmDias;

    private Integer pontosRecompensa = 25;

    private Integer pontosPenalidade = 15;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRodizio tipoRodizio;

    @Column(nullable = false)
    private LocalDate proximaGeracao;

    private boolean ativa = true;

    /** Quem recebeu a última tarefa gerada; o rodízio usa isso para não repetir a mesma pessoa. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ultimo_responsavel_id")
    private Morador ultimoResponsavel;

    public TarefaRecorrente(String titulo, String descricao, Periodicidade periodicidade, int prazoEmDias,
                            Integer pontosRecompensa, Integer pontosPenalidade, TipoRodizio tipoRodizio,
                            LocalDate primeiraGeracao) {
        if (titulo == null || titulo.isBlank()) {
            throw new RegraNegocioException("O título da tarefa recorrente é obrigatório.");
        }
        if (periodicidade == null) {
            throw new RegraNegocioException("A periodicidade é obrigatória.");
        }
        if (tipoRodizio == null) {
            throw new RegraNegocioException("O tipo de rodízio é obrigatório.");
        }
        if (prazoEmDias < 1) {
            throw new RegraNegocioException("O prazo de cada tarefa deve ser de ao menos 1 dia.");
        }
        LocalDate inicio = primeiraGeracao != null ? primeiraGeracao : LocalDate.now();
        if (inicio.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("A primeira geração não pode ser no passado.");
        }

        this.titulo = titulo;
        this.descricao = descricao;
        this.periodicidade = periodicidade;
        this.prazoEmDias = prazoEmDias;
        this.pontosRecompensa = pontosRecompensa != null ? pontosRecompensa : 25;
        this.pontosPenalidade = pontosPenalidade != null ? pontosPenalidade : 15;
        this.tipoRodizio = tipoRodizio;
        this.proximaGeracao = inicio;
        this.ativa = true;
    }

    public boolean devidaEm(LocalDate data) {
        return ativa && !proximaGeracao.isAfter(data);
    }

    /**
     * Gera a tarefa de hoje para o morador escolhido e avança a próxima geração. Se o sistema ficou
     * parado e várias datas se acumularam, gera uma só e pula direto para a próxima data futura.
     */
    public TarefaDomestica gerarTarefa(Morador responsavel, LocalDate hoje) {
        if (!devidaEm(hoje)) {
            throw new RegraNegocioException("A tarefa recorrente '" + titulo + "' não está devida em " + hoje + ".");
        }
        if (responsavel == null) {
            throw new RegraNegocioException("É preciso escolher um responsável para gerar a tarefa.");
        }

        // Sem síndico atribuidor: o sistema mostra "Sistema Automático".
        TarefaDomestica tarefa = new TarefaDomestica(titulo, descricao, hoje.plusDays(prazoEmDias),
                pontosRecompensa, pontosPenalidade, responsavel, null);

        this.ultimoResponsavel = responsavel;
        do {
            this.proximaGeracao = periodicidade.proxima(this.proximaGeracao);
        } while (!this.proximaGeracao.isAfter(hoje));
        return tarefa;
    }

    public void ativar() {
        this.ativa = true;
    }

    public void desativar() {
        this.ativa = false;
    }
}
