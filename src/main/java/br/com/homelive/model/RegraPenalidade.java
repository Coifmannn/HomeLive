package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.TipoInfracao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Regra de penalidade configurada pelo síndico: para qual infração, quantos pontos descontar e como
 * calcular (fixo ou progressivo para reincidentes). Só pode haver uma regra ativa por tipo de infração.
 */
@Entity
@Table(name = "regras_penalidade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegraPenalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInfracao tipoInfracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModoCalculoPenalidade modoCalculo;

    @Column(nullable = false)
    private int pontosBase;

    /** Pontos somados a cada reincidência (só no modo PROGRESSIVA). */
    private int incrementoPorReincidencia;

    /** Máximo de pontos de uma única penalidade; nulo significa sem teto. */
    private Integer tetoPontos;

    /** Por quantos dias uma penalidade anterior ainda conta como reincidência. */
    @Column(nullable = false)
    private int janelaReincidenciaDias = 30;

    /** Só para CANCELAMENTO_TARDIO: cancelar com menos horas que isso de antecedência é infração. */
    private Integer limiteHorasAntecedencia;

    private boolean ativa = true;

    public RegraPenalidade(String nome, TipoInfracao tipoInfracao, ModoCalculoPenalidade modoCalculo, int pontosBase,
                           Integer incrementoPorReincidencia, Integer tetoPontos, Integer janelaReincidenciaDias,
                           Integer limiteHorasAntecedencia) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da regra é obrigatório.");
        }
        if (tipoInfracao == null || !tipoInfracao.isConfiguravel()) {
            throw new RegraNegocioException("Não é possível configurar regra para este tipo de infração.");
        }
        if (modoCalculo == null) {
            throw new RegraNegocioException("O modo de cálculo é obrigatório.");
        }
        if (pontosBase < 1) {
            throw new RegraNegocioException("Os pontos base devem ser ao menos 1.");
        }
        int incremento = incrementoPorReincidencia != null ? incrementoPorReincidencia : 0;
        if (modoCalculo == ModoCalculoPenalidade.PROGRESSIVA && incremento < 1) {
            throw new RegraNegocioException("Uma regra progressiva precisa de incremento por reincidência de ao menos 1.");
        }
        if (modoCalculo == ModoCalculoPenalidade.FIXA && incremento != 0) {
            throw new RegraNegocioException("Uma regra fixa não usa incremento por reincidência.");
        }
        if (tetoPontos != null && tetoPontos < pontosBase) {
            throw new RegraNegocioException("O teto de pontos não pode ser menor que os pontos base.");
        }
        int janela = janelaReincidenciaDias != null ? janelaReincidenciaDias : 30;
        if (janela < 1) {
            throw new RegraNegocioException("A janela de reincidência deve ser de ao menos 1 dia.");
        }
        if (tipoInfracao == TipoInfracao.CANCELAMENTO_TARDIO) {
            if (limiteHorasAntecedencia == null || limiteHorasAntecedencia < 1) {
                throw new RegraNegocioException("Informe o limite de horas de antecedência (mínimo 1) para cancelamento tardio.");
            }
        } else if (limiteHorasAntecedencia != null) {
            throw new RegraNegocioException("O limite de horas de antecedência só vale para cancelamento tardio.");
        }

        this.nome = nome;
        this.tipoInfracao = tipoInfracao;
        this.modoCalculo = modoCalculo;
        this.pontosBase = pontosBase;
        this.incrementoPorReincidencia = incremento;
        this.tetoPontos = tetoPontos;
        this.janelaReincidenciaDias = janela;
        this.limiteHorasAntecedencia = limiteHorasAntecedencia;
        this.ativa = true;
    }

    public int pontosPara(long reincidencias) {
        return modoCalculo.calcular(pontosBase, incrementoPorReincidencia, tetoPontos, reincidencias);
    }

    /** Cancelar com menos de {@code limiteHorasAntecedencia} horas para o início (ou depois dele) é tardio. */
    public boolean cancelamentoEhTardio(LocalDateTime inicioReserva, LocalDateTime quando) {
        return limiteHorasAntecedencia != null && quando.isAfter(inicioReserva.minusHours(limiteHorasAntecedencia));
    }

    public void ativar() {
        this.ativa = true;
    }

    public void desativar() {
        this.ativa = false;
    }
}
