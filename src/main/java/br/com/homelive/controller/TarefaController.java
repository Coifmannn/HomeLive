package br.com.homelive.controller;

import br.com.homelive.dto.TarefaDTO;
import br.com.homelive.model.enums.StatusTarefa;
import br.com.homelive.service.TarefaService;
import br.com.homelive.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    public ResponseEntity<TarefaDTO.Response> criar(@RequestBody @Valid TarefaDTO.Request request,
                                                @AuthenticationPrincipal UsuarioAutenticado sindico) {
        TarefaDTO.Response response = tarefaService.criarTarefa(request, sindico.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<TarefaDTO.Response> concluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado ator) {
        return ResponseEntity.ok(tarefaService.concluirTarefa(id, ator));
    }

    @PostMapping("/processar-atrasos")
    public ResponseEntity<Map<String, Object>> processarAtrasos() {
        int penalizadas = tarefaService.verificarEProcessarAtrasos();
        return ResponseEntity.ok(Map.of(
                "mensagem", "Processamento de atrasos concluído com sucesso",
                "tarefasPenalizadas", penalizadas
        ));
    }

    @GetMapping
    public ResponseEntity<List<TarefaDTO.Response>> listarTodas(
            @RequestParam(required = false) StatusTarefa status) {
        if (status != null) {
            return ResponseEntity.ok(tarefaService.listarPorStatus(status));
        }
        return ResponseEntity.ok(tarefaService.listarTodas());
    }

    @GetMapping("/morador/{moradorId}")
    public ResponseEntity<List<TarefaDTO.Response>> listarPorMorador(@PathVariable Long moradorId) {
        return ResponseEntity.ok(tarefaService.listarPorMorador(moradorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tarefaService.toResponse(tarefaService.buscarEntidade(id)));
    }
}
