package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * A residência (co-living). Hoje existe uma só por instalação: quem a cria vira o síndico, e os moradores
 * entram com o {@code codigoConvite}. Por isso os usuários não apontam para a casa (não há outra para escolher).
 */
@Entity
@Table(name = "casas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Casa {

    /** Sem I, O, 0 e 1, que se confundem ao digitar ou ler o código em voz alta. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final int TAMANHO_MAXIMO_NOME = 80;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    /** Formato {@code HL-7K2QXM}. Único. */
    @Column(nullable = false, unique = true)
    private String codigoConvite;

    private LocalDateTime dataCriacao = LocalDateTime.now();

    public Casa(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da residência é obrigatório.");
        }
        if (nome.trim().length() > TAMANHO_MAXIMO_NOME) {
            throw new RegraNegocioException("O nome da residência deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        this.nome = nome.trim();
        this.codigoConvite = gerarCodigo();
        this.dataCriacao = LocalDateTime.now();
    }

    /** Gera outro código; o antigo deixa de valer (ex.: o código vazou e o síndico quer trocá-lo). */
    public void renovarCodigo() {
        String novo;
        do {
            novo = gerarCodigo();
        } while (novo.equals(this.codigoConvite));
        this.codigoConvite = novo;
    }

    /** O código informado confere? Ignora maiúsculas/minúsculas e espaços nas pontas. */
    public boolean aceitaCodigo(String informado) {
        return informado != null && codigoConvite.equalsIgnoreCase(informado.trim());
    }

    static String gerarCodigo() {
        StringBuilder codigo = new StringBuilder("HL-");
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
