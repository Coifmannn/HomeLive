package br.com.homelive.repository;

import br.com.homelive.model.Morador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Long> {
    List<Morador> findByAtivoTrue();
    List<Morador> findAllByOrderByPontosConvivenciaDesc();
}
