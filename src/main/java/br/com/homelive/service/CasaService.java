package br.com.homelive.service;

import br.com.homelive.dto.CasaDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.model.Casa;
import br.com.homelive.repository.CasaRepository;
import br.com.homelive.repository.UsuarioRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CasaService {

    /** Nome da casa criada automaticamente para um banco antigo que já tinha usuários (ver {@link #garantirCasaSeHouverUsuarios}). */
    static final String NOME_CASA_PADRAO = "Minha residência HomeLive";

    private final CasaRepository casaRepository;
    private final UsuarioRepository usuarioRepository;

    public Optional<Casa> buscarCasa() {
        return casaRepository.findFirstByOrderByIdAsc();
    }

    public Casa casaAtual() {
        return buscarCasa().orElseThrow(() -> new RecursoNaoEncontradoException("A residência ainda não foi criada."));
    }

    /**
     * Casa que já tem alguém para passar o código de convite. Uma casa sem nenhum usuário (ex.: banco em que as contas
     * foram apagadas) não conta: ninguém poderia entrar, então o cadastro volta ao fluxo de criar a residência.
     */
    public Optional<Casa> buscarCasaComMoradores() {
        return buscarCasa().filter(casa -> usuarioRepository.count() > 0);
    }

    /** Público: diz se a residência já existe, para o cadastro escolher entre criar e entrar. */
    public CasaDTO.Estado estado() {
        return buscarCasaComMoradores()
                .map(casa -> new CasaDTO.Estado(true, casa.getNome()))
                .orElse(new CasaDTO.Estado(false, null));
    }

    /** Dados da casa. O código de convite só vai para o síndico. */
    public CasaDTO.Response obter(UsuarioAutenticado usuario) {
        return paraResposta(casaAtual(), usuario.isSindico());
    }

    @Transactional
    public CasaDTO.Response renovarCodigo() {
        Casa casa = casaAtual();
        casa.renovarCodigo();
        return paraResposta(casaRepository.save(casa), true);
    }

    @Transactional
    public Casa criar(String nome) {
        casaRepository.deleteAll(); // sobra de uma casa sem usuários; a instalação tem uma casa só
        return casaRepository.save(new Casa(nome));
    }

    /**
     * Banco criado antes de existir a entidade Casa: já tem usuários e nenhuma casa. Sem isso, o sistema acharia
     * que está vazio e deixaria um desconhecido "criar a residência" e virar síndico.
     */
    @Transactional
    public void garantirCasaSeHouverUsuarios() {
        if (casaRepository.count() == 0 && usuarioRepository.count() > 0) {
            casaRepository.save(new Casa(NOME_CASA_PADRAO));
        }
    }

    private CasaDTO.Response paraResposta(Casa casa, boolean incluirCodigo) {
        return new CasaDTO.Response(casa.getNome(), casa.getDataCriacao(), incluirCodigo ? casa.getCodigoConvite() : null);
    }
}
