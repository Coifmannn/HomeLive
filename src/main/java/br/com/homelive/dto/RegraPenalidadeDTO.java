package br.com.homelive.dto;

import br.com.homelive.model.enums.ModoCalculoPenalidade;
import br.com.homelive.model.enums.TipoInfracao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class RegraPenalidadeDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O nome da regra é obrigatório")
        private String nome;

        @NotNull(message = "O tipo de infração é obrigatório")
        private TipoInfracao tipoInfracao;

        @NotNull(message = "O modo de cálculo é obrigatório")
        private ModoCalculoPenalidade modoCalculo;

        @NotNull(message = "Os pontos base são obrigatórios")
        @Positive(message = "Os pontos base devem ser ao menos 1")
        private Integer pontosBase;

        /** Só para o modo PROGRESSIVA. */
        @PositiveOrZero(message = "O incremento deve ser zero ou positivo")
        private Integer incrementoPorReincidencia;

        /** Opcional: máximo de pontos de uma única penalidade. */
        @Positive(message = "O teto deve ser positivo")
        private Integer tetoPontos;

        /** Opcional (padrão 30): por quantos dias uma penalidade anterior conta como reincidência. */
        @Positive(message = "A janela de reincidência deve ser de ao menos 1 dia")
        private Integer janelaReincidenciaDias;

        /** Obrigatório (e só aceito) para CANCELAMENTO_TARDIO. */
        @Positive(message = "O limite de horas deve ser positivo")
        private Integer limiteHorasAntecedencia;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nome;
        private TipoInfracao tipoInfracao;
        private ModoCalculoPenalidade modoCalculo;
        private int pontosBase;
        private int incrementoPorReincidencia;
        private Integer tetoPontos;
        private int janelaReincidenciaDias;
        private Integer limiteHorasAntecedencia;
        private boolean ativa;
    }
}
