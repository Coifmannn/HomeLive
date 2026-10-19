package br.com.homelive.controller;

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

    @PostMapping
    public ResponseEntity<MoradorDTO.Response> cadastrar(@RequestBody @Valid MoradorDTO.Request request) {
        MoradorDTO.Response response = moradorService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MoradorDTO.Response>> listarTodos(@RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(moradorService.listarAtivos());
        }
        return ResponseEntity.ok(moradorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MoradorDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(moradorService.obterPorId(id));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<MoradorDTO.Response>> obterRankingGamificacao() {
        return ResponseEntity.ok(moradorService.obterRankingGamificacao());
    }

    @PatchMapping("/{id}/consumo")
    public ResponseEntity<MoradorDTO.Response> atualizarConsumo(@PathVariable Long id, @RequestParam Double consumoKwh) {
        return ResponseEntity.ok(moradorService.atualizarConsumo(id, consumoKwh));
    }

    @PatchMapping("/{id}/estadia")
    public ResponseEntity<MoradorDTO.Response> atualizarEstadia(@PathVariable Long id, @RequestParam Integer dias) {
        return ResponseEntity.ok(moradorService.atualizarEstadia(id, dias));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        moradorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
