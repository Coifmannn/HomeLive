package br.com.homelive.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.dto.MoradorDTO;
import br.com.homelive.service.MoradorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moradores")
@RequiredArgsConstructor
public class MoradorController {

    private final MoradorService moradorService;

    @GetMapping
    public ResponseEntity<List<MoradorDTO.Response>> listarTodos(@RequestParam(required = false, defaultValue = "false") boolean apenasAtivos, @AuthenticationPrincipal UsuarioAutenticado logado) {
        if (apenasAtivos) {
            return ResponseEntity.ok(moradorService.listarAtivos(logado.getCasaId()));
        }
        return ResponseEntity.ok(moradorService.listarTodos(logado.getCasaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MoradorDTO.Response> obterPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(moradorService.obterPorId(logado.getCasaId(), id));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<MoradorDTO.Response>> obterRankingGamificacao(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(moradorService.obterRankingGamificacao(logado.getCasaId()));
    }

    @PatchMapping("/{id}/estadia")
    public ResponseEntity<MoradorDTO.Response> atualizarEstadia(@PathVariable Long id, @RequestParam Integer dias, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(moradorService.atualizarEstadia(logado.getCasaId(), id, dias));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        moradorService.desativar(logado.getCasaId(), id);
        return ResponseEntity.noContent().build();
    }
}
