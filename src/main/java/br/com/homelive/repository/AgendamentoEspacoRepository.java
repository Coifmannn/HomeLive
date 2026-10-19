package br.com.homelive.repository;

import br.com.homelive.model.AgendamentoEspaco;
import br.com.homelive.model.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoEspacoRepository extends JpaRepository<AgendamentoEspaco, Long> {

    List<AgendamentoEspaco> findByEspacoComumId(Long espacoId);

    List<AgendamentoEspaco> findByMoradorId(Long moradorId);

    /** Conta reservas com o status dado que começam em [inicio, fim). */
    long countByStatusAndDataHoraInicioGreaterThanEqualAndDataHoraInicioLessThan(
            StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

    List<AgendamentoEspaco> findTop5ByStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
            StatusAgendamento status, LocalDateTime depoisDe);

    /**
     * Busca agendamentos confirmados que colidam temporalmente com o intervalo informado:
     * inicioNovo < fimExistente E fimNovo > inicioExistente
     */
    @Query("SELECT a FROM AgendamentoEspaco a WHERE a.espacoComum.id = :espacoId " +
           "AND a.status = br.com.homelive.model.enums.StatusAgendamento.CONFIRMADO " +
           "AND (:inicio < a.dataHoraFim AND :fim > a.dataHoraInicio)")
    List<AgendamentoEspaco> findConflitos(@Param("espacoId") Long espacoId,
                                         @Param("inicio") LocalDateTime inicio,
                                         @Param("fim") LocalDateTime fim);
}
