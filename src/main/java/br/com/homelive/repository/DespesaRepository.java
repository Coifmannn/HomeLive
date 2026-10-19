package br.com.homelive.repository;

import br.com.homelive.model.Despesa;
import br.com.homelive.model.enums.StatusDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByStatus(StatusDespesa status);
    List<Despesa> findBySolicitanteId(Long solicitanteId);

    /** Soma o valor das despesas lançadas no intervalo [inicio, fim). */
    @Query("SELECT COALESCE(SUM(d.valorTotal), 0) FROM Despesa d " +
           "WHERE d.dataLancamento >= :inicio AND d.dataLancamento < :fim")
    BigDecimal somarValorLancadoEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
