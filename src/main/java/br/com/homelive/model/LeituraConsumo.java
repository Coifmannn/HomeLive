package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Leitura de consumo (kWh) de um morador em um mês. É o dado que o rateio por consumo usa: uma conta de
 * setembro é dividida pelas leituras de setembro, não pelo que o morador consome hoje.
 * Há no máximo uma leitura por morador e mês; registrar de novo corrige a anterior.
 */
@Entity
@Table(name = "leituras_consumo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"morador_id", "competencia"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeituraConsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "morador_id", nullable = false)
    private Morador morador;

    /** Sempre o primeiro dia do mês de referência. */
    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal kwh;

    private LocalDateTime registradaEm;

    public LeituraConsumo(Morador morador, YearMonth competencia, BigDecimal kwh) {
        if (morador == null) {
            throw new RegraNegocioException("O morador da leitura é obrigatório.");
        }
        if (competencia == null) {
            throw new RegraNegocioException("A competência da leitura é obrigatória.");
        }
        if (competencia.isAfter(YearMonth.now())) {
            throw new RegraNegocioException("Não é possível registrar leitura de um mês futuro.");
        }
        validarKwh(kwh);
        this.morador = morador;
        this.competencia = competencia.atDay(1);
        this.kwh = kwh;
        this.registradaEm = LocalDateTime.now();
    }

    public void corrigir(BigDecimal novoKwh) {
        validarKwh(novoKwh);
        this.kwh = novoKwh;
        this.registradaEm = LocalDateTime.now();
    }

    public YearMonth getMes() {
        return YearMonth.from(competencia);
    }

    private static void validarKwh(BigDecimal kwh) {
        if (kwh == null || kwh.signum() < 0) {
            throw new RegraNegocioException("O consumo deve ser zero ou positivo.");
        }
    }
}
