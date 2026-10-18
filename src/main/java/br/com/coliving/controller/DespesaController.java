package br.com.coliving.controller;

import br.com.coliving.dto.DespesaDTO;
import br.com.coliving.model.enums.StatusDespesa;
import br.com.coliving.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DespesaController {

    private final DespesaService despesaService;

    @PostMapping
    public ResponseEntity<DespesaDTO.Response> lancar(@RequestBody @Valid DespesaDTO.Request request) {
        DespesaDTO.Response response = despesaService.lancarDespesa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/avaliar")
    public ResponseEntity<DespesaDTO.Response> avaliar(
            @PathVariable Long id,
            @RequestBody @Valid DespesaDTO.AprovacaoRequest request) {
        DespesaDTO.Response response = despesaService.avaliarDespesa(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/rateio/{itemId}/quitar")
    public ResponseEntity<DespesaDTO.RateioItemResponse> quitarItemRateio(@PathVariable Long itemId) {
        return ResponseEntity.ok(despesaService.quitarItemRateio(itemId));
    }

    @GetMapping
    public ResponseEntity<List<DespesaDTO.Response>> listarTodas(
            @RequestParam(required = false) StatusDespesa status) {
        if (status != null) {
            return ResponseEntity.ok(despesaService.listarPorStatus(status));
        }
        return ResponseEntity.ok(despesaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespesaDTO.Response> obterPorId(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.obterPorId(id));
    }

    @GetMapping("/morador/{moradorId}/rateios")
    public ResponseEntity<List<DespesaDTO.RateioItemResponse>> listarRateiosPorMorador(@PathVariable Long moradorId) {
        return ResponseEntity.ok(despesaService.listarRateiosPorMorador(moradorId));
    }
}
