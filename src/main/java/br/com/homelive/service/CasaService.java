package br.com.homelive.service;

import br.com.homelive.dto.CasaDTO;
import br.com.homelive.exception.RecursoNaoEncontradoException;
import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Casa;
import br.com.homelive.model.Morador;
import br.com.homelive.model.RegraPenalidade;
import br.com.homelive.model.Sindico;
import br.com.homelive.model.Usuario;
import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.repository.CasaRepository;
import br.com.homelive.repository.MoradorRepository;
import br.com.homelive.repository.RegraPenalidadeRepository;
import br.com.homelive.repository.UsuarioRepository;
import br.com.homelive.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Ciclo de vida da casa: fundar (quem funda vira síndico), entrar com o código de convite
 * e aprovar/recusar quem pediu para entrar.
 */
@Service
@RequiredArgsConstructor
public class CasaService {

    /** Sem 0/O e 1/I, que se confundem quando o código é ditado ou copiado à mão. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CasaRepository casaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MoradorRepository moradorRepository;
    private final RegraPenalidadeRepository regraPenalidadeRepository;

    /**
     * Funda uma casa. A pessoa se cadastrou como morador sem casa; ao fundar, esse perfil vazio é
     * trocado por um síndico com os mesmos dados (o JPA não permite mudar a subclasse de uma linha).
     * O login é por e-mail, então a próxima requisição já entra como síndico.
     */
    @Transactional
    public CasaDTO.Response criar(UsuarioAutenticado logado, CasaDTO.CriarRequest request) {
        Usuario usuario = buscarUsuario(logado);
        if (usuario.getCasa() != null) {
            throw new RegraNegocioException("Você já pertence a uma casa.");
        }
        Casa casa = casaRepository.save(new Casa(request.getNome(), gerarCodigoUnico()));
        regraPenalidadeRepository.saveAll(RegraPenalidade.padroesDa(casa));

        if (usuario instanceof Morador moradorSemCasa) {
            Sindico sindico = new Sindico(moradorSemCasa.getNome(), moradorSemCasa.getEmail(),
                    moradorSemCasa.getSenhaHash(), moradorSemCasa.getTelefone(), null);
            sindico.entrarNaCasa(casa);
            usuarioRepository.delete(moradorSemCasa);
            usuarioRepository.flush(); // libera o e-mail (único) antes de inserir o síndico
            usuarioRepository.save(sindico);
        } else {
            usuario.entrarNaCasa(casa);
            usuarioRepository.save(usuario);
        }
        return toResponse(casa, true);
    }

    /** Pede para entrar numa casa pelo código. O morador fica PENDENTE até o síndico aprovar. */
    @Transactional
    public CasaDTO.Response entrar(UsuarioAutenticado logado, CasaDTO.EntrarRequest request) {
        Usuario usuario = buscarUsuario(logado);
        if (!(usuario instanceof Morador morador)) {
            throw new RegraNegocioException("Você já pertence a uma casa.");
        }
        Casa casa = casaRepository.findByCodigoConvite(request.getCodigoConvite().trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Código de convite inválido."));
        morador.solicitarEntrada(casa, request.getNumeroQuarto());
        moradorRepository.save(morador);
        return toResponse(casa, false);
    }

    @Transactional(readOnly = true)
    public CasaDTO.Response minhaCasa(UsuarioAutenticado logado) {
        Usuario usuario = buscarUsuario(logado);
        if (usuario.getCasa() == null) {
            throw new RecursoNaoEncontradoException("Você ainda não pertence a nenhuma casa.");
        }
        return toResponse(usuario.getCasa(), logado.isSindico());
    }

    @Transactional(readOnly = true)
    public List<CasaDTO.MembroResponse> listarPendentes(UsuarioAutenticado sindico) {
        return moradorRepository.findByCasaIdAndStatusMembro(casaDoSindico(sindico), StatusMembro.PENDENTE)
                .stream().map(this::toMembro).toList();
    }

    @Transactional
    public CasaDTO.MembroResponse aprovar(UsuarioAutenticado sindico, Long moradorId) {
        Morador morador = moradorDaMinhaCasa(sindico, moradorId);
        morador.aprovarEntrada();
        return toMembro(moradorRepository.save(morador));
    }

    @Transactional
    public CasaDTO.MembroResponse recusar(UsuarioAutenticado sindico, Long moradorId) {
        Morador morador = moradorDaMinhaCasa(sindico, moradorId);
        morador.recusarEntrada();
        return toMembro(moradorRepository.save(morador));
    }

    // ---- auxiliares ----

    /** Só enxerga moradores da própria casa: de outra casa, responde 404 como se não existisse. */
    private Morador moradorDaMinhaCasa(UsuarioAutenticado sindico, Long moradorId) {
        Long casaId = casaDoSindico(sindico);
        return moradorRepository.findById(moradorId)
                .filter(m -> m.getCasa() != null && casaId.equals(m.getCasa().getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador não encontrado com o ID: " + moradorId));
    }

    private Long casaDoSindico(UsuarioAutenticado sindico) {
        if (sindico.getCasaId() == null) {
            throw new RegraNegocioException("Crie uma casa antes de gerenciar moradores.");
        }
        return sindico.getCasaId();
    }

    private Usuario buscarUsuario(UsuarioAutenticado logado) {
        return usuarioRepository.findById(logado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private String gerarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("HL-");
            for (int i = 0; i < 4; i++) {
                sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (casaRepository.existsByCodigoConvite(codigo));
        return codigo;
    }

    private CasaDTO.Response toResponse(Casa casa, boolean mostrarCodigo) {
        return new CasaDTO.Response(casa.getId(), casa.getNome(), mostrarCodigo ? casa.getCodigoConvite() : null);
    }

    private CasaDTO.MembroResponse toMembro(Morador m) {
        return new CasaDTO.MembroResponse(m.getId(), m.getNome(), m.getEmail(), m.getNumeroQuarto(), m.getStatusMembro());
    }
}
