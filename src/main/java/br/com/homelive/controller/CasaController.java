package br.com.homelive.controller;

import br.com.homelive.dto.CasaDTO;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.CasaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/casa")
@RequiredArgsConstructor
public class CasaController {

    private final CasaService casaService;

    /** Pública: a tela de cadastro pergunta se a residência já existe. */
    @GetMapping("/estado")
    public ResponseEntity<CasaDTO.Estado> estado() {
        return ResponseEntity.ok(casaService.estado());
    }

    /** Dados da residência. O código de convite só aparece para o síndico. */
    @GetMapping
    public ResponseEntity<CasaDTO.Response> obter(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(casaService.obter(usuario));
    }

    /** Síndico: gera um novo código de convite (o anterior deixa de valer). */
    @PostMapping("/convite/renovar")
    public ResponseEntity<CasaDTO.Response> renovarConvite() {
        return ResponseEntity.ok(casaService.renovarCodigo());
    }
}
