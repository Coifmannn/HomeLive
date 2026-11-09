package br.com.homelive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class SenhaDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlterarRequest {
        @NotBlank(message = "Informe a senha atual")
        private String senhaAtual;

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 6, message = "A nova senha deve ter ao menos 6 caracteres")
        private String novaSenha;
    }

    /** Senha temporária gerada para o morador. Aparece só nesta resposta: depois disso só existe o hash. */
    @Getter
    @AllArgsConstructor
    public static class RedefinicaoResponse {
        private final String senhaTemporaria;
    }
}
