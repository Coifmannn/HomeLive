package br.com.homelive.dto;

import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.model.enums.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthDTO {

    /** Cadastro público: só cria a conta. Casa e quarto vêm depois, ao criar/entrar numa casa. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CadastroRequest {
        @NotBlank(message = "O nome é obrigatório")
        private String nome;

        @Email(message = "Email inválido")
        @NotBlank(message = "O email é obrigatório")
        private String email;

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres")
        private String senha;

        private String telefone;
    }

    /** Quem sou eu e em que pé estou: o front usa isto para decidir qual tela mostrar. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Perfil {
        private Long id;
        private String nome;
        private String email;
        private TipoUsuario tipo;
        private StatusMembro statusMembro;
        private Long casaId;
        private String casaNome;
    }
}
