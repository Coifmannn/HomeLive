package br.com.homelive.config;

import br.com.homelive.service.TarefaRecorrenteService;
import br.com.homelive.service.TarefaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Rotinas diárias: é isso que torna a agenda de tarefas automática, sem ninguém precisar chamar a API. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AgendadorConfig {

    private final TarefaRecorrenteService recorrenteService;
    private final TarefaService tarefaService;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    public void rotinaDiaria() {
        // Penaliza o que venceu ontem antes de distribuir as tarefas novas (que entram no cálculo de carga).
        tarefaService.verificarEProcessarAtrasos();
        recorrenteService.gerarTarefasDevidas();
    }
}
