package br.com.homelive.repository;

import br.com.homelive.model.Casa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CasaRepository extends JpaRepository<Casa, Long> {
    /** Existe uma só casa por instalação; esta é "a" casa. */
    Optional<Casa> findFirstByOrderByIdAsc();
}
