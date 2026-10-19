package br.com.homelive.security;

import br.com.homelive.model.Usuario;
import br.com.homelive.model.enums.TipoUsuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

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
    private final boolean ativo;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.tipo = usuario.getTipoUsuario();
        this.ativo = usuario.isAtivo();
    }

    public Long getId() {
        return id;
    }

    public TipoUsuario getTipo() {
        return tipo;
    }

    public boolean isSindico() {
        return tipo == TipoUsuario.SINDICO;
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
