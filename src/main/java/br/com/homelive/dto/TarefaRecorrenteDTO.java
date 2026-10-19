package br.com.homelive.dto;

import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoRodizio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public class TarefaRecorrenteDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O título é obrigatório")
        private String titulo;

        private String descricao;

        @NotNull(message = "A periodicidade é obrigatória")
        private Periodicidade periodicidade;

        @NotNull(message = "O prazo em dias é obrigatório")
        @Positive(message = "O prazo deve ser de ao menos 1 dia")
        private Integer prazoEmDias;

        @PositiveOrZero(message = "Os pontos de recompensa devem ser zero ou positivos")
        private Integer pontosRecompensa;

        @PositiveOrZero(message = "Os pontos de penalidade devem ser zero ou positivos")
        private Integer pontosPenalidade;

        @NotNull(message = "O tipo de rodízio é obrigatório")
        private TipoRodizio tipoRodizio;

        /** Opcional: se omitida, a primeira tarefa é gerada hoje. */
        private LocalDate primeiraGeracao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String titulo;
        private String descricao;
        private Periodicidade periodicidade;
        private int prazoEmDias;
        private Integer pontosRecompensa;
        private Integer pontosPenalidade;
        private TipoRodizio tipoRodizio;
        private LocalDate proximaGeracao;
        private boolean ativa;
        private String ultimoResponsavelNome;
    }
}
