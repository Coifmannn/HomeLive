package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rateio_itens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despesa_id", nullable = false)
    private Despesa despesa;

    @NotNull(message = "O morador associado é obrigatório")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "morador_id", nullable = false)
    private Morador morador;

    @NotNull(message = "O valor rateado é obrigatório")
    @PositiveOrZero(message = "O valor deve ser positivo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCalculado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    private LocalDate dataPagamento;

    public RateioItem(Despesa despesa, Morador morador, BigDecimal valorCalculado) {
        this.despesa = despesa;
        this.morador = morador;
        this.valorCalculado = valorCalculado;
        this.statusPagamento = StatusPagamento.PENDENTE;
    }

    public void quitar() {
        if (this.statusPagamento == StatusPagamento.PAGO) {
            throw new RegraNegocioException("Este item de rateio já foi quitado em " + this.dataPagamento);
        }
        this.statusPagamento = StatusPagamento.PAGO;
        this.dataPagamento = LocalDate.now();
    }
}
