package br.com.conde.regex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RegexTest {

    @Nested
    @DisplayName("casamento do texto inteiro")
    class Casamento {

        @ParameterizedTest
        @CsvSource({
            "abc, abc, true",
            "abc, abd, false",
            "a.c, abc, true",
            "a.c, a c, true",
            "a.c, ac, false",
            "a*, '', true",
            "a*, aaaa, true",
            "a+, '', false",
            "a+, aaaa, true",
            "a?, '', true",
            "a?, a, true",
            "a?, aa, false",
            "a|b, a, true",
            "a|b, b, true",
            "a|b, c, false",
            "(ab)+, ababab, true",
            "(ab)+, aba, false",
        })
        void casos(String expressao, String texto, boolean esperado) {
            assertThat(Regex.compilar(expressao).casa(texto)).isEqualTo(esperado);
        }

        @Test
        void textoNuloNaoCasa() {
            assertThat(Regex.compilar("a*").casa(null)).isFalse();
        }

        @Test
        @DisplayName("o contador exige a quantidade exata")
        void contador() {
            var regex = Regex.compilar("a{3}");

            assertThat(regex.casa("aaa")).isTrue();
            assertThat(regex.casa("aa")).isFalse();
            assertThat(regex.casa("aaaa")).isFalse();
        }

        @Test
        void contadorComFaixa() {
            var regex = Regex.compilar("a{2,4}");

            assertThat(regex.casa("a")).isFalse();
            assertThat(regex.casa("aa")).isTrue();
            assertThat(regex.casa("aaaa")).isTrue();
            assertThat(regex.casa("aaaaa")).isFalse();
        }

        @Test
        void contadorSemTeto() {
            var regex = Regex.compilar("a{2,}");

            assertThat(regex.casa("a")).isFalse();
            assertThat(regex.casa("aaaaaaaa")).isTrue();
        }

        @Test
        void classeDeCaracteres() {
            var regex = Regex.compilar("[a-c]+");

            assertThat(regex.casa("abcba")).isTrue();
            assertThat(regex.casa("abd")).isFalse();
        }

        @Test
        void classeNegada() {
            assertThat(Regex.compilar("[^0-9]+").casa("abc")).isTrue();
            assertThat(Regex.compilar("[^0-9]+").casa("ab1")).isFalse();
        }

        @Test
        @DisplayName("alternância aninhada com repetição")
        void combinacao() {
            var regex = Regex.compilar("(gato|cachorro)s?");

            assertThat(regex.casa("gato")).isTrue();
            assertThat(regex.casa("gatos")).isTrue();
            assertThat(regex.casa("cachorros")).isTrue();
            assertThat(regex.casa("gatoss")).isFalse();
        }
    }

    @Nested
    @DisplayName("âncoras")
    class Ancoras {

        @Test
        void inicioEFim() {
            assertThat(Regex.compilar("^abc$").casa("abc")).isTrue();
            assertThat(Regex.compilar("^a").contemEm("abc")).isTrue();
            assertThat(Regex.compilar("^b").contemEm("abc")).isFalse();
            assertThat(Regex.compilar("c$").contemEm("abc")).isTrue();
            assertThat(Regex.compilar("b$").contemEm("abc")).isFalse();
        }

        @Test
        @DisplayName("a borda de palavra separa palavra de pontuação")
        void bordaDePalavra() {
            var regex = Regex.compilar("\\bgato\\b");

            assertThat(regex.contemEm("o gato subiu")).isTrue();
            assertThat(regex.contemEm("o gato, subiu")).isTrue();
            assertThat(regex.contemEm("o gatorra subiu")).isFalse();
        }

        @Test
        void semBordaDePalavra() {
            assertThat(Regex.compilar("\\Bato").contemEm("gato")).isTrue();
            assertThat(Regex.compilar("\\Bato").contemEm("ato")).isFalse();
        }
    }

    @Nested
    @DisplayName("busca no texto")
    class Busca {

        @Test
        void encontraOPrimeiroTrecho() {
            var achado = Regex.compilar("\\d+").encontrar("pedido 4471 de 2026");

            assertThat(achado).isPresent();
            assertThat(achado.get().texto()).isEqualTo("4471");
            assertThat(achado.get().inicio()).isEqualTo(7);
            assertThat(achado.get().fim()).isEqualTo(11);
        }

        @Test
        @DisplayName("entre duas que começam junto, vale a mais longa")
        void maisLonga() {
            assertThat(Regex.compilar("a+").encontrar("aaa").orElseThrow().texto()).isEqualTo("aaa");
        }

        @Test
        void naoEncontraNada() {
            assertThat(Regex.compilar("\\d").encontrar("sem número")).isEmpty();
        }

        @Test
        void encontraAPartirDeUmaPosicao() {
            var achado = Regex.compilar("\\d+").encontrar("1 e 22 e 333", 3);

            assertThat(achado.orElseThrow().texto()).isEqualTo("22");
        }

        @Test
        void encontraTodos() {
            var achados = Regex.compilar("\\d+").encontrarTodos("1 e 22 e 333");

            assertThat(achados).extracting(Correspondencia::texto).containsExactly("1", "22", "333");
        }

        @Test
        @DisplayName("correspondência vazia não trava o laço")
        void correspondenciaVaziaNaoTrava() {
            var achados = Regex.compilar("a*").encontrarTodos("bab");

            assertThat(achados).isNotEmpty();
            assertThat(achados).anyMatch(achado -> achado.texto().equals("a"));
        }

        @Test
        void textoNuloDevolveListaVazia() {
            assertThat(Regex.compilar("a").encontrarTodos(null)).isEmpty();
        }

        @Test
        void substitui() {
            assertThat(Regex.compilar("\\d+").substituir("pedido 4471 de 2026", "#"))
                    .isEqualTo("pedido # de #");
        }

        @Test
        void substituirSemAcharDevolveOMesmoTexto() {
            assertThat(Regex.compilar("z+").substituir("abc", "#")).isEqualTo("abc");
        }

        @Test
        void divide() {
            assertThat(Regex.compilar("\\s*,\\s*").dividir("a, b ,c"))
                    .containsExactly("a", "b", "c");
        }

        @Test
        void dividirSemSeparadorDevolveOTextoInteiro() {
            assertThat(Regex.compilar(";").dividir("abc")).containsExactly("abc");
        }
    }

    @Nested
    @DisplayName("expressões do mundo real")
    class Praticas {

        @ParameterizedTest
        @ValueSource(strings = {"21/09/2026", "01/01/2000"})
        void data(String texto) {
            assertThat(Regex.compilar("\\d{2}/\\d{2}/\\d{4}").casa(texto)).isTrue();
        }

        @Test
        void dataMalFormada() {
            assertThat(Regex.compilar("\\d{2}/\\d{2}/\\d{4}").casa("1/9/26")).isFalse();
        }

        @Test
        void cep() {
            var regex = Regex.compilar("\\d{5}-?\\d{3}");

            assertThat(regex.casa("01310100")).isTrue();
            assertThat(regex.casa("01310-100")).isTrue();
            assertThat(regex.casa("0131-0100")).isFalse();
        }

        @Test
        void cnpj() {
            var regex = Regex.compilar("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");

            assertThat(regex.casa("11.222.333/0001-81")).isTrue();
            assertThat(regex.casa("11222333000181")).isFalse();
        }

        @Test
        void emailSimples() {
            var regex = Regex.compilar("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

            assertThat(regex.casa("maria.souza+nota@exemplo.com.br")).isTrue();
            assertThat(regex.casa("maria@exemplo")).isFalse();
        }

        @Test
        void extraiValoresDeUmaLinha() {
            var achados = Regex.compilar("R\\$ ?\\d+,\\d{2}")
                    .encontrarTodos("total R$ 19,90 e frete R$8,50");

            assertThat(achados).extracting(Correspondencia::texto)
                    .containsExactly("R$ 19,90", "R$8,50");
        }
    }

    @Test
    void oRegexSeDescreveDeVolta() {
        assertThat(Regex.compilar("a+").toString()).contains("/a+/").contains("estados");
    }

    @Test
    void exponhaAArvoreEOAutomato() {
        var regex = Regex.compilar("ab");

        assertThat(regex.expressao()).isEqualTo("ab");
        assertThat(regex.arvore()).isNotNull();
        assertThat(regex.automato().quantidadeDeEstados()).isPositive();
    }
}
