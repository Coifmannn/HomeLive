package br.com.homelive.model;

import br.com.homelive.model.enums.TipoUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sindicos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sindico extends Usuario {

    private String departamento = "Administração HomeLive 2050";
    private LocalDate inicioMandato = LocalDate.now();
    private LocalDate fimMandato = LocalDate.now().plusYears(1);

    public Sindico(String nome, String email, String senhaHash, String telefone, String departamento) {
        super(nome, email, senhaHash, telefone);
        this.departamento = departamento != null ? departamento : "Administração HomeLive 2050";
        this.inicioMandato = LocalDate.now();
        this.fimMandato = LocalDate.now().plusYears(1);
    }

    @Override
    public TipoUsuario getTipoUsuario() {
        return TipoUsuario.SINDICO;
    }
}
