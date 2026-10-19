package br.com.homelive.dto;

import br.com.homelive.model.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DespesaDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O título da despesa é obrigatório")
        private String titulo;

        private String descricao;

        @NotNull(message = "O valor total é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        private BigDecimal valorTotal;

        @NotNull(message = "A data de vencimento é obrigatória")
        private LocalDate dataVencimento;

        @NotNull(message = "A categoria é obrigatória")
        private CategoriaDespesa categoria;

        @NotNull(message = "O tipo da despesa é obrigatório")
        private TipoDespesa tipoDespesa;

        private TipoRateio tipoRateio = TipoRateio.IGUALITARIO;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AprovacaoRequest {
        @NotNull(message = "A decisão de aprovação é obrigatória")
        private Boolean aprovado;

        private String motivoRejeicao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateioItemResponse {
        private Long id;
        private Long moradorId;
        private String moradorNome;
        private String moradorQuarto;
        private BigDecimal valorCalculado;
        private StatusPagamento statusPagamento;
        private LocalDate dataPagamento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String titulo;
        private String descricao;
        private BigDecimal valorTotal;
        private LocalDate dataVencimento;
        private LocalDate dataLancamento;
        private CategoriaDespesa categoria;
        private TipoDespesa tipoDespesa;
        private TipoRateio tipoRateio;
        private StatusDespesa status;
        private String solicitanteNome;
        private String aprovadorNome;
        private String motivoRejeicao;
        private List<RateioItemResponse> itensRateio;
    }
}
