package br.com.homelive.controller;

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
    public ResponseEntity<EspacoComumDTO.Response> cadastrar(@RequestBody @Valid EspacoComumDTO.Request request) {
        EspacoComumDTO.Response response = espacoComumService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EspacoComumDTO.Response>> listarTodos(@RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) {
            return ResponseEntity.ok(espacoComumService.listarAtivos());
        }
        return ResponseEntity.ok(espacoComumService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacoComumDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(espacoComumService.obterPorId(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alternarStatus(@PathVariable Long id) {
        espacoComumService.alternarStatus(id);
        return ResponseEntity.noContent().build();
    }
}
