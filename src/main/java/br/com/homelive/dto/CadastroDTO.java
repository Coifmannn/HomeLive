package br.com.homelive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class CadastroDTO {

    /**
     * Pedido de cadastro público. Os campos que valem dependem de a residência já existir:
     * <ul>
     *   <li>não existe casa: {@code nomeCasa} (e, opcionalmente, {@code usarSugestoes}); a pessoa vira síndico;</li>
     *   <li>já existe: {@code codigoConvite} e {@code numeroQuarto}; a pessoa vira morador.</li>
     * </ul>
     */
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

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres")
        private String senha;

        private String telefone;

        // Criar a residência (primeiro cadastro)
        private String nomeCasa;
        /** Cria espaços, regras e escalas de exemplo. Ausente = sim. */
        private Boolean usarSugestoes;

        // Entrar numa residência existente
        private String codigoConvite;
        private String numeroQuarto;

        /** "Continuar conectado" (token de 30 dias). Ausente = não. */
        private Boolean lembrar;

        public boolean querSugestoes() {
            return !Boolean.FALSE.equals(usarSugestoes);
        }

        public boolean querLembrar() {
            return Boolean.TRUE.equals(lembrar);
        }
    }
}
