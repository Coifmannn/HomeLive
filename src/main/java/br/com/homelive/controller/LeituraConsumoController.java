package br.com.homelive.controller;

import br.com.homelive.dto.LeituraConsumoDTO;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.service.LeituraConsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/leituras-consumo")
@RequiredArgsConstructor
public class LeituraConsumoController {

    private final LeituraConsumoService leituraService;

    /** Registra (ou corrige) a leitura do mês. */
    @PostMapping
    public ResponseEntity<LeituraConsumoDTO.Response> registrar(@RequestBody @Valid LeituraConsumoDTO.Request request,
                                                                @AuthenticationPrincipal UsuarioAutenticado ator) {
        return ResponseEntity.ok(leituraService.registrar(request, ator));
    }

    /** Leituras de um mês (aaaa-MM); sem parâmetro, o mês corrente. */
    @GetMapping
    public ResponseEntity<List<LeituraConsumoDTO.Response>> listarPorCompetencia(
            @RequestParam(required = false) String competencia) {
        YearMonth mes = competencia != null ? YearMonth.parse(competencia) : YearMonth.now();
        return ResponseEntity.ok(leituraService.listarPorCompetencia(mes));
    }

    @GetMapping("/morador/{moradorId}")
    public ResponseEntity<List<LeituraConsumoDTO.Response>> listarPorMorador(@PathVariable Long moradorId) {
        return ResponseEntity.ok(leituraService.listarPorMorador(moradorId));
    }
}
