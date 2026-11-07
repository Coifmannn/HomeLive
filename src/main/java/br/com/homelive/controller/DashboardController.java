package br.com.homelive.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.homelive.security.UsuarioAutenticado;
import br.com.homelive.dto.DashboardDTO;
import br.com.homelive.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardDTO.Response> obterResumo(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return ResponseEntity.ok(dashboardService.obterResumoGeral(logado.getCasaId()));
    }
}
