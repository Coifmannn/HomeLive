package br.com.homelive.dto;

import br.com.homelive.model.enums.StatusMembro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CasaDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriarRequest {
        @NotBlank(message = "O nome da casa é obrigatório")
        @Size(max = 80, message = "O nome da casa deve ter no máximo 80 caracteres")
        private String nome;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntrarRequest {
        @NotBlank(message = "O código de convite é obrigatório")
        private String codigoConvite;

        @NotBlank(message = "O número do quarto é obrigatório")
        private String numeroQuarto;
    }

    /** {@code codigoConvite} só vai preenchido para o síndico; para os demais fica nulo. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nome;
        private String codigoConvite;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembroResponse {
        private Long id;
        private String nome;
        private String email;
        private String numeroQuarto;
        private StatusMembro statusMembro;
    }
}
