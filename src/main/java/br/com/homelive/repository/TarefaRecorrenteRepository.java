package br.com.homelive.repository;

import br.com.homelive.model.TarefaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarefaRecorrenteRepository extends JpaRepository<TarefaRecorrente, Long> {
    List<TarefaRecorrente> findByCasaId(Long casaId);
    Optional<TarefaRecorrente> findByIdAndCasaId(Long id, Long casaId);
    List<TarefaRecorrente> findByCasaIdAndAtivaTrueOrderByIdAsc(Long casaId);
}
