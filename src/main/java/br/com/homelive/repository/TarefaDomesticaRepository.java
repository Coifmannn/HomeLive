package br.com.homelive.repository;

import br.com.homelive.model.TarefaDomestica;
import br.com.homelive.model.enums.StatusTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TarefaDomesticaRepository extends JpaRepository<TarefaDomestica, Long> {
    List<TarefaDomestica> findByCasaId(Long casaId);
    Optional<TarefaDomestica> findByIdAndCasaId(Long id, Long casaId);
    List<TarefaDomestica> findByCasaIdAndResponsavelId(Long casaId, Long moradorId);
    List<TarefaDomestica> findByCasaIdAndStatus(Long casaId, StatusTarefa status);
    long countByCasaIdAndStatus(Long casaId, StatusTarefa status);
    List<TarefaDomestica> findByCasaIdAndStatusAndDataPrazoBefore(Long casaId, StatusTarefa status, LocalDate data);
}
