package br.com.conde.regex.sintaxe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.conde.regex.ErroDeRegex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnalisadorTest {

    @Test
    void literalSolto() {
        assertThat(Analisador.analisar("a")).isInstanceOf(No.Classe.class);
    }

    @Test
    void sequencia() {
        var no = Analisador.analisar("abc");

        assertThat(no).isInstanceOf(No.Sequencia.class);
        assertThat(((No.Sequencia) no).partes()).hasSize(3);
    }

    @Test
    void expressaoVaziaCasaComNada() {
        assertThat(Analisador.analisar("")).isInstanceOf(No.Vazio.class);
    }

    @Test
    @DisplayName("a alternância une menos que a sequência: ab|cd é (ab)|(cd)")
    void precedenciaDaAlternancia() {
        var no = Analisador.analisar("ab|cd");

        assertThat(no).isInstanceOf(No.Alternancia.class);
        assertThat(((No.Alternancia) no).esquerda()).isInstanceOf(No.Sequencia.class);
        assertThat(((No.Alternancia) no).direita()).isInstanceOf(No.Sequencia.class);
    }

    @Test
    @DisplayName("o quantificador pega só o átomo anterior")
    void quantificadorPegaSoOAtomo() {
        var no = Analisador.analisar("ab*");

        assertThat(no).isInstanceOf(No.Sequencia.class);
        assertThat(((No.Sequencia) no).partes().get(1)).isInstanceOf(No.Repeticao.class);
    }

    @Test
    void parentesesMudamOQueOQuantificadorPega() {
        assertThat(Analisador.analisar("(ab)*")).isInstanceOf(No.Repeticao.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a*", "a+", "a?", "a{2}", "a{2,}", "a{2,5}"})
    void todasAsFormasDeRepeticao(String expressao) {
        assertThat(Analisador.analisar(expressao)).isInstanceOf(No.Repeticao.class);
    }

    @Test
    void limitesDoContador() {
        var no = (No.Repeticao) Analisador.analisar("a{2,5}");

        assertThat(no.minimo()).isEqualTo(2);
        assertThat(no.maximo()).isEqualTo(5);
        assertThat(no.semTeto()).isFalse();
    }

    @Test
    void contadorSemTeto() {
        var no = (No.Repeticao) Analisador.analisar("a{3,}");

        assertThat(no.minimo()).isEqualTo(3);
        assertThat(no.semTeto()).isTrue();
    }

    @Test
    @DisplayName("chave que não forma contador é caractere comum")
    void chaveSolta() {
        assertThat(Analisador.analisar("a{x}")).isInstanceOf(No.Sequencia.class);
    }

    @Test
    void grupoNaoCapturante() {
        assertThat(Analisador.analisar("(?:ab)+")).isInstanceOf(No.Repeticao.class);
    }

    @Test
    void ancoras() {
        assertThat(Analisador.analisar("^")).isEqualTo(new No.Ancora(No.Ancora.Tipo.INICIO));
        assertThat(Analisador.analisar("$")).isEqualTo(new No.Ancora(No.Ancora.Tipo.FIM));
        assertThat(Analisador.analisar("\\b")).isEqualTo(new No.Ancora(No.Ancora.Tipo.BORDA_DE_PALAVRA));
    }

    @Test
    void classeDeCaracteres() {
        var no = (No.Classe) Analisador.analisar("[a-z0-9_]");

        assertThat(no.classe().aceita('m')).isTrue();
        assertThat(no.classe().aceita('4')).isTrue();
        assertThat(no.classe().aceita('-')).isFalse();
    }

    @Test
    void classeNegada() {
        var no = (No.Classe) Analisador.analisar("[^abc]");

        assertThat(no.classe().aceita('a')).isFalse();
        assertThat(no.classe().aceita('z')).isTrue();
    }

    @Test
    @DisplayName("o traço no fim da classe é literal")
    void tracoLiteralNaClasse() {
        var no = (No.Classe) Analisador.analisar("[a-]");

        assertThat(no.classe().aceita('a')).isTrue();
        assertThat(no.classe().aceita('-')).isTrue();
    }

    @Test
    void escapeDeMetacaractere() {
        var no = (No.Classe) Analisador.analisar("\\.");

        assertThat(no.classe().aceita('.')).isTrue();
        assertThat(no.classe().aceita('x')).isFalse();
    }

    @Test
    void atalhosDeClasse() {
        assertThat(((No.Classe) Analisador.analisar("\\d")).classe().aceita('7')).isTrue();
        assertThat(((No.Classe) Analisador.analisar("\\D")).classe().aceita('7')).isFalse();
        assertThat(((No.Classe) Analisador.analisar("\\w")).classe().aceita('_')).isTrue();
        assertThat(((No.Classe) Analisador.analisar("\\s")).classe().aceita(' ')).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"(", ")", "[a", "*a", "+", "a{2,1}", "\\", "[]"})
    void expressaoInvalidaReclama(String expressao) {
        assertThatThrownBy(() -> Analisador.analisar(expressao)).isInstanceOf(ErroDeRegex.class);
    }

    @Test
    @DisplayName("o erro aponta a posição do problema")
    void erroApontaAPosicao() {
        assertThatThrownBy(() -> Analisador.analisar("ab(cd"))
                .isInstanceOf(ErroDeRegex.class)
                .satisfies(erro -> assertThat(((ErroDeRegex) erro).posicao()).isEqualTo(5));
    }

    @Test
    void aArvoreSeDescreveDeVolta() {
        assertThat(Analisador.analisar("a+").toString()).isEqualTo("[a]+");
    }
}
