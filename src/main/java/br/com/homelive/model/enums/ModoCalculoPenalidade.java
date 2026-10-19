package br.com.homelive.model.enums;

/**
 * Strategy implementada no próprio enum: cada modo sabe calcular os pontos a descontar.
 * Recebe valores primitivos (e não a regra) para o enum não depender das entidades.
 */
public enum ModoCalculoPenalidade {

    /** Sempre os mesmos pontos, não importa quantas vezes o morador reincida. */
    FIXA {
        @Override
        public int calcular(int pontosBase, int incrementoPorReincidencia, Integer tetoPontos, long reincidencias) {
            return pontosBase;
        }
    },

    /** Cada reincidência dentro da janela soma um incremento, até o teto (se houver). */
    PROGRESSIVA {
        @Override
        public int calcular(int pontosBase, int incrementoPorReincidencia, Integer tetoPontos, long reincidencias) {
            long total = pontosBase + (long) incrementoPorReincidencia * reincidencias;
            if (tetoPontos != null) {
                total = Math.min(total, tetoPontos);
            }
            return (int) total;
        }
    };

    /**
     * @param reincidencias quantas penalidades do mesmo tipo o morador já recebeu na janela da regra
     */
    public abstract int calcular(int pontosBase, int incrementoPorReincidencia, Integer tetoPontos, long reincidencias);
}
