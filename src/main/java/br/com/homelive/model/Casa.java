package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma HomeLive: a casa compartilhada. Quem a funda vira o síndico; quem entra com o
 * {@code codigoConvite} vira morador (depois de aprovado).
 */
@Entity
@Table(name = "casas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Casa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Código curto que o síndico passa para quem vai morar na casa (ex.: HL-7K2Q). */
    @Column(nullable = false, unique = true)
    private String codigoConvite;

    private LocalDateTime dataCriacao = LocalDateTime.now();

    public Casa(String nome, String codigoConvite) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da casa é obrigatório.");
        }
        if (codigoConvite == null || codigoConvite.isBlank()) {
            throw new RegraNegocioException("A casa precisa de um código de convite.");
        }
        this.nome = nome.trim();
        this.codigoConvite = codigoConvite;
        this.dataCriacao = LocalDateTime.now();
    }

    /** Todo registro de uma casa precisa apontar para ela; sem isso o isolamento entre casas se perde. */
    public static Casa exigir(Casa casa) {
        if (casa == null) {
            throw new RegraNegocioException("A casa é obrigatória.");
        }
        return casa;
    }
}
