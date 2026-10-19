package br.com.homelive.repository;

import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.enums.TipoInfracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegraPenalidadeRepository extends JpaRepository<RegraPenalidade, Long> {

    Optional<RegraPenalidade> findByTipoInfracaoAndAtivaTrue(TipoInfracao tipoInfracao);

    boolean existsByTipoInfracaoAndAtivaTrue(TipoInfracao tipoInfracao);
}
