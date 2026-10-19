package br.com.homelive.model.enums;

public enum TipoInfracao {
    TAREFA_ATRASADA(true),
    CANCELAMENTO_TARDIO(true),
    NAO_COMPARECIMENTO(true),
    /** Aplicada diretamente pelo síndico, sem regra configurável. */
    MANUAL(false);

    private final boolean configuravel;

    TipoInfracao(boolean configuravel) {
        this.configuravel = configuravel;
    }

    /** Se o síndico pode cadastrar uma regra para esta infração. */
    public boolean isConfiguravel() {
        return configuravel;
    }
}
