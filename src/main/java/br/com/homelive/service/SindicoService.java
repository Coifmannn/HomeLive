package br.com.homelive.service;

import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.Sindico;
import br.com.homelive.repository.SindicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Síndicos nascem ao fundar uma casa (veja {@link CasaService}); aqui só se busca o já existente. */
@Service
@RequiredArgsConstructor
public class SindicoService {

    private final SindicoRepository sindicoRepository;

    public Sindico buscarEntidade(Long id) {
        return sindicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Síndico não encontrado com o ID: " + id));
    }
}
