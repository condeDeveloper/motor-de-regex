package br.com.conde.regex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CorrespondenciaTest {

    @Test
    void guardaOTrechoEAPosicao() {
        var achado = new Correspondencia("abc", 2, 5);

        assertThat(achado.texto()).isEqualTo("abc");
        assertThat(achado.inicio()).isEqualTo(2);
        assertThat(achado.fim()).isEqualTo(5);
        assertThat(achado.tamanho()).isEqualTo(3);
        assertThat(achado.vazia()).isFalse();
    }

    @Test
    void reconheceOTrechoVazio() {
        assertThat(new Correspondencia("", 3, 3).vazia()).isTrue();
    }

    @Test
    void intervaloInvalidoReclama() {
        assertThatThrownBy(() -> new Correspondencia("a", 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Correspondencia("a", -1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void seDescreveDeVolta() {
        assertThat(new Correspondencia("oi", 0, 2)).hasToString("'oi' em [0, 2)");
    }
}
