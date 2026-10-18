package br.com.homelive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public class MoradorDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O nome é obrigatório")
        private String nome;

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        private String email;

        private String telefone;

        @NotBlank(message = "O número do quarto é obrigatório")
        private String numeroQuarto;

        @PositiveOrZero(message = "Dias de estadia deve ser zero ou positivo")
        private Integer diasEstadia;

        @PositiveOrZero(message = "Consumo declarado em kWh deve ser positivo")
        private Double consumoDeclaradoKwh;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nome;
        private String email;
        private String telefone;
        private String numeroQuarto;
        private Integer diasEstadia;
        private Double consumoDeclaradoKwh;
        private Integer pontosConvivencia;
        private LocalDate dataEntrada;
        private boolean ativo;
    }
}
