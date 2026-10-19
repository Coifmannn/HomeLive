package br.com.homelive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LeituraConsumoDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** Só o síndico informa (leitura em nome de um morador). Para morador, vale sempre o usuário logado. */
        private Long moradorId;

        /** Mês de referência no formato aaaa-MM. Se omitido, vale o mês corrente. */
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "A competência deve estar no formato aaaa-MM")
        private String competencia;

        @NotNull(message = "O consumo em kWh é obrigatório")
        @PositiveOrZero(message = "O consumo deve ser zero ou positivo")
        private BigDecimal kwh;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long moradorId;
        private String moradorNome;
        private String competencia;
        private BigDecimal kwh;
        private LocalDateTime registradaEm;
    }
}
