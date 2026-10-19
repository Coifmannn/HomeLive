package br.com.homelive.repository;

import br.com.homelive.model.LeituraConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeituraConsumoRepository extends JpaRepository<LeituraConsumo, Long> {

    Optional<LeituraConsumo> findByMoradorIdAndCompetencia(Long moradorId, LocalDate competencia);

    List<LeituraConsumo> findByCompetencia(LocalDate competencia);

    List<LeituraConsumo> findByMoradorIdOrderByCompetenciaDesc(Long moradorId);
}
