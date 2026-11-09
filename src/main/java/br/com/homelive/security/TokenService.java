package br.com.homelive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cria e valida o token de login (JWT assinado com HMAC-SHA256).
 * <p>
 * Pense no token como um "crachá" temporário: o login (e-mail + senha) acontece uma vez, a API entrega o crachá,
 * e o front só mostra o crachá nas próximas requisições. A senha nunca fica guardada no navegador.
 * O token leva o e-mail (subject) e a data de validade; se alguém alterar qualquer byte, a assinatura não confere.
 */
@Service
public class TokenService {

    /** Token pronto para devolver ao front, com o instante em que deixa de valer. */
    public record TokenEmitido(String valor, Instant expiraEm) {
    }

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration validadeLembrar;
    private final Duration validadePadrao;

    public TokenService(@Value("${homelive.jwt.secret}") String segredo,
                        @Value("${homelive.jwt.validade-lembrar-dias}") long diasLembrar,
                        @Value("${homelive.jwt.validade-padrao-horas}") long horasPadrao) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("homelive.jwt.secret precisa ter ao menos 32 caracteres (exigência do HS256).");
        }
        SecretKey chave = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        this.validadeLembrar = Duration.ofDays(diasLembrar);
        this.validadePadrao = Duration.ofHours(horasPadrao);
    }

    /** Token para o usuário. {@code lembrar} escolhe a validade longa (continuar conectado). */
    public TokenEmitido gerar(String email, String papel, boolean lembrar) {
        return gerar(email, papel, Instant.now(), lembrar ? validadeLembrar : validadePadrao);
    }

    /** Visível ao pacote para os testes poderem fabricar um token já expirado. */
    TokenEmitido gerar(String email, String papel, Instant agora, Duration validade) {
        Instant expira = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("papel", papel)
                .issuedAt(agora)
                .expiresAt(expira)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(valor, expira);
    }

    /** E-mail dono do token, ou vazio se o token for inválido, adulterado ou estiver expirado. */
    public Optional<String> emailDoToken(String token) {
        try {
            return Optional.ofNullable(decoder.decode(token).getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
