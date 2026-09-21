package br.com.conde.regex.automato;

import br.com.conde.regex.sintaxe.No;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o autômato a partir da árvore, pela construção de Thompson.
 *
 * <p>A ideia é que cada operador da expressão tem um desenho fixo de estados:
 * a alternância vira uma bifurcação, a estrela vira uma bifurcação que volta
 * para si mesma, e a concatenação é só ligar um pedaço no outro. Como nenhum
 * desses desenhos precisa saber o que veio antes ou depois, a montagem é
 * puramente de baixo para cima.
 */
public final class Construtor {

    private int proximoId;

    private Construtor() {
    }

    /** Constrói o autômato de uma árvore já analisada. */
    public static Automato construir(No raiz) {
        var construtor = new Construtor();
        var fragmento = construtor.montar(raiz);
        var fim = construtor.novo(Estado.Tipo.ACEITACAO, null, null);

        fragmento.ligar(fim);

        return new Automato(fragmento.entrada(), construtor.proximoId);
    }

    private Fragmento montar(No no) {
        return switch (no) {
            case No.Vazio ignorado -> vazio();
            case No.Classe classe -> caractere(classe);
            case No.Ancora ancora -> ancora(ancora);
            case No.Sequencia sequencia -> sequencia(sequencia);
            case No.Alternancia alternancia -> alternancia(alternancia);
            case No.Repeticao repeticao -> repeticao(repeticao);
        };
    }

    /**
     * O vazio não pode ser um fragmento sem estados, senão não há onde
     * pendurar a saída. Uma divisão com os dois caminhos soltos resolve: ela
     * não consome nada e deixa duas pontas para ligar.
     */
    private Fragmento vazio() {
        var estado = novo(Estado.Tipo.DIVISAO, null, null);
        return new Fragmento(estado, List.of(
                new Fragmento.Saida(estado, false),
                new Fragmento.Saida(estado, true)));
    }

    private Fragmento caractere(No.Classe no) {
        var estado = novo(Estado.Tipo.CARACTERE, no.classe(), null);
        return new Fragmento(estado, List.of(new Fragmento.Saida(estado, false)));
    }

    private Fragmento ancora(No.Ancora no) {
        var estado = novo(Estado.Tipo.ANCORA, null, no.tipo());
        return new Fragmento(estado, List.of(new Fragmento.Saida(estado, false)));
    }

    private Fragmento sequencia(No.Sequencia no) {
        Fragmento acumulado = null;

        for (var parte : no.partes()) {
            var atual = montar(parte);
            acumulado = (acumulado == null) ? atual : acumulado.seguidoDe(atual);
        }

        return acumulado == null ? vazio() : acumulado;
    }

    private Fragmento alternancia(No.Alternancia no) {
        var divisao = novo(Estado.Tipo.DIVISAO, null, null);

        var esquerda = montar(no.esquerda());
        var direita = montar(no.direita());

        divisao.proximo = esquerda.entrada();
        divisao.alternativo = direita.entrada();

        var saidas = new ArrayList<>(esquerda.saidas());
        saidas.addAll(direita.saidas());

        return new Fragmento(divisao, saidas);
    }

    private Fragmento repeticao(No.Repeticao no) {
        if (no.minimo() == 0 && no.semTeto()) {
            return estrela(no.corpo());
        }

        if (no.minimo() == 1 && no.semTeto()) {
            return uma(no.corpo()).seguidoDe(estrela(no.corpo()));
        }

        if (no.minimo() == 0 && no.maximo() == 1) {
            return opcional(no.corpo());
        }

        if (no.minimo() == 0 && no.maximo() == 0) {
            return vazio();
        }

        // {n,m} vira n cópias obrigatórias seguidas de (m-n) opcionais, ou de
        // uma estrela quando não há teto. Duplicar o corpo custa estados, mas
        // mantém o resto do motor sem nenhum caso especial de contagem.
        Fragmento acumulado = null;

        for (var vez = 0; vez < no.minimo(); vez++) {
            var copia = uma(no.corpo());
            acumulado = (acumulado == null) ? copia : acumulado.seguidoDe(copia);
        }

        if (no.semTeto()) {
            var cauda = estrela(no.corpo());
            return acumulado == null ? cauda : acumulado.seguidoDe(cauda);
        }

        for (var vez = no.minimo(); vez < no.maximo(); vez++) {
            var copia = opcional(no.corpo());
            acumulado = (acumulado == null) ? copia : acumulado.seguidoDe(copia);
        }

        return acumulado == null ? vazio() : acumulado;
    }

    private Fragmento uma(No corpo) {
        return montar(corpo);
    }

    private Fragmento estrela(No corpo) {
        var divisao = novo(Estado.Tipo.DIVISAO, null, null);
        var dentro = montar(corpo);

        divisao.proximo = dentro.entrada();
        dentro.ligar(divisao);

        return new Fragmento(divisao, List.of(new Fragmento.Saida(divisao, true)));
    }

    private Fragmento opcional(No corpo) {
        var divisao = novo(Estado.Tipo.DIVISAO, null, null);
        var dentro = montar(corpo);

        divisao.proximo = dentro.entrada();

        var saidas = new ArrayList<>(dentro.saidas());
        saidas.add(new Fragmento.Saida(divisao, true));

        return new Fragmento(divisao, saidas);
    }

    private Estado novo(Estado.Tipo tipo, br.com.conde.regex.sintaxe.ClasseDeCaracteres classe, No.Ancora.Tipo ancora) {
        var id = proximoId++;

        return switch (tipo) {
            case CARACTERE -> Estado.caractere(id, classe);
            case DIVISAO -> Estado.divisao(id);
            case ANCORA -> Estado.ancora(id, ancora);
            case ACEITACAO -> Estado.aceitacao(id);
        };
    }
}
