package br.com.homelive.model;

import br.com.homelive.model.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    private boolean ativo = true;

    private LocalDateTime dataCriacao = LocalDateTime.now();

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }
}
