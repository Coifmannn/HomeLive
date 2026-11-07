package br.com.homelive;

import br.com.homelive.model.Casa;

/** Dados de apoio para testes que não passam pelo banco. */
public final class Fixtures {

    private Fixtures() {
    }

    /** Uma casa só em memória (sem id): serve para construir entidades em testes unitários. */
    public static Casa casa() {
        return new Casa("Casa de Teste", "HL-TEST");
    }
}
