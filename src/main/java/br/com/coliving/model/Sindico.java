package br.com.coliving.model;

import br.com.coliving.model.enums.TipoUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "sindicos")
@Getter
@Setter
@NoArgsConstructor
public class Sindico extends Usuario {

    private String departamento = "Administração Co-Living 2050";
    private LocalDate inicioMandato = LocalDate.now();
    private LocalDate fimMandato = LocalDate.now().plusYears(1);

    public Sindico(String nome, String email, String telefone, String departamento) {
        this.setNome(nome);
        this.setEmail(email);
        this.setTelefone(telefone);
        this.setTipoUsuario(TipoUsuario.SINDICO);
        this.departamento = departamento != null ? departamento : "Administração Co-Living 2050";
        this.inicioMandato = LocalDate.now();
        this.fimMandato = LocalDate.now().plusYears(1);
    }
}
