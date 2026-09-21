package br.com.conde.regex.automato;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.conde.regex.Regex;
import br.com.conde.regex.sintaxe.Analisador;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AutomatoTest {

    private static Automato de(String expressao) {
        return Construtor.construir(Analisador.analisar(expressao));
    }

    @Test
    @DisplayName("o número de estados cresce com o tamanho da expressão, não com o do texto")
    void quantidadeDeEstados() {
        assertThat(de("a").quantidadeDeEstados()).isPositive();
        assertThat(de("abc").quantidadeDeEstados()).isGreaterThan(de("a").quantidadeDeEstados());
    }

    @Test
    void oInicioExiste() {
        assertThat(de("a").inicio()).isNotNull();
    }

    @Test
    void casaTudoConfereOTextoInteiro() {
        assertThat(de("ab").casaTudo("ab")).isTrue();
        assertThat(de("ab").casaTudo("abc")).isFalse();
    }

    @Test
    @DisplayName("o fim da correspondência é o da maior que começa naquele ponto")
    void fimDaCorrespondencia() {
        assertThat(de("a+").fimDaCorrespondencia("aaab", 0)).isEqualTo(3);
        assertThat(de("a+").fimDaCorrespondencia("baaa", 0)).isEqualTo(-1);
        assertThat(de("a+").fimDaCorrespondencia("baaa", 1)).isEqualTo(4);
    }

    @Test
    @DisplayName("correspondência vazia devolve a própria posição")
    void correspondenciaVazia() {
        assertThat(de("a*").fimDaCorrespondencia("bbb", 0)).isZero();
    }

    @Test
    @DisplayName("laço de repetição vazia não roda para sempre")
    @Timeout(5)
    void repeticaoVaziaNaoTrava() {
        assertThat(de("(a*)*").casaTudo("aaa")).isTrue();
        assertThat(de("(a*)*").casaTudo("b")).isFalse();
    }

    @Test
    @DisplayName("o caso que trava motor com retrocesso passa aqui sem esforço")
    @Timeout(5)
    void semExplosaoExponencial() {
        // (a+)+b contra uma sequência de "a" sem o "b" no fim é o exemplo
        // clássico de ReDoS: um motor com retrocesso tenta todas as maneiras de
        // repartir os "a" entre os dois quantificadores, o que é exponencial.
        // A simulação por conjunto de estados lê cada caractere uma vez só.
        var regex = Regex.compilar("(a+)+b");
        var texto = "a".repeat(2000);

        var inicio = System.nanoTime();
        var casou = regex.casa(texto);
        var gasto = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(casou).isFalse();
        assertThat(gasto).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("alternância aninhada também não explode")
    @Timeout(5)
    void alternanciaAninhadaNaoExplode() {
        var regex = Regex.compilar("(a|aa)+c");

        assertThat(regex.casa("a".repeat(1000))).isFalse();
    }

    @Test
    @DisplayName("o tempo cresce de forma linear com o texto")
    @Timeout(20)
    void tempoLinear() {
        var regex = Regex.compilar("(a+)+b");

        var curto = medir(regex, 1000);
        var longo = medir(regex, 4000);

        // Quatro vezes o texto não pode custar cem vezes o tempo. A folga é
        // larga de propósito: o teste existe para pegar explosão exponencial,
        // não para medir desempenho com precisão.
        assertThat(longo).isLessThan(Math.max(curto, 1) * 100);
    }

    private static long medir(Regex regex, int tamanho) {
        var texto = "a".repeat(tamanho);
        var inicio = System.nanoTime();
        regex.casa(texto);
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}
