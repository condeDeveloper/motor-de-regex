package br.com.conde.regex.sintaxe;

import br.com.conde.regex.ErroDeRegex;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê a expressão e monta a árvore, por descida recursiva.
 *
 * <p>A gramática tem três níveis, do menos para o mais unido: alternância,
 * sequência e repetição. É essa ordem que faz {@code ab|cd} ser
 * {@code (ab)|(cd)} e não {@code a(b|c)d}.
 */
public final class Analisador {

    private final String expressao;
    private int i;

    private Analisador(String expressao) {
        this.expressao = expressao;
    }

    /** Interpreta a expressão inteira. */
    public static No analisar(String expressao) {
        if (expressao == null) {
            throw new ErroDeRegex("", 0, "expressão nula");
        }

        var analisador = new Analisador(expressao);
        var no = analisador.alternancia();

        if (!analisador.acabou()) {
            throw new ErroDeRegex(expressao, analisador.i, "sobrou '" + analisador.atual() + "'");
        }

        return no;
    }

    private No alternancia() {
        var no = sequencia();

        while (aceitar('|')) {
            no = new No.Alternancia(no, sequencia());
        }

        return no;
    }

    private No sequencia() {
        var partes = new ArrayList<No>();

        while (!acabou() && atual() != '|' && atual() != ')') {
            partes.add(repeticao());
        }

        return switch (partes.size()) {
            case 0 -> new No.Vazio();
            case 1 -> partes.get(0);
            default -> new No.Sequencia(partes);
        };
    }

    private No repeticao() {
        var corpo = atomo();

        while (!acabou()) {
            var c = atual();

            if (c == '*') {
                i++;
                corpo = new No.Repeticao(corpo, 0, No.Repeticao.INFINITO);
            } else if (c == '+') {
                i++;
                corpo = new No.Repeticao(corpo, 1, No.Repeticao.INFINITO);
            } else if (c == '?') {
                i++;
                corpo = new No.Repeticao(corpo, 0, 1);
            } else if (c == '{' && pareceContador()) {
                corpo = contador(corpo);
            } else {
                break;
            }
        }

        return corpo;
    }

    /**
     * A chave só abre um contador se o que vem depois for mesmo {@code n},
     * {@code n,} ou {@code n,m}. Uma chave solta é um caractere comum, que é
     * como a maioria dos motores trata.
     */
    private boolean pareceContador() {
        var j = i + 1;
        var digitos = 0;

        while (j < expressao.length() && Character.isDigit(expressao.charAt(j))) {
            j++;
            digitos++;
        }

        if (digitos == 0) {
            return false;
        }

        if (j < expressao.length() && expressao.charAt(j) == ',') {
            j++;
            while (j < expressao.length() && Character.isDigit(expressao.charAt(j))) {
                j++;
            }
        }

        return j < expressao.length() && expressao.charAt(j) == '}';
    }

    private No contador(No corpo) {
        var inicio = i;
        i++;

        var minimo = numero();
        var maximo = minimo;

        if (aceitar(',')) {
            maximo = (atual() == '}') ? No.Repeticao.INFINITO : numero();
        }

        if (!aceitar('}')) {
            throw new ErroDeRegex(expressao, i, "faltou fechar a chave");
        }

        if (maximo != No.Repeticao.INFINITO && maximo < minimo) {
            throw new ErroDeRegex(expressao, inicio, "o máximo é menor que o mínimo");
        }

        return new No.Repeticao(corpo, minimo, maximo);
    }

    private int numero() {
        var inicio = i;

        while (!acabou() && Character.isDigit(atual())) {
            i++;
        }

        if (inicio == i) {
            throw new ErroDeRegex(expressao, i, "esperava um número");
        }

        try {
            return Integer.parseInt(expressao.substring(inicio, i));
        } catch (NumberFormatException erro) {
            throw new ErroDeRegex(expressao, inicio, "número grande demais");
        }
    }

