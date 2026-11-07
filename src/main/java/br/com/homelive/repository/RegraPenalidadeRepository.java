package br.com.homelive.repository;

import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.enums.TipoInfracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegraPenalidadeRepository extends JpaRepository<RegraPenalidade, Long> {
    List<RegraPenalidade> findByCasaId(Long casaId);
    Optional<RegraPenalidade> findByIdAndCasaId(Long id, Long casaId);
    Optional<RegraPenalidade> findByCasaIdAndTipoInfracaoAndAtivaTrue(Long casaId, TipoInfracao tipoInfracao);
    boolean existsByCasaIdAndTipoInfracaoAndAtivaTrue(Long casaId, TipoInfracao tipoInfracao);
}
