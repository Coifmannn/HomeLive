package br.com.homelive.model.enums;

/**
 * Situação de um usuário em relação a uma casa (HomeLive).
 * SEM_CASA: acabou de se cadastrar. PENDENTE: pediu para entrar e espera o síndico. ATIVO: já é da casa.
 */
public enum StatusMembro {
    SEM_CASA,
    PENDENTE,
    ATIVO
}
