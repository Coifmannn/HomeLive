package br.com.homelive.controller;

import br.com.homelive.dto.PenalidadeDTO;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.PenalidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalidades")
@RequiredArgsConstructor
public class PenalidadeController {

    private final PenalidadeService penalidadeService;

    /** Penalidade manual do síndico logado. */
    @PostMapping
    public ResponseEntity<PenalidadeDTO.Response> aplicarManual(@RequestBody @Valid PenalidadeDTO.ManualRequest request,
                                                                @AuthenticationPrincipal UsuarioAutenticado sindico) {
        return ResponseEntity.status(HttpStatus.CREATED).body(penalidadeService.aplicarManual(sindico.getCasaId(), request, sindico.getId()));
    }

    @GetMapping
    public ResponseEntity<List<PenalidadeDTO.Response>> listarTodas(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(penalidadeService.listarTodas(logado.getCasaId()));
    }

    @GetMapping("/morador/{moradorId}")
    public ResponseEntity<List<PenalidadeDTO.Response>> listarPorMorador(@PathVariable Long moradorId,
                                                                         @AuthenticationPrincipal UsuarioAutenticado ator) {
        return ResponseEntity.ok(penalidadeService.listarPorMorador(moradorId, ator));
    }
}
