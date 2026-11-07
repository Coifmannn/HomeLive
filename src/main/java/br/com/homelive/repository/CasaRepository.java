package br.com.homelive.repository;

import br.com.homelive.model.Casa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CasaRepository extends JpaRepository<Casa, Long> {
    Optional<Casa> findByCodigoConvite(String codigoConvite);
    boolean existsByCodigoConvite(String codigoConvite);
}
