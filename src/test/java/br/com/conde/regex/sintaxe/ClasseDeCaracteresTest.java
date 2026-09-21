package br.com.conde.regex.sintaxe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClasseDeCaracteresTest {

    @Test
    @DisplayName("um caractere aceita só ele mesmo")
    void umCaractere() {
        var classe = ClasseDeCaracteres.de('a');

        assertThat(classe.aceita('a')).isTrue();
        assertThat(classe.aceita('b')).isFalse();
    }

    @Test
    @DisplayName("a faixa inclui as duas pontas")
    void faixa() {
        var classe = ClasseDeCaracteres.faixa('c', 'e');

        assertThat(classe.aceita('c')).isTrue();
        assertThat(classe.aceita('d')).isTrue();
        assertThat(classe.aceita('e')).isTrue();
        assertThat(classe.aceita('b')).isFalse();
        assertThat(classe.aceita('f')).isFalse();
    }

    @Test
    @DisplayName("o ponto aceita tudo menos a quebra de linha")
    void qualquer() {
        var classe = ClasseDeCaracteres.qualquer();

        assertThat(classe.aceita('a')).isTrue();
        assertThat(classe.aceita(' ')).isTrue();
        assertThat(classe.aceita('\n')).isFalse();
    }

    @ParameterizedTest
    @ValueSource(chars = {'0', '5', '9'})
    void digitosAceitos(char c) {
        assertThat(ClasseDeCaracteres.digitos().aceita(c)).isTrue();
    }

    @Test
    void digitosRecusamLetra() {
        assertThat(ClasseDeCaracteres.digitos().aceita('a')).isFalse();
    }

    @Test
    @DisplayName("a classe de palavra pega letra, número e sublinhado")
    void palavra() {
        var classe = ClasseDeCaracteres.palavra();

        assertThat(classe.aceita('a')).isTrue();
        assertThat(classe.aceita('Z')).isTrue();
        assertThat(classe.aceita('7')).isTrue();
        assertThat(classe.aceita('_')).isTrue();
        assertThat(classe.aceita('-')).isFalse();
    }

    @Test
    void espacos() {
        assertThat(ClasseDeCaracteres.espacos().aceita(' ')).isTrue();
        assertThat(ClasseDeCaracteres.espacos().aceita('\t')).isTrue();
        assertThat(ClasseDeCaracteres.espacos().aceita('x')).isFalse();
    }

    @Test
    @DisplayName("negar inverte o sentido")
    void negar() {
        var classe = ClasseDeCaracteres.digitos().negar();

        assertThat(classe.negada()).isTrue();
        assertThat(classe.aceita('5')).isFalse();
        assertThat(classe.aceita('a')).isTrue();
    }

    @Test
    void negarDuasVezesVoltaAoOriginal() {
        assertThat(ClasseDeCaracteres.digitos().negar().negar().aceita('5')).isTrue();
    }

    @Test
    @DisplayName("a união junta as faixas")
    void uniao() {
        var classe = ClasseDeCaracteres.uniao(
                List.of(ClasseDeCaracteres.faixa('a', 'c'), ClasseDeCaracteres.digitos()), false);

        assertThat(classe.aceita('b')).isTrue();
        assertThat(classe.aceita('7')).isTrue();
        assertThat(classe.aceita('z')).isFalse();
    }

    @Test
    void uniaoNegada() {
        var classe = ClasseDeCaracteres.uniao(List.of(ClasseDeCaracteres.faixa('a', 'c')), true);

        assertThat(classe.aceita('b')).isFalse();
        assertThat(classe.aceita('z')).isTrue();
    }

    @Test
    @DisplayName("classe negada dentro de outra é recusada em vez de errar calada")
    void uniaoComNegadaDentro() {
        var partes = List.of(ClasseDeCaracteres.digitos().negar());

        assertThatThrownBy(() -> ClasseDeCaracteres.uniao(partes, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void seDescreveDeVolta() {
        assertThat(ClasseDeCaracteres.faixa('a', 'z')).hasToString("[a-z]");
        assertThat(ClasseDeCaracteres.de('x')).hasToString("[x]");
        assertThat(ClasseDeCaracteres.digitos().negar()).hasToString("[^0-9]");
    }
}
