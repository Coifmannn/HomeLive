package br.com.homelive.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Respostas 401 (sem login) e 403 (sem permissão) no mesmo formato JSON do GlobalExceptionHandler,
 * já que essas falhas acontecem nos filtros, antes de chegar a qualquer controller.
 */
@Component
public class RespostasDeSegurancaJson implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"HomeLive\"");
        escrever(response, HttpStatus.UNAUTHORIZED, "Não Autenticado",
                "Informe e-mail e senha válidos (HTTP Basic) para acessar este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, "Acesso Negado",
                "Seu perfil não tem permissão para realizar esta ação.");
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String erro, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Textos fixos, sem entrada do usuário: não há o que escapar.
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + erro + "\",\"message\":\"" + mensagem + "\"}");
    }
}
