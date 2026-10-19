package br.com.homelive.model;

import br.com.homelive.model.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @Email(message = "Email inválido")
    @NotBlank(message = "O email é obrigatório")
    @Column(nullable = false, unique = true)
    private String email;

    private String telefone;

    /** Hash BCrypt. A senha em texto puro nunca chega à entidade. */
    @NotBlank(message = "A senha é obrigatória")
    @Column(nullable = false)
    private String senhaHash;

    private boolean ativo = true;

    private LocalDateTime dataCriacao = LocalDateTime.now();

    protected Usuario(String nome, String email, String senhaHash, String telefone) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
    }

    /** Cada perfil declara o próprio papel (polimorfismo); a segurança deriva as permissões daqui. */
    public abstract TipoUsuario getTipoUsuario();

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }
}
