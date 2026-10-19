package br.com.homelive.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "espacos_comuns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EspacoComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do espaço é obrigatório")
    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Positive(message = "A capacidade deve ser maior que zero")
    private Integer capacidadeMaxima = 4;

    private String regrasUso;

    private boolean ativo = true;

    public EspacoComum(String nome, String descricao, Integer capacidadeMaxima, String regrasUso) {
        this.nome = nome;
        this.descricao = descricao;
        this.capacidadeMaxima = capacidadeMaxima;
        this.regrasUso = regrasUso;
        this.ativo = true;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void alternarStatus() {
        this.ativo = !this.ativo;
    }
}
