package br.com.conde.regex.automato;

import br.com.conde.regex.sintaxe.No;
import java.util.ArrayList;
import java.util.List;

/**
 * O autômato pronto, e a simulação dele.
 *
 * <p>Em vez de tentar um caminho e voltar atrás quando dá errado, a simulação
 * anda com <em>todos</em> os caminhos possíveis ao mesmo tempo, como um
 * conjunto de estados. Cada caractere do texto é lido uma única vez, e o custo
 * é o tamanho do texto vezes o número de estados — nunca exponencial. É por
 * isso que {@code (a+)+b}, que trava motores com retrocesso, aqui não faz
 * cócegas.
 */
public final class Automato {

    private final Estado inicio;
    private final int quantidadeDeEstados;

    Automato(Estado inicio, int quantidadeDeEstados) {
        this.inicio = inicio;
        this.quantidadeDeEstados = quantidadeDeEstados;
    }

    /** Quantos estados o autômato tem. */
    public int quantidadeDeEstados() {
        return quantidadeDeEstados;
    }

    /** O estado inicial. */
    public Estado inicio() {
        return inicio;
    }

    /** Indica se a expressão casa com o texto inteiro. */
    public boolean casaTudo(String texto) {
        var atuais = partirDe(inicio, 0, texto);

        for (var posicao = 0; posicao < texto.length(); posicao++) {
            atuais = avancar(atuais, texto, posicao);

            if (atuais.isEmpty()) {
                return false;
            }
        }

        return aceita(atuais);
    }

    /**
     * Procura a partir de {@code desde} e devolve onde termina a maior
     * correspondência que começa exatamente ali, ou -1 se não houver nenhuma.
     */
    public int fimDaCorrespondencia(String texto, int desde) {
        var atuais = partirDe(inicio, desde, texto);
        var ultimoFim = aceita(atuais) ? desde : -1;

        for (var posicao = desde; posicao < texto.length(); posicao++) {
            atuais = avancar(atuais, texto, posicao);

            if (atuais.isEmpty()) {
                break;
            }

            if (aceita(atuais)) {
                ultimoFim = posicao + 1;
            }
        }

        return ultimoFim;
    }

    private List<Estado> partirDe(Estado estado, int posicao, String texto) {
        var conjunto = new ArrayList<Estado>();
        expandir(estado, posicao, texto, conjunto, new boolean[quantidadeDeEstados]);
        return conjunto;
    }

    /**
     * Segue todas as bifurcações e âncoras sem consumir caractere, juntando os
     * estados que de fato esperam por um caractere. O vetor de visitados é o
     * que impede que um laço como {@code (a*)*} rode para sempre.
     */
    private void expandir(Estado estado, int posicao, String texto, List<Estado> destino, boolean[] visitados) {
        if (estado == null || visitados[estado.id()]) {
            return;
        }

        visitados[estado.id()] = true;

        switch (estado.tipo()) {
            case DIVISAO -> {
                expandir(estado.proximo(), posicao, texto, destino, visitados);
                expandir(estado.alternativo(), posicao, texto, destino, visitados);
            }
            case ANCORA -> {
                if (ancoraVale(estado.ancora(), posicao, texto)) {
                    expandir(estado.proximo(), posicao, texto, destino, visitados);
                }
            }
            default -> destino.add(estado);
        }
    }

    private List<Estado> avancar(List<Estado> atuais, String texto, int posicao) {
        var proximos = new ArrayList<Estado>();
        var visitados = new boolean[quantidadeDeEstados];
        var c = texto.charAt(posicao);

        for (var estado : atuais) {
            if (estado.tipo() == Estado.Tipo.CARACTERE && estado.classe().aceita(c)) {
                expandir(estado.proximo(), posicao + 1, texto, proximos, visitados);
            }
        }

        return proximos;
    }

    private static boolean aceita(List<Estado> estados) {
        for (var estado : estados) {
            if (estado.tipo() == Estado.Tipo.ACEITACAO) {
                return true;
            }
        }
        return false;
    }

    private static boolean ancoraVale(No.Ancora.Tipo tipo, int posicao, String texto) {
        return switch (tipo) {
            case INICIO -> posicao == 0;
            case FIM -> posicao == texto.length();
            case BORDA_DE_PALAVRA -> ehBorda(posicao, texto);
            case SEM_BORDA_DE_PALAVRA -> !ehBorda(posicao, texto);
        };
    }

    private static boolean ehBorda(int posicao, String texto) {
        var antes = posicao > 0 && ehDePalavra(texto.charAt(posicao - 1));
        var depois = posicao < texto.length() && ehDePalavra(texto.charAt(posicao));
        return antes != depois;
    }

    private static boolean ehDePalavra(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
