package br.com.coliving.repository;

import br.com.coliving.model.EspacoComum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacoComumRepository extends JpaRepository<EspacoComum, Long> {
    List<EspacoComum> findByAtivoTrue();
}
