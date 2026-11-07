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
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByCasaId(Long casaId);
    List<Despesa> findByCasaIdAndStatus(Long casaId, StatusDespesa status);
    Optional<Despesa> findByIdAndCasaId(Long id, Long casaId);

    /** Soma o valor das despesas da casa lançadas no intervalo [inicio, fim). */
    @Query("SELECT COALESCE(SUM(d.valorTotal), 0) FROM Despesa d " +
           "WHERE d.casa.id = :casaId AND d.dataLancamento >= :inicio AND d.dataLancamento < :fim")
    BigDecimal somarValorLancadoEntre(@Param("casaId") Long casaId,
                                      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
