package br.com.homelive.repository;

import br.com.homelive.model.EspacoComum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspacoComumRepository extends JpaRepository<EspacoComum, Long> {
    List<EspacoComum> findByCasaId(Long casaId);
    List<EspacoComum> findByCasaIdAndAtivoTrue(Long casaId);
    Optional<EspacoComum> findByIdAndCasaId(Long id, Long casaId);
}
