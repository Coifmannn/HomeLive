package br.com.coliving.controller;

import br.com.coliving.dto.AgendamentoDTO;
import br.com.coliving.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoDTO.Response> criar(@RequestBody @Valid AgendamentoDTO.Request request) {
        AgendamentoDTO.Response response = agendamentoService.criarAgendamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoDTO.Response>> listarTodos() {
        return ResponseEntity.ok(agendamentoService.listarTodos());
    }

    @GetMapping("/espaco/{espacoId}")
    public ResponseEntity<List<AgendamentoDTO.Response>> listarPorEspaco(@PathVariable Long espacoId) {
        return ResponseEntity.ok(agendamentoService.listarPorEspaco(espacoId));
    }

    @GetMapping("/morador/{moradorId}")
    public ResponseEntity<List<AgendamentoDTO.Response>> listarPorMorador(@PathVariable Long moradorId) {
        return ResponseEntity.ok(agendamentoService.listarPorMorador(moradorId));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelarAgendamento(id);
        return ResponseEntity.noContent().build();
    }
}
