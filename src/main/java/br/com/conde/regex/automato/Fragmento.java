package br.com.conde.regex.automato;

import java.util.ArrayList;
import java.util.List;

/**
 * Um pedaço de autômato ainda solto: tem por onde entrar, mas as saídas estão
 * penduradas, esperando alguém dizer para onde vão.
 *
 * <p>É o truque que faz a construção de Thompson funcionar de baixo para cima —
 * cada pedaço é montado sem saber o que vem depois dele.
 */
final class Fragmento {

    /** Uma saída pendurada: o campo de um estado que ainda não foi preenchido. */
    record Saida(Estado estado, boolean peloAlternativo) {
        void ligar(Estado destino) {
            if (peloAlternativo) {
                estado.alternativo = destino;
            } else {
                estado.proximo = destino;
            }
        }
    }

    private final Estado entrada;
    private final List<Saida> saidas;

    Fragmento(Estado entrada, List<Saida> saidas) {
        this.entrada = entrada;
        this.saidas = new ArrayList<>(saidas);
    }

    Estado entrada() {
        return entrada;
    }

    List<Saida> saidas() {
        return saidas;
    }

    /** Liga todas as saídas soltas a um destino. */
    void ligar(Estado destino) {
        for (var saida : saidas) {
            saida.ligar(destino);
        }
    }

    /** Liga este pedaço ao seguinte e devolve os dois como um só. */
    Fragmento seguidoDe(Fragmento outro) {
        ligar(outro.entrada);
        return new Fragmento(entrada, outro.saidas);
    }
}
