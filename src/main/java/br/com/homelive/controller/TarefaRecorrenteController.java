package br.com.homelive.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.homelive.security.UsuarioAutenticado;
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
    public ResponseEntity<TarefaRecorrenteDTO.Response> criar(@RequestBody @Valid TarefaRecorrenteDTO.Request request, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recorrenteService.criar(logado.getCasaId(), request));
    }

    @GetMapping
    public ResponseEntity<List<TarefaRecorrenteDTO.Response>> listarTodas(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(recorrenteService.listarTodas(logado.getCasaId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaRecorrenteDTO.Response> obterPorId(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(recorrenteService.obterPorId(logado.getCasaId(), id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<TarefaRecorrenteDTO.Response> ativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(recorrenteService.ativar(logado.getCasaId(), id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<TarefaRecorrenteDTO.Response> desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(recorrenteService.desativar(logado.getCasaId(), id));
    }

    /** Dispara a geração na hora (o agendador já faz isso todo dia). Útil para demonstração e testes. */
    @PostMapping("/gerar")
    public ResponseEntity<Map<String, Object>> gerarAgora(@AuthenticationPrincipal UsuarioAutenticado logado) {
        List<TarefaDTO.Response> geradas = recorrenteService.gerarTarefasDevidas(logado.getCasaId());
        return ResponseEntity.ok(Map.of("totalGeradas", geradas.size(), "tarefas", geradas));
    }
}
