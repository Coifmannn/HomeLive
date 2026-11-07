package br.com.homelive.controller;

import br.com.homelive.dto.AuthDTO;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Pública: qualquer pessoa cria a própria conta. */
    @PostMapping("/cadastro")
    public ResponseEntity<AuthDTO.Perfil> cadastrar(@RequestBody @Valid AuthDTO.CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrar(request));
    }

    /** Quem está logado, em que papel e em que casa. */
    @GetMapping("/eu")
    public ResponseEntity<AuthDTO.Perfil> eu(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(authService.perfilDe(logado.getId()));
    }
}
