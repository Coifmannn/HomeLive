package br.com.homelive.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Porta de entrada dos dados: todo o resto da API pertence a uma casa, então quem ainda não criou/entrou
 * em uma (SEM_CASA) ou espera aprovação (PENDENTE) só usa {@code /api/auth/**} e {@code /api/casas/**}.
 */
/** Sem @Component de propósito: o SecurityConfig o cria, para ele rodar só dentro da cadeia de segurança. */
@RequiredArgsConstructor
public class ExigeMembroAtivoFilter extends OncePerRequestFilter {

    private final RespostasDeSegurancaJson respostasJson;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !caminho.startsWith("/api/")
                || caminho.startsWith("/api/auth/")
                || caminho.startsWith("/api/casas/") || caminho.equals("/api/casas");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado logado && !logado.isMembroAtivo()) {
            String mensagem = logado.getCasaId() == null
                    ? "Crie uma casa ou entre em uma com o código de convite para acessar este recurso."
                    : "Sua entrada na casa ainda aguarda a aprovação do síndico.";
            respostasJson.semCasaAtiva(response, mensagem);
            return;
        }
        chain.doFilter(request, response);
    }
}
