package br.com.homelive.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.dto.RegraPenalidadeDTO;
import br.com.homelive.service.RegraPenalidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regras-penalidade")
@RequiredArgsConstructor
public class RegraPenalidadeController {

    private final RegraPenalidadeService regraService;

    @PostMapping
    public ResponseEntity<RegraPenalidadeDTO.Response> criar(@RequestBody @Valid RegraPenalidadeDTO.Request request, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regraService.criar(logado.getCasaId(), request));
    }

    @GetMapping
    public ResponseEntity<List<RegraPenalidadeDTO.Response>> listarTodas(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(regraService.listarTodas(logado.getCasaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegraPenalidadeDTO.Response> obterPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(regraService.obterPorId(logado.getCasaId(), id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<RegraPenalidadeDTO.Response> ativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(regraService.ativar(logado.getCasaId(), id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<RegraPenalidadeDTO.Response> desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(regraService.desativar(logado.getCasaId(), id));
    }
}
