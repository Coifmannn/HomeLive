package br.com.homelive.controller;

import br.com.homelive.dto.MoradorDTO;
import br.com.homelive.dto.SenhaDTO;
import br.com.homelive.service.MoradorService;
import br.com.homelive.service.SenhaService;

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
    private final SenhaService senhaService;

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

    @PatchMapping("/{id}/estadia")
    public ResponseEntity<MoradorDTO.Response> atualizarEstadia(@PathVariable Long id, @RequestParam Integer dias) {
        return ResponseEntity.ok(moradorService.atualizarEstadia(id, dias));
    }

    /** Síndico: gera uma senha temporária para o morador que esqueceu a dele. */
    @PostMapping("/{id}/redefinir-senha")
    public ResponseEntity<SenhaDTO.RedefinicaoResponse> redefinirSenha(@PathVariable Long id) {
        return ResponseEntity.ok(senhaService.redefinirDoMorador(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        moradorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
