package br.com.homelive.controller;

import br.com.homelive.dto.CasaDTO;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.CasaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casas")
@RequiredArgsConstructor
public class CasaController {

    private final CasaService casaService;

    /** Funda uma casa; quem chama vira o síndico dela. */
    @PostMapping
    public ResponseEntity<CasaDTO.Response> criar(@AuthenticationPrincipal UsuarioAutenticado logado,
                                                  @RequestBody @Valid CasaDTO.CriarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(casaService.criar(logado, request));
    }

    /** Pede para entrar numa casa com o código de convite; fica pendente até o síndico aprovar. */
    @PostMapping("/entrar")
    public ResponseEntity<CasaDTO.Response> entrar(@AuthenticationPrincipal UsuarioAutenticado logado,
                                                   @RequestBody @Valid CasaDTO.EntrarRequest request) {
        return ResponseEntity.ok(casaService.entrar(logado, request));
    }

    @GetMapping("/minha")
    public ResponseEntity<CasaDTO.Response> minha(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(casaService.minhaCasa(logado));
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<CasaDTO.MembroResponse>> pendentes(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(casaService.listarPendentes(logado));
    }

    @PostMapping("/membros/{moradorId}/aprovar")
    public ResponseEntity<CasaDTO.MembroResponse> aprovar(@AuthenticationPrincipal UsuarioAutenticado logado,
                                                          @PathVariable Long moradorId) {
        return ResponseEntity.ok(casaService.aprovar(logado, moradorId));
    }

    @PostMapping("/membros/{moradorId}/recusar")
    public ResponseEntity<CasaDTO.MembroResponse> recusar(@AuthenticationPrincipal UsuarioAutenticado logado,
                                                          @PathVariable Long moradorId) {
        return ResponseEntity.ok(casaService.recusar(logado, moradorId));
    }
}
