package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "moradores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Morador extends Usuario {

    @Column(nullable = false)
    private String numeroQuarto;

    private Integer diasEstadia = 30;

    private Integer pontosConvivencia = 100;

    private LocalDate dataEntrada = LocalDate.now();

    public Morador(String nome, String email, String senhaHash, String telefone, String numeroQuarto, Integer diasEstadia) {
        super(nome, email, senhaHash, telefone);
        this.numeroQuarto = numeroQuarto;
        this.diasEstadia = diasEstadia != null ? diasEstadia : 30;
        this.pontosConvivencia = 100;
        this.dataEntrada = LocalDate.now();
    }

    @Override
    public TipoUsuario getTipoUsuario() {
        return TipoUsuario.MORADOR;
    }

    public void adicionarPontos(int pontos) {
        if (pontos < 0) {
            throw new RegraNegocioException("A quantidade de pontos a adicionar não pode ser negativa.");
        }
        this.pontosConvivencia += pontos;
    }

    public void deduzirPontos(int pontos) {
        if (pontos < 0) {
            throw new RegraNegocioException("A quantidade de pontos a deduzir não pode ser negativa.");
        }
        this.pontosConvivencia = Math.max(0, this.pontosConvivencia - pontos);
    }

    public void atualizarEstadia(Integer dias) {
        if (dias == null || dias < 0) {
            throw new RegraNegocioException("Os dias de estadia devem ser zero ou positivos.");
        }
        this.diasEstadia = dias;
    }
}
