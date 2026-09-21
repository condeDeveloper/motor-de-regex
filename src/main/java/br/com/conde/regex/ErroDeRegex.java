package br.com.conde.regex;

/**
 * Falha ao interpretar uma expressão. Guarda a posição do caractere que causou
 * o problema, porque numa expressão longa dizer só "sintaxe inválida" não
 * ajuda ninguém.
 */
public class ErroDeRegex extends RuntimeException {

    private final String expressao;
    private final int posicao;

    public ErroDeRegex(String expressao, int posicao, String motivo) {
        super("Expressão inválida na posição " + posicao + ": " + motivo + " — " + expressao);
        this.expressao = expressao;
        this.posicao = posicao;
    }

    /** A expressão que foi recusada. */
    public String expressao() {
        return expressao;
    }

    /** Onde, na expressão, o problema apareceu. */
    public int posicao() {
        return posicao;
    }
}
