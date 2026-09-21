package br.com.conde.regex;

import br.com.conde.regex.automato.Automato;
import br.com.conde.regex.automato.Construtor;
import br.com.conde.regex.sintaxe.Analisador;
import br.com.conde.regex.sintaxe.No;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A porta de entrada: compila uma expressão e usa ela.
 *
 * <pre>{@code
 * var regex = Regex.compilar("\\d{2}/\\d{2}/\\d{4}");
 *
 * regex.casa("21/09/2026");                       // true
 * regex.encontrar("entregue em 21/09/2026, ok");  // 21/09/2026 em [12, 22)
 * }</pre>
 */
public final class Regex {

    private final String expressao;
    private final No arvore;
    private final Automato automato;

    private Regex(String expressao, No arvore, Automato automato) {
        this.expressao = expressao;
        this.arvore = arvore;
        this.automato = automato;
    }

    /** Compila a expressão, ou lança {@link ErroDeRegex} se ela não fizer sentido. */
    public static Regex compilar(String expressao) {
        var arvore = Analisador.analisar(expressao);
        return new Regex(expressao, arvore, Construtor.construir(arvore));
    }

    /** A expressão como ela foi escrita. */
    public String expressao() {
        return expressao;
    }

    /** A árvore montada a partir da expressão. */
    public No arvore() {
        return arvore;
    }

    /** O autômato construído. */
    public Automato automato() {
        return automato;
    }

    /** Indica se a expressão casa com o texto inteiro, do começo ao fim. */
    public boolean casa(String texto) {
        return texto != null && automato.casaTudo(texto);
    }

    /** Indica se a expressão aparece em algum lugar do texto. */
    public boolean contemEm(String texto) {
        return encontrar(texto).isPresent();
    }

    /** O primeiro trecho encontrado, da esquerda para a direita. */
    public Optional<Correspondencia> encontrar(String texto) {
        return encontrar(texto, 0);
    }

    /**
     * O primeiro trecho a partir de uma posição. Entre duas correspondências
     * que começam no mesmo ponto, vale a mais longa; entre pontos diferentes,
     * vale a mais à esquerda.
     */
    public Optional<Correspondencia> encontrar(String texto, int desde) {
        if (texto == null || desde > texto.length()) {
            return Optional.empty();
        }

        for (var inicio = Math.max(0, desde); inicio <= texto.length(); inicio++) {
            var fim = automato.fimDaCorrespondencia(texto, inicio);

            if (fim >= 0) {
                return Optional.of(new Correspondencia(texto.substring(inicio, fim), inicio, fim));
            }
        }

        return Optional.empty();
    }

    /** Todos os trechos, sem sobreposição. */
    public List<Correspondencia> encontrarTodos(String texto) {
        var achados = new ArrayList<Correspondencia>();

        if (texto == null) {
            return achados;
        }

        var cursor = 0;

        while (cursor <= texto.length()) {
            var achado = encontrar(texto, cursor);

            if (achado.isEmpty()) {
                break;
            }

            var atual = achado.get();
            achados.add(atual);

            // Uma correspondência vazia não anda sozinha; sem empurrar o cursor
            // à mão, uma expressão como "a*" giraria para sempre no mesmo lugar.
            cursor = atual.vazia() ? atual.fim() + 1 : atual.fim();
        }

        return achados;
    }

    /** Troca todas as ocorrências por outro texto. */
    public String substituir(String texto, String porQual) {
        if (texto == null) {
            return null;
        }

        var resultado = new StringBuilder();
        var cursor = 0;

        for (var achado : encontrarTodos(texto)) {
            resultado.append(texto, cursor, achado.inicio()).append(porQual);
            cursor = achado.fim();
        }

        return resultado.append(texto.substring(cursor)).toString();
    }

    /** Quebra o texto nos pontos em que a expressão casa. */
    public List<String> dividir(String texto) {
        var pedacos = new ArrayList<String>();

        if (texto == null) {
            return pedacos;
        }

        var cursor = 0;

        for (var achado : encontrarTodos(texto)) {
            if (achado.vazia()) {
                continue;
            }

            pedacos.add(texto.substring(cursor, achado.inicio()));
            cursor = achado.fim();
        }

        pedacos.add(texto.substring(cursor));
        return pedacos;
    }

    @Override
    public String toString() {
        return "/" + expressao + "/ (" + automato.quantidadeDeEstados() + " estados)";
    }
}
