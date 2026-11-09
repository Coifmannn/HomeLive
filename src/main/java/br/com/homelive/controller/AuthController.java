package br.com.homelive.controller;

import br.com.homelive.dto.AuthDTO;
import br.com.homelive.dto.CadastroDTO;
import br.com.homelive.service.AuthService;
import br.com.homelive.service.CadastroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CadastroService cadastroService;

    /**
     * Rota pública: cria a conta e já devolve o token (a pessoa sai logada). Sem residência ainda, quem se cadastra
     * cria a casa e vira síndico; com a residência criada, entra como morador usando o código de convite.
     */
    @PostMapping("/cadastro")
    public ResponseEntity<AuthDTO.LoginResponse> cadastrar(@Valid @RequestBody CadastroDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroService.cadastrar(request));
    }

    /** Rota pública: troca e-mail + senha por um token. */
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
