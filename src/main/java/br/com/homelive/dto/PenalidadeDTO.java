package br.com.homelive.dto;

import br.com.homelive.model.enums.TipoInfracao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class PenalidadeDTO {

    /** Penalidade aplicada diretamente pelo síndico, sem passar por regra. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualRequest {
        @NotNull(message = "O morador é obrigatório")
        private Long moradorId;

        @NotNull(message = "Os pontos são obrigatórios")
        @Positive(message = "Os pontos devem ser ao menos 1")
        private Integer pontos;

        @NotBlank(message = "O motivo é obrigatório")
        private String motivo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long moradorId;
        private String moradorNome;
        private Long regraId;
        private String regraNome;
        private TipoInfracao tipoInfracao;
        private int pontos;
        private String motivo;
        private LocalDateTime dataAplicacao;
        private String aplicadaPorNome;
    }
}
