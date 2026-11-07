package br.com.homelive.model;

import br.com.homelive.exception.RegraNegocioException;
import br.com.homelive.model.enums.StatusMembro;
import br.com.homelive.model.enums.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** Só é conhecido quando o morador pede para entrar numa casa. */
    private String numeroQuarto;

    private Integer diasEstadia = 30;

    private Integer pontosConvivencia = 100;

    private LocalDate dataEntrada = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMembro statusMembro = StatusMembro.ATIVO;

    /** Cadastro público: a pessoa só cria a conta e ainda não tem casa nem quarto. */
    public Morador(String nome, String email, String senhaHash, String telefone) {
        super(nome, email, senhaHash, telefone);
        this.statusMembro = StatusMembro.SEM_CASA;
    }

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

    /** Pede para entrar numa casa; só passa a valer depois que o síndico aprovar. */
    public void solicitarEntrada(Casa casa, String quarto) {
        if (quarto == null || quarto.isBlank()) {
            throw new RegraNegocioException("O número do quarto é obrigatório.");
        }
        entrarNaCasa(casa);
        this.numeroQuarto = quarto.trim();
        this.statusMembro = StatusMembro.PENDENTE;
    }

    public void aprovarEntrada() {
        if (statusMembro != StatusMembro.PENDENTE) {
            throw new RegraNegocioException("Só é possível aprovar quem está com a entrada pendente.");
        }
        this.statusMembro = StatusMembro.ATIVO;
        this.dataEntrada = LocalDate.now();
    }

    /** Recusar devolve a pessoa a "sem casa", livre para tentar outro código. */
    public void recusarEntrada() {
        if (statusMembro != StatusMembro.PENDENTE) {
            throw new RegraNegocioException("Só é possível recusar quem está com a entrada pendente.");
        }
        sairDaCasa();
        this.numeroQuarto = null;
        this.statusMembro = StatusMembro.SEM_CASA;
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
