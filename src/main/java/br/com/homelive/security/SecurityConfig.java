package br.com.homelive.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /** Arquivos do front (páginas, CSS, JS, ícones e manifesto do app): públicos, os dados não. */
    private static final String[] ARQUIVOS_DO_FRONT = {
            "/", "/index.html", "/login.html", "/cadastro.html", "/esqueceu-senha.html",
            "/manifest.json", "/sw.js", "/favicon.ico",
            "/css/**", "/js/**", "/assets/**", "/icons/**"
    };

    private final RespostasDeSegurancaJson respostasJson;
    private final TokenService tokenService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Matriz de permissões por perfil. Fica aqui (e não em anotações nos controllers) para ser aplicada
     * antes de o Spring ler/validar o corpo da requisição e para caber numa tela só.
     * <p>
     * Regras do tipo "só o próprio morador" dependem do dado acessado e ficam nos services.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API stateless (HTTP Basic ou token Bearer): sem sessão e sem cookie, então CSRF não se aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                // O console do H2 usa <frame>; precisa da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Entrada pública: login (troca e-mail + senha por token) e o front (HTML/CSS/JS/ícones do app).
                        // Os dados continuam todos protegidos: o front sozinho não mostra nada sem a API.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/cadastro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/casa/estado").permitAll()
                        .requestMatchers(HttpMethod.GET, ARQUIVOS_DO_FRONT).permitAll()
                        .requestMatchers(HttpMethod.HEAD, ARQUIVOS_DO_FRONT).permitAll()

                        // Console do banco dá acesso a tudo (inclusive hashes): só síndico.
                        .requestMatchers("/h2-console/**").hasRole("SINDICO")

                        // Só o síndico governa a residência.
                        .requestMatchers(HttpMethod.POST, "/api/moradores", "/api/sindicos", "/api/espacos").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/casa/convite/renovar", "/api/moradores/*/redefinir-senha").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/tarefas", "/api/tarefas/processar-atrasos").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/despesas/*/avaliar").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/espacos/*/status", "/api/moradores/*/estadia", "/api/moradores/*/reativar").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.DELETE, "/api/moradores/*").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/regras-penalidade", "/api/penalidades").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/regras-penalidade/*/ativar", "/api/regras-penalidade/*/desativar").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/nao-compareceu").hasRole("SINDICO")
                        // O histórico geral de penalidades é do síndico; o de cada morador ele e o dono consultam.
                        .requestMatchers(HttpMethod.GET, "/api/penalidades").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/tarefas-recorrentes", "/api/tarefas-recorrentes/gerar").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/tarefas-recorrentes/*/ativar", "/api/tarefas-recorrentes/*/desativar").hasRole("SINDICO")

                        // Todo o resto exige apenas estar logado (consultas e ações do dia a dia do morador).
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(respostasJson))
                // Token (Bearer) como segunda forma de entrar, antes do filtro do Basic.
                .addFilterBefore(new TokenAuthenticationFilter(tokenService, usuarioDetailsService),
                        BasicAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostasJson)
                        .accessDeniedHandler(respostasJson));
        return http.build();
    }
}
