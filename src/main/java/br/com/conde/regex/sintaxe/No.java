package br.com.conde.regex.sintaxe;

import java.util.List;

/**
 * Os nós da árvore de uma expressão regular.
 *
 * <p>É só estrutura de dados: quem sabe transformar isso em autômato é o
 * construtor, e quem sabe casar texto é o simulador.
 */
public sealed interface No {

    /** Casa com a string vazia. Aparece em alternâncias como {@code (a|)}. */
    record Vazio() implements No {
        @Override
        public String toString() {
            return "";
        }
    }

    /** Um conjunto de caracteres aceitos em uma posição. */
    record Classe(ClasseDeCaracteres classe) implements No {
        @Override
        public String toString() {
            return classe.toString();
        }
    }

    /** Duas ou mais partes em sequência. */
    record Sequencia(List<No> partes) implements No {
        public Sequencia {
            partes = List.copyOf(partes);
        }

        @Override
        public String toString() {
            return partes.stream().map(Object::toString).reduce("", String::concat);
        }
    }

    /** Uma alternativa ou outra. */
    record Alternancia(No esquerda, No direita) implements No {
        @Override
        public String toString() {
            return "(" + esquerda + "|" + direita + ")";
        }
    }

    /**
     * Repetição de {@code minimo} a {@code maximo} vezes. O máximo
     * {@link #INFINITO} cobre {@code *}, {@code +} e {@code {n,}}.
     */
    record Repeticao(No corpo, int minimo, int maximo) implements No {

        /** Marca a repetição sem limite superior. */
        public static final int INFINITO = -1;

        public Repeticao {
            if (minimo < 0) {
                throw new IllegalArgumentException("O mínimo não pode ser negativo.");
            }
            if (maximo != INFINITO && maximo < minimo) {
                throw new IllegalArgumentException("O máximo não pode ser menor que o mínimo.");
            }
        }

        /** Indica se a repetição não tem teto. */
        public boolean semTeto() {
            return maximo == INFINITO;
        }

        @Override
        public String toString() {
            var sufixo = (minimo == 0 && semTeto()) ? "*"
                    : (minimo == 1 && semTeto()) ? "+"
                    : (minimo == 0 && maximo == 1) ? "?"
                    : semTeto() ? "{" + minimo + ",}"
                    : minimo == maximo ? "{" + minimo + "}"
                    : "{" + minimo + "," + maximo + "}";

            return corpo + sufixo;
        }
    }

    /** Uma âncora, que não consome caractere nenhum. */
    record Ancora(Tipo tipo) implements No {

        /** Que borda a âncora exige. */
        public enum Tipo {
            /** Começo do texto. */
            INICIO,
            /** Fim do texto. */
            FIM,
            /** Borda de palavra. */
            BORDA_DE_PALAVRA,
            /** Onde não há borda de palavra. */
            SEM_BORDA_DE_PALAVRA,
        }

        @Override
        public String toString() {
            return switch (tipo) {
                case INICIO -> "^";
                case FIM -> "$";
                case BORDA_DE_PALAVRA -> "\\b";
                case SEM_BORDA_DE_PALAVRA -> "\\B";
            };
        }
    }
}
