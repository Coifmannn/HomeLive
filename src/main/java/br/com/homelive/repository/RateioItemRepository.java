package br.com.homelive.repository;

import br.com.homelive.model.RateioItem;
import br.com.homelive.model.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateioItemRepository extends JpaRepository<RateioItem, Long> {
    List<RateioItem> findByMoradorId(Long moradorId);
    List<RateioItem> findByDespesaId(Long despesaId);
    List<RateioItem> findByMoradorIdAndStatusPagamento(Long moradorId, StatusPagamento status);
}
