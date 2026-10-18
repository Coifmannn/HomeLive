package br.com.coliving.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class EspacoComumDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "O nome do espaço é obrigatório")
        private String nome;

        private String descricao;

        @Positive(message = "A capacidade deve ser maior que zero")
        private Integer capacidadeMaxima;

        private String regrasUso;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nome;
        private String descricao;
        private Integer capacidadeMaxima;
        private String regrasUso;
        private boolean ativo;
    }
}
