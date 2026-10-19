package br.com.homelive.controller;

import br.com.homelive.dto.AgendamentoDTO;
import br.com.homelive.service.AgendamentoService;
import br.com.homelive.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoDTO.Response> criar(@RequestBody @Valid AgendamentoDTO.Request request,
                                                        @AuthenticationPrincipal UsuarioAutenticado ator) {
        AgendamentoDTO.Response response = agendamentoService.criarAgendamento(request, ator);
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
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado ator) {
        agendamentoService.cancelarAgendamento(id, ator);
        return ResponseEntity.noContent().build();
    }
}
