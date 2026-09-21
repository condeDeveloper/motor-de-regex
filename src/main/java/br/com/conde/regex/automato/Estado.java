package br.com.conde.regex.automato;

import br.com.conde.regex.sintaxe.ClasseDeCaracteres;
import br.com.conde.regex.sintaxe.No;

/**
 * Um estado do autômato.
 *
 * <p>São só quatro tipos, e é essa economia que faz a construção de Thompson
 * ser simples: consumir um caractere, bifurcar em dois caminhos sem consumir
 * nada, conferir uma âncora e aceitar.
 */
public final class Estado {

    /** O que o estado faz. */
    public enum Tipo {
        /** Consome um caractere se ele estiver na classe. */
        CARACTERE,
        /** Segue por dois caminhos ao mesmo tempo, sem consumir nada. */
        DIVISAO,
        /** Só passa se a âncora valer naquela posição. */
        ANCORA,
        /** Chegou aqui, casou. */
        ACEITACAO,
    }

    final int id;
    final Tipo tipo;
    final ClasseDeCaracteres classe;
    final No.Ancora.Tipo ancora;

    Estado proximo;
    Estado alternativo;

    private Estado(int id, Tipo tipo, ClasseDeCaracteres classe, No.Ancora.Tipo ancora) {
        this.id = id;
        this.tipo = tipo;
        this.classe = classe;
        this.ancora = ancora;
    }

    static Estado caractere(int id, ClasseDeCaracteres classe) {
        return new Estado(id, Tipo.CARACTERE, classe, null);
    }

    static Estado divisao(int id) {
        return new Estado(id, Tipo.DIVISAO, null, null);
    }

    static Estado ancora(int id, No.Ancora.Tipo tipo) {
        return new Estado(id, Tipo.ANCORA, null, tipo);
    }

    static Estado aceitacao(int id) {
        return new Estado(id, Tipo.ACEITACAO, null, null);
    }

    /** Identificador único dentro do autômato. */
    public int id() {
        return id;
    }

    /** O que este estado faz. */
    public Tipo tipo() {
        return tipo;
    }

    /** A classe de caracteres, quando o tipo é {@link Tipo#CARACTERE}. */
    public ClasseDeCaracteres classe() {
        return classe;
    }

    /** A âncora, quando o tipo é {@link Tipo#ANCORA}. */
    public No.Ancora.Tipo ancora() {
        return ancora;
    }

    /** O estado seguinte. */
    public Estado proximo() {
        return proximo;
    }

    /** O segundo caminho, quando o tipo é {@link Tipo#DIVISAO}. */
    public Estado alternativo() {
        return alternativo;
    }

    @Override
    public String toString() {
        return switch (tipo) {
            case CARACTERE -> id + ":" + classe;
            case DIVISAO -> id + ":divisão";
            case ANCORA -> id + ":" + ancora;
            case ACEITACAO -> id + ":aceita";
        };
    }
}
