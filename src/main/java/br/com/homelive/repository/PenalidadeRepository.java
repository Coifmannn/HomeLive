package br.com.homelive.repository;

import br.com.homelive.model.Penalidade;
import br.com.homelive.model.enums.TipoInfracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PenalidadeRepository extends JpaRepository<Penalidade, Long> {

    List<Penalidade> findAllByOrderByDataAplicacaoDescIdDesc();

    List<Penalidade> findByMoradorIdOrderByDataAplicacaoDescIdDesc(Long moradorId);

    long countByMoradorIdAndTipoInfracaoAndDataAplicacaoGreaterThanEqual(
            Long moradorId, TipoInfracao tipoInfracao, LocalDateTime desde);
}
