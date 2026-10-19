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

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RespostasDeSegurancaJson respostasJson;

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
                // API stateless com HTTP Basic: sem sessão e sem cookie, então CSRF não se aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                // O console do H2 usa <frame>; precisa da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Console do banco dá acesso a tudo (inclusive hashes): só síndico.
                        .requestMatchers("/h2-console/**").hasRole("SINDICO")

                        // Só o síndico governa a residência.
                        .requestMatchers(HttpMethod.POST, "/api/moradores", "/api/sindicos", "/api/espacos").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/tarefas", "/api/tarefas/processar-atrasos").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.POST, "/api/despesas/*/avaliar").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/espacos/*/status", "/api/moradores/*/estadia").hasRole("SINDICO")
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
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostasJson)
                        .accessDeniedHandler(respostasJson));
        return http.build();
    }
}
