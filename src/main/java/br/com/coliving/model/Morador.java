package br.com.coliving.model;

import br.com.coliving.model.enums.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "moradores")
@Getter
@Setter
@NoArgsConstructor
public class Morador extends Usuario {

    @Column(nullable = false)
    private String numeroQuarto;

    private Integer diasEstadia = 30;

    private Double consumoDeclaradoKwh = 100.0;

    private Integer pontosConvivencia = 100;

    private LocalDate dataEntrada = LocalDate.now();

    public Morador(String nome, String email, String telefone, String numeroQuarto, Integer diasEstadia, Double consumoDeclaradoKwh) {
        this.setNome(nome);
        this.setEmail(email);
        this.setTelefone(telefone);
        this.setTipoUsuario(TipoUsuario.MORADOR);
        this.numeroQuarto = numeroQuarto;
        this.diasEstadia = diasEstadia != null ? diasEstadia : 30;
        this.consumoDeclaradoKwh = consumoDeclaradoKwh != null ? consumoDeclaradoKwh : 100.0;
        this.pontosConvivencia = 100;
        this.dataEntrada = LocalDate.now();
    }

    public void adicionarPontos(int pontos) {
        this.pontosConvivencia += pontos;
    }

    public void deduzirPontos(int pontos) {
        this.pontosConvivencia = Math.max(0, this.pontosConvivencia - pontos);
    }
}
