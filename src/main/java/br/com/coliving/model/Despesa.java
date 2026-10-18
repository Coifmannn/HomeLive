package br.com.coliving.model;

import br.com.coliving.model.enums.CategoriaDespesa;
import br.com.coliving.model.enums.StatusDespesa;
import br.com.coliving.model.enums.TipoDespesa;
import br.com.coliving.model.enums.TipoRateio;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "despesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public void aprovar(Sindico sindico) {
        this.status = StatusDespesa.APROVADA;
        this.aprovador = sindico;
        this.motivoRejeicao = null;
    }

    public void rejeitar(Sindico sindico, String motivo) {
        this.status = StatusDespesa.REJEITADA;
        this.aprovador = sindico;
        this.motivoRejeicao = motivo;
    }

    public void marcarComoRateada(List<RateioItem> itens) {
        this.itensRateio.clear();
        if (itens != null) {
            this.itensRateio.addAll(itens);
        }
        this.status = StatusDespesa.RATEADA;
    }
}
