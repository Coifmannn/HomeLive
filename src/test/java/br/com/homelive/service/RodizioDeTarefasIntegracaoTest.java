package br.com.homelive.service;

import br.com.homelive.dto.TarefaDTO;
import br.com.homelive.model.Morador;
import br.com.homelive.model.TarefaRecorrente;
import br.com.homelive.model.enums.Periodicidade;
import br.com.homelive.model.enums.TipoRodizio;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.TarefaRecorrenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Rodízio de tarefas (fluxo completo com banco)")
class RodizioDeTarefasIntegracaoTest {

    @Autowired TarefaRecorrenteService service;
    @Autowired TarefaRecorrenteRepository recorrenteRepository;
    @Autowired MoradorRepository moradorRepository;

    @BeforeEach
    void isolar() {
        // Os modelos do DataInitializer também estão devidos hoje; cada teste cria só o que precisa.
        recorrenteRepository.findAll().forEach(TarefaRecorrente::desativar);
        recorrenteRepository.flush();
    }

    private TarefaRecorrente criar(TipoRodizio tipo) {
        return recorrenteRepository.saveAndFlush(new TarefaRecorrente("Teste " + tipo, "x",
                Periodicidade.SEMANAL, 2, 20, 10, tipo, LocalDate.now()));
    }

    /** Faz o modelo voltar a ficar devido hoje, como se a próxima semana tivesse chegado. */
    private void viraOProximoPeriodo(TarefaRecorrente modelo) {
        ReflectionTestUtils.setField(modelo, "proximaGeracao", LocalDate.now());
    }

    private String gerarUma() {
        List<TarefaDTO.Response> geradas = service.gerarTarefasDevidas();
        assertEquals(1, geradas.size());
        return geradas.get(0).getResponsavelNome();
    }

    @Test
    @DisplayName("Sequencial: depois da primeira vez, passa por todos em ordem circular e volta a quem começou")
    void sequencialGiraPorTodos() {
        TarefaRecorrente modelo = criar(TipoRodizio.SEQUENCIAL);
        List<String> ordemDeId = moradorRepository.findByAtivoTrue().stream()
                .sorted(Comparator.comparing(Morador::getId)).map(Morador::getNome).toList();

        List<String> sorteados = new java.util.ArrayList<>();
        for (int i = 0; i <= ordemDeId.size(); i++) {
            sorteados.add(gerarUma());
            viraOProximoPeriodo(modelo);
        }

        for (int i = 0; i < ordemDeId.size(); i++) {
            String atual = sorteados.get(i);
            String esperadoDepois = ordemDeId.get((ordemDeId.indexOf(atual) + 1) % ordemDeId.size());
            assertEquals(esperadoDepois, sorteados.get(i + 1), "depois de " + atual);
        }
        assertEquals(sorteados.get(0), sorteados.get(ordemDeId.size()), "completa o círculo");
        assertEquals(ordemDeId.size(), sorteados.stream().distinct().count(), "ninguém ficou de fora");
    }

    @Test
    @DisplayName("Sequencial: modelos criados juntos começam em pessoas diferentes e continuam sem colidir")
    void sequenciaisNaoGiramEmSincronia() {
        List<TarefaRecorrente> modelos = List.of(
                criar(TipoRodizio.SEQUENCIAL), criar(TipoRodizio.SEQUENCIAL), criar(TipoRodizio.SEQUENCIAL));

        for (int semana = 1; semana <= 4; semana++) {
            List<String> responsaveis = service.gerarTarefasDevidas().stream()
                    .map(TarefaDTO.Response::getResponsavelNome).toList();

            assertEquals(3, responsaveis.size());
            assertEquals(3, responsaveis.stream().distinct().count(),
                    "semana " + semana + ": três tarefas sequenciais não podem cair na mesma pessoa: " + responsaveis);
            modelos.forEach(this::viraOProximoPeriodo);
        }
    }

    @Test
    @DisplayName("É idempotente: chamar duas vezes no mesmo dia não duplica tarefas")
    void naoDuplica() {
        criar(TipoRodizio.SEQUENCIAL);

        assertEquals(1, service.gerarTarefasDevidas().size());
        assertTrue(service.gerarTarefasDevidas().isEmpty());
    }

