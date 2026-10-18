package br.com.homelive.controller;

import br.com.homelive.dto.SindicoDTO;
import br.com.homelive.service.SindicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sindicos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SindicoController {

    private final SindicoService sindicoService;

    @PostMapping
    public ResponseEntity<SindicoDTO.Response> cadastrar(@RequestBody @Valid SindicoDTO.Request request) {
        SindicoDTO.Response response = sindicoService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SindicoDTO.Response>> listarTodos() {
        return ResponseEntity.ok(sindicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SindicoDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sindicoService.obterPorId(id));
    }
}
