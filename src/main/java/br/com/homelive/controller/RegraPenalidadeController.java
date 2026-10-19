package br.com.homelive.controller;

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
    public ResponseEntity<RegraPenalidadeDTO.Response> criar(@RequestBody @Valid RegraPenalidadeDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regraService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<RegraPenalidadeDTO.Response>> listarTodas() {
        return ResponseEntity.ok(regraService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegraPenalidadeDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(regraService.obterPorId(id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<RegraPenalidadeDTO.Response> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(regraService.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<RegraPenalidadeDTO.Response> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(regraService.desativar(id));
    }
}
