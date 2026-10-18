package br.com.coliving.repository;

import br.com.coliving.model.Sindico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SindicoRepository extends JpaRepository<Sindico, Long> {
    List<Sindico> findByAtivoTrue();
}
