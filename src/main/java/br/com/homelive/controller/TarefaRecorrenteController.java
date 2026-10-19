package br.com.homelive.controller;

import br.com.homelive.dto.TarefaDTO;
import br.com.homelive.dto.TarefaRecorrenteDTO;
import br.com.homelive.service.TarefaRecorrenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarefas-recorrentes")
@RequiredArgsConstructor
public class TarefaRecorrenteController {

    private final TarefaRecorrenteService recorrenteService;

    @PostMapping
    public ResponseEntity<TarefaRecorrenteDTO.Response> criar(@RequestBody @Valid TarefaRecorrenteDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recorrenteService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<TarefaRecorrenteDTO.Response>> listarTodas() {
        return ResponseEntity.ok(recorrenteService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaRecorrenteDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recorrenteService.obterPorId(id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<TarefaRecorrenteDTO.Response> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(recorrenteService.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<TarefaRecorrenteDTO.Response> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(recorrenteService.desativar(id));
    }

    /** Dispara a geração na hora (o agendador já faz isso todo dia). Útil para demonstração e testes. */
    @PostMapping("/gerar")
    public ResponseEntity<Map<String, Object>> gerarAgora() {
        List<TarefaDTO.Response> geradas = recorrenteService.gerarTarefasDevidas();
        return ResponseEntity.ok(Map.of("totalGeradas", geradas.size(), "tarefas", geradas));
    }
}
