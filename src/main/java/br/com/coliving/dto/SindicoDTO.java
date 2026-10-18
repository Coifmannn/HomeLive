package br.com.coliving.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public class SindicoDTO {

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

        private String departamento;
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
        private String departamento;
        private LocalDate inicioMandato;
        private LocalDate fimMandato;
        private boolean ativo;
    }
}
