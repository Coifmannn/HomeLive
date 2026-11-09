package br.com.homelive.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class CasaDTO {

    /** Resposta pública: o cadastro usa para decidir entre "criar residência" e "entrar com código". */
    @Getter
    @AllArgsConstructor
    public static class Estado {
        private final boolean casaCriada;
        private final String nomeCasa;
    }

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private final String nome;
        private final LocalDateTime dataCriacao;
        /** Só o síndico enxerga o código; para o morador o campo nem aparece. */
        private final String codigoConvite;
    }
}
