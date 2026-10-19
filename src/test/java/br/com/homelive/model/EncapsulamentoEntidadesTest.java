package br.com.homelive.model;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Guarda de encapsulamento: entidades não expõem setters públicos")
class EncapsulamentoEntidadesTest {

    private static final List<Class<?>> ENTIDADES = List.of(
            Usuario.class, Morador.class, Sindico.class, EspacoComum.class, AgendamentoEspaco.class,
            Despesa.class, RateioItem.class, TarefaDomestica.class, TarefaRecorrente.class, LeituraConsumo.class,
            RegraPenalidade.class, Penalidade.class);

    @Test
    @DisplayName("Nenhuma entidade tem método público setXxx(...)")
    void semSettersPublicos() {
        List<String> violacoes = ENTIDADES.stream()
                .peek(c -> assertTrue(c.isAnnotationPresent(Entity.class), c.getSimpleName() + " deveria ser @Entity"))
                .flatMap(c -> java.util.Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1)
                .map(Method::toGenericString)
                .toList();

        assertTrue(violacoes.isEmpty(),
                "Estado só deve mudar por métodos de negócio (aprovar, cancelar, concluir...). Setters encontrados: " + violacoes);
    }
}
