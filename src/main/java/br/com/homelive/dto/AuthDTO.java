package br.com.homelive.dto;

import br.com.homelive.model.enums.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

public class AuthDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "O e-mail é obrigatório")
        private String email;

        @NotBlank(message = "A senha é obrigatória")
        private String senha;

        /** "Continuar conectado": token de longa duração. Opcional; ausente = não. */
        private Boolean lembrar;

        public boolean querLembrar() {
            return Boolean.TRUE.equals(lembrar);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class LoginResponse {
        private final String token;
        private final Instant expiraEm;
        private final TipoUsuario papel;
        /** Dados do usuário (morador ou síndico), no mesmo formato das listagens. */
        private final Object usuario;
    }
}
