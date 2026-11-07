package br.com.homelive.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.dto.EspacoComumDTO;
import br.com.homelive.service.EspacoComumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacos")
@RequiredArgsConstructor
public class EspacoComumController {

    private final EspacoComumService espacoComumService;

    @PostMapping
    public ResponseEntity<EspacoComumDTO.Response> cadastrar(@RequestBody @Valid EspacoComumDTO.Request request, @AuthenticationPrincipal UsuarioAutenticado logado) {
        EspacoComumDTO.Response response = espacoComumService.cadastrar(logado.getCasaId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EspacoComumDTO.Response>> listarTodos(@RequestParam(required = false, defaultValue = "false") boolean apenasAtivos, @AuthenticationPrincipal UsuarioAutenticado logado) {
        if (apenasAtivos) {
            return ResponseEntity.ok(espacoComumService.listarAtivos(logado.getCasaId()));
        }
        return ResponseEntity.ok(espacoComumService.listarTodos(logado.getCasaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacoComumDTO.Response> obterPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(espacoComumService.obterPorId(logado.getCasaId(), id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alternarStatus(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        espacoComumService.alternarStatus(logado.getCasaId(), id);
        return ResponseEntity.noContent().build();
    }
}
