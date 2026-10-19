package br.com.homelive.repository;

import br.com.homelive.model.TarefaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaRecorrenteRepository extends JpaRepository<TarefaRecorrente, Long> {

    List<TarefaRecorrente> findByAtivaTrueOrderByIdAsc();
}
