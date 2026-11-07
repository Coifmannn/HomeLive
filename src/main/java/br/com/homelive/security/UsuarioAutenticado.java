package br.com.homelive.security;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.Usuario;
import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.model.enums.TipoUsuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador entre o domínio e o Spring Security: guarda só um retrato imutável do usuário
 * (id, e-mail, hash, papel), sem manter a entidade JPA viva durante a requisição.
 * O papel vem do método polimórfico {@link Usuario#getTipoUsuario()}.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String email;
    private final String senhaHash;
    private final TipoUsuario tipo;
    /** Nulo enquanto a pessoa não criou nem entrou em uma casa. */
    private final Long casaId;
    private final StatusMembro statusMembro;
    private final boolean ativo;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.tipo = usuario.getTipoUsuario();
        this.statusMembro = usuario.getStatusMembro();
        this.casaId = usuario.getCasa() != null ? usuario.getCasa().getId() : null;
        this.ativo = usuario.isAtivo();
    }

    public Long getId() {
        return id;
    }

    public StatusMembro getStatusMembro() {
        return statusMembro;
    }

    /** Só quem pertence a uma casa e já foi aprovado enxerga os dados dela. */
    public boolean isMembroAtivo() {
        return casaId != null && statusMembro == StatusMembro.ATIVO;
    }

    public Long getCasaId() {
        return casaId;
    }

    public TipoUsuario getTipo() {
        return tipo;
    }

    public boolean isSindico() {
        return tipo == TipoUsuario.SINDICO;
    }

    /**
     * Regra de "dono": o síndico pode agir sobre qualquer recurso; o morador só sobre os próprios.
     * Passe {@code null} quando o recurso não tem dono (ex.: tarefa sem responsável): só o síndico passa.
     */
    public void exigirSindicoOuDono(Long donoId) {
        if (!isSindico() && !Objects.equals(id, donoId)) {
            throw new AccessDeniedException("Você só pode realizar esta ação sobre os seus próprios dados.");
        }
    }

    /**
     * Em nome de qual morador a ação será feita. O morador age sempre em nome próprio (e não pode apontar
     * outro); o síndico precisa dizer qual morador.
     */
    public Long moradorAlvo(Long moradorIdInformado) {
        if (isSindico()) {
            if (moradorIdInformado == null) {
                throw new RegraNegocioException("O síndico deve informar o morador (moradorId).");
            }
            return moradorIdInformado;
        }
        exigirSindicoOuDono(moradorIdInformado != null ? moradorIdInformado : id);
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Usuário desativado não consegue mais entrar. */
    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