    @Test
    @DisplayName("Quem foi desativado sai do rodízio")
    void ignoraMoradorInativo() {
        TarefaRecorrente modelo = criar(TipoRodizio.SEQUENCIAL);
        List<Morador> ativos = moradorRepository.findByAtivoTrue().stream()
                .sorted(Comparator.comparing(Morador::getId)).toList();
        Morador segundo = ativos.get(1);
        segundo.desativar();
        moradorRepository.flush();

        List<String> sorteados = new java.util.ArrayList<>();
        for (int i = 0; i < ativos.size(); i++) {
            sorteados.add(gerarUma());
            viraOProximoPeriodo(modelo);
        }

        assertFalse(sorteados.contains(segundo.getNome()), "morador inativo não pode receber tarefa");
        assertEquals(ativos.size() - 1, sorteados.stream().distinct().count(), "todos os outros participam");
    }

    @Test
    @DisplayName("Menor carga: dá a tarefa a quem tem menos em aberto, e a tarefa gerada passa a contar na rodada seguinte")
    void menorCargaEquilibra() {
        TarefaRecorrente modelo = criar(TipoRodizio.MENOR_CARGA);
        // Dados de demonstração: Lucas e Mateus têm uma tarefa em aberto; Sofia e Beatriz, nenhuma.

        String primeira = gerarUma();
        viraOProximoPeriodo(modelo);
        String segunda = gerarUma();

        assertEquals("Sofia Chen", primeira, "empate em zero tarefas: vence o menor id");
        assertEquals("Beatriz Santos", segunda, "Sofia já recebeu uma, então a próxima vai para quem ainda tem zero");
    }

    @Test
    @DisplayName("Menor carga: dois modelos devidos na mesma rodada não caem na mesma pessoa")
    void menorCargaNaMesmaRodada() {
        criar(TipoRodizio.MENOR_CARGA);
        criar(TipoRodizio.MENOR_CARGA);

        List<TarefaDTO.Response> geradas = service.gerarTarefasDevidas();

        assertEquals(2, geradas.size());
        assertNotEquals(geradas.get(0).getResponsavelNome(), geradas.get(1).getResponsavelNome(),
                "a primeira tarefa da rodada precisa pesar na escolha da segunda");
    }

    @Test
    @DisplayName("Menor pontuação: dá a tarefa a quem tem menos pontos de convivência")
    void menorPontuacao() {
        criar(TipoRodizio.MENOR_PONTUACAO);
        Morador esperado = moradorRepository.findByAtivoTrue().stream()
                .min(Comparator.comparingInt(Morador::getPontosConvivencia).thenComparing(Morador::getId))
                .orElseThrow();

        assertEquals(esperado.getNome(), gerarUma());
    }

    @Test
    @DisplayName("Modelo ainda não devido ou desativado não gera tarefa")
    void respeitaDataEAtivacao() {
        TarefaRecorrente futuro = recorrenteRepository.saveAndFlush(new TarefaRecorrente("Futuro", "x",
                Periodicidade.MENSAL, 3, null, null, TipoRodizio.SEQUENCIAL, LocalDate.now().plusDays(5)));
        assertTrue(service.gerarTarefasDevidas().isEmpty());

        TarefaRecorrente desativado = criar(TipoRodizio.SEQUENCIAL);
        service.desativar(desativado.getId());
        assertTrue(service.gerarTarefasDevidas().isEmpty());

        service.ativar(desativado.getId());
        assertEquals(1, service.gerarTarefasDevidas().size());
        assertNotNull(futuro.getId());
    }

    @Test
    @DisplayName("Sem moradores ativos, a rotina não falha: apenas não gera nada")
    void semMoradoresAtivos() {
        criar(TipoRodizio.SEQUENCIAL);
        moradorRepository.findByAtivoTrue().forEach(Morador::desativar);
        moradorRepository.flush();

        assertTrue(service.gerarTarefasDevidas().isEmpty());
    }
}