    private No atomo() {
        if (acabou()) {
            throw new ErroDeRegex(expressao, i, "expressão termina onde deveria haver algo");
        }

        var c = atual();

        return switch (c) {
            case '(' -> grupo();
            case '[' -> new No.Classe(classe());
            case '.' -> consumir(new No.Classe(ClasseDeCaracteres.qualquer()));
            case '^' -> consumir(new No.Ancora(No.Ancora.Tipo.INICIO));
            case '$' -> consumir(new No.Ancora(No.Ancora.Tipo.FIM));
            case '\\' -> escape();
            case '*', '+', '?' -> throw new ErroDeRegex(expressao, i, "quantificador sem nada antes");
            default -> consumir(new No.Classe(ClasseDeCaracteres.de(c)));
        };
    }

    private No grupo() {
        i++;

        // (?: existe só para agrupar sem capturar; como este motor não captura
        // grupo nenhum, os dois tipos acabam no mesmo lugar.
        if (i + 1 < expressao.length() && atual() == '?' && expressao.charAt(i + 1) == ':') {
            i += 2;
        }

        var dentro = alternancia();

        if (!aceitar(')')) {
            throw new ErroDeRegex(expressao, i, "faltou fechar o parêntese");
        }

        return dentro;
    }

    private No escape() {
        i++;

        if (acabou()) {
            throw new ErroDeRegex(expressao, i, "a barra invertida ficou sozinha no fim");
        }

        var c = atual();
        i++;

        return switch (c) {
            case 'd' -> new No.Classe(ClasseDeCaracteres.digitos());
            case 'D' -> new No.Classe(ClasseDeCaracteres.digitos().negar());
            case 'w' -> new No.Classe(ClasseDeCaracteres.palavra());
            case 'W' -> new No.Classe(ClasseDeCaracteres.palavra().negar());
            case 's' -> new No.Classe(ClasseDeCaracteres.espacos());
            case 'S' -> new No.Classe(ClasseDeCaracteres.espacos().negar());
            case 'b' -> new No.Ancora(No.Ancora.Tipo.BORDA_DE_PALAVRA);
            case 'B' -> new No.Ancora(No.Ancora.Tipo.SEM_BORDA_DE_PALAVRA);
            case 'n' -> new No.Classe(ClasseDeCaracteres.de('\n'));
            case 't' -> new No.Classe(ClasseDeCaracteres.de('\t'));
            case 'r' -> new No.Classe(ClasseDeCaracteres.de('\r'));
            default -> new No.Classe(ClasseDeCaracteres.de(c));
        };
    }

    private ClasseDeCaracteres classe() {
        var inicio = i;
        i++;

        var negada = aceitar('^');
        var partes = new ArrayList<ClasseDeCaracteres>();

        while (!acabou() && atual() != ']') {
            partes.add(itemDaClasse());
        }

        if (!aceitar(']')) {
            throw new ErroDeRegex(expressao, inicio, "faltou fechar o colchete");
        }

        if (partes.isEmpty()) {
            throw new ErroDeRegex(expressao, inicio, "classe de caracteres vazia");
        }

        return ClasseDeCaracteres.uniao(partes, negada);
    }

    private ClasseDeCaracteres itemDaClasse() {
        if (atual() == '\\') {
            var escapado = escape();

            if (escapado instanceof No.Classe classe) {
                return classe.classe();
            }

            throw new ErroDeRegex(expressao, i, "âncora não vale dentro de classe");
        }

        var inicio = atual();
        i++;

        // O traço só forma faixa entre dois caracteres; em "[a-]" ele é literal.
        if (!acabou() && atual() == '-' && i + 1 < expressao.length() && expressao.charAt(i + 1) != ']') {
            i++;
            var fim = atual();
            i++;

            if (fim < inicio) {
                throw new ErroDeRegex(expressao, i, "faixa invertida: '" + inicio + "-" + fim + "'");
            }

            return ClasseDeCaracteres.faixa(inicio, fim);
        }

        return ClasseDeCaracteres.de(inicio);
    }

    private No consumir(No no) {
        i++;
        return no;
    }

    private boolean aceitar(char c) {
        if (!acabou() && atual() == c) {
            i++;
            return true;
        }
        return false;
    }

    private char atual() {
        return expressao.charAt(i);
    }

    private boolean acabou() {
        return i >= expressao.length();
    }
}
