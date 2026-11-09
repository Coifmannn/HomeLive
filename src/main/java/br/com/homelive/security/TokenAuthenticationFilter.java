package br.com.homelive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Segunda forma de entrar na API, ao lado do HTTP Basic: {@code Authorization: Bearer <token>}.
 * <p>
 * Se o token for válido e o usuário continuar ativo, a requisição passa a ser dele. Se não for, o filtro
 * simplesmente não autentica ninguém, e o Spring Security responde 401 como em qualquer outra requisição anônima.
 * Não é um {@code @Component}: é criado no {@link SecurityConfig} para rodar só dentro da cadeia de segurança.
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final TokenService tokenService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.emailDoToken(cabecalho.substring(PREFIXO.length()).trim()).ifPresent(this::autenticar);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String email) {
        try {
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);
            // Quem foi desativado depois de receber o token perde o acesso na hora.
            if (usuario.isEnabled()) {
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
            }
        } catch (UsernameNotFoundException ignorado) {
            // token de um usuário que não existe mais: segue anônimo
        }
    }
}
