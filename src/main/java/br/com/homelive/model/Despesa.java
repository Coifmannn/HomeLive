package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.CategoriaDespesa;
import br.com.homelive.model.enums.StatusDespesa;
import br.com.homelive.model.enums.TipoDespesa;
import br.com.homelive.model.enums.TipoRateio;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ciclo de vida: PENDENTE_APROVACAO -> APROVADA -> RATEADA, ou PENDENTE_APROVACAO -> REJEITADA.
 * Toda transição passa por um método desta classe, que recusa as inválidas.
 */
@Entity
@Table(name = "despesas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título da despesa é obrigatório")
    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @NotNull(message = "O valor total é obrigatório")
    @Positive(message = "O valor total deve ser positivo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @NotNull(message = "A data de vencimento é obrigatória")
    @Column(nullable = false)
    private LocalDate dataVencimento;

    private LocalDate dataLancamento = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaDespesa categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDespesa tipoDespesa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRateio tipoRateio = TipoRateio.IGUALITARIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDespesa status = StatusDespesa.PENDENTE_APROVACAO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitante_id")
    private Morador solicitante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "aprovador_id")
    private Sindico aprovador;

    private String motivoRejeicao;

    @OneToMany(mappedBy = "despesa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RateioItem> itensRateio = new ArrayList<>();

    public Despesa(String titulo, String descricao, BigDecimal valorTotal, LocalDate dataVencimento,
                   CategoriaDespesa categoria, TipoDespesa tipoDespesa, TipoRateio tipoRateio, Morador solicitante) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.valorTotal = valorTotal;
        this.dataVencimento = dataVencimento;
        this.categoria = categoria;
        this.tipoDespesa = tipoDespesa;
        this.tipoRateio = tipoRateio != null ? tipoRateio : TipoRateio.IGUALITARIO;
        this.solicitante = solicitante;
        this.status = StatusDespesa.PENDENTE_APROVACAO;
        this.dataLancamento = LocalDate.now();
    }

    /** Visão somente leitura: quem quiser alterar o rateio precisa passar por {@link #marcarComoRateada}. */
    public List<RateioItem> getItensRateio() {
        return Collections.unmodifiableList(itensRateio);
    }

    public void aprovar(Sindico sindico) {
        exigirAvaliacaoPendente(sindico);
        this.status = StatusDespesa.APROVADA;
        this.aprovador = sindico;
        this.motivoRejeicao = null;
    }

    public void rejeitar(Sindico sindico, String motivo) {
        exigirAvaliacaoPendente(sindico);
        this.status = StatusDespesa.REJEITADA;
        this.aprovador = sindico;
        this.motivoRejeicao = motivo;
    }

    public void marcarComoRateada(List<RateioItem> itens) {
        if (this.status != StatusDespesa.APROVADA) {
            throw new RegraNegocioException("Só é possível ratear uma despesa aprovada (Status atual: " + this.status + ").");
        }
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("O rateio deve conter ao menos um item.");
        }
        this.itensRateio.clear();
        this.itensRateio.addAll(itens);
        this.status = StatusDespesa.RATEADA;
    }

    private void exigirAvaliacaoPendente(Sindico sindico) {
        if (sindico == null) {
            throw new RegraNegocioException("Apenas um síndico pode avaliar uma despesa.");
        }
        if (this.status != StatusDespesa.PENDENTE_APROVACAO) {
            throw new RegraNegocioException("Esta despesa já foi avaliada anteriormente (Status atual: " + this.status + ").");
        }
    }
}
