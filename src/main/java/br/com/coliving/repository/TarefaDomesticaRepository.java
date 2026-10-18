package br.com.coliving.repository;

import br.com.coliving.model.TarefaDomestica;
import br.com.coliving.model.enums.StatusTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TarefaDomesticaRepository extends JpaRepository<TarefaDomestica, Long> {
    List<TarefaDomestica> findByResponsavelId(Long moradorId);
    List<TarefaDomestica> findByStatus(StatusTarefa status);
    List<TarefaDomestica> findByStatusAndDataPrazoBefore(StatusTarefa status, LocalDate data);
}
