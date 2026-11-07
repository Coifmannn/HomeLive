package br.com.homelive.repository;

import br.com.homelive.model.Morador;
import br.com.homelive.model.enums.StatusMembro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Long> {
    List<Morador> findByCasaIdAndStatusMembro(Long casaId, StatusMembro statusMembro);
    List<Morador> findByCasaIdAndStatusMembroAndAtivoTrue(Long casaId, StatusMembro statusMembro);
    List<Morador> findByCasaIdAndStatusMembroOrderByPontosConvivenciaDesc(Long casaId, StatusMembro statusMembro);
    long countByCasaIdAndStatusMembroAndAtivoTrue(Long casaId, StatusMembro statusMembro);
    Optional<Morador> findByIdAndCasaIdAndStatusMembro(Long id, Long casaId, StatusMembro statusMembro);
}
