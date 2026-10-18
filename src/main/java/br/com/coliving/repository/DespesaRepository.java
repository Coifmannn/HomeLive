package br.com.coliving.repository;

import br.com.coliving.model.Despesa;
import br.com.coliving.model.enums.StatusDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByStatus(StatusDespesa status);
    List<Despesa> findBySolicitanteId(Long solicitanteId);
}
