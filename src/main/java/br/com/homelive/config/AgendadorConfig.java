package br.com.homelive.config;

import br.com.homelive.model.Casa;
import br.com.homelive.repository.CasaRepository;
import br.com.homelive.service.TarefaRecorrenteService;
import br.com.homelive.service.TarefaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Rotinas diárias: é isso que torna a agenda de tarefas automática, sem ninguém precisar chamar a API. */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AgendadorConfig {

    private final TarefaRecorrenteService recorrenteService;
    private final TarefaService tarefaService;
    private final CasaRepository casaRepository;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    public void rotinaDiaria() {
        for (Casa casa : casaRepository.findAll()) {
            rotinaDaCasa(casa.getId());
        }
    }

    /**
     * Cada casa roda por conta própria e em transações separadas: um problema numa casa não impede as outras.
     * Penaliza o que venceu ontem antes de distribuir as tarefas novas (que entram no cálculo de carga).
     */
    void rotinaDaCasa(Long casaId) {
        try {
            tarefaService.verificarEProcessarAtrasos(casaId);
            recorrenteService.gerarTarefasDevidas(casaId);
        } catch (RuntimeException e) {
            log.error("Falha na rotina diária da casa {}", casaId, e);
        }
    }
}
