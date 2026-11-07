package br.com.homelive.dto;

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
    public static class Response {
        private Long id;
        private String nome;
        private String email;
        private String telefone;
        private String numeroQuarto;
        private Integer diasEstadia;
        private Integer pontosConvivencia;
        private LocalDate dataEntrada;
        private boolean ativo;
    }
}
