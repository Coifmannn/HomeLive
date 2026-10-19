package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.TipoInfracao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de uma penalidade já aplicada: trilha de auditoria e base para contar reincidências.
 * Só nasce por {@link #aplicar}, que também desconta os pontos do morador.
 */
@Entity
@Table(name = "penalidades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Penalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "morador_id", nullable = false)
    private Morador morador;

    /** Regra que originou a penalidade; nulo nas penalidades manuais e nas sem regra configurada. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "regra_id")
    private RegraPenalidade regra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInfracao tipoInfracao;

    @Column(nullable = false)
    private int pontos;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime dataAplicacao;

    /** Síndico que aplicou; nulo quando foi automática. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "aplicada_por_id")
    private Sindico aplicadaPor;

    public static Penalidade aplicar(Morador morador, RegraPenalidade regra, TipoInfracao tipoInfracao, int pontos,
                                     String motivo, Sindico aplicadaPor) {
        if (morador == null) {
            throw new RegraNegocioException("O morador penalizado é obrigatório.");
        }
        if (tipoInfracao == null) {
            throw new RegraNegocioException("O tipo de infração é obrigatório.");
        }
        if (pontos < 1) {
            throw new RegraNegocioException("A penalidade deve descontar ao menos 1 ponto.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("O motivo da penalidade é obrigatório.");
        }

        Penalidade p = new Penalidade();
        p.morador = morador;
        p.regra = regra;
        p.tipoInfracao = tipoInfracao;
        p.pontos = pontos;
        p.motivo = motivo;
        p.aplicadaPor = aplicadaPor;
        p.dataAplicacao = LocalDateTime.now();
        morador.deduzirPontos(pontos);
        return p;
    }
}
