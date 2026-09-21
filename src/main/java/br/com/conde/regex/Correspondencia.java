package br.com.conde.regex;

/**
 * Um trecho encontrado no texto.
 *
 * @param texto o trecho em si
 * @param inicio onde ele começa no texto original
 * @param fim posição logo depois do trecho
 */
public record Correspondencia(String texto, int inicio, int fim) {

    public Correspondencia {
        if (inicio < 0 || fim < inicio) {
            throw new IllegalArgumentException("Intervalo inválido: " + inicio + ".." + fim);
        }
    }

    /** Quantos caracteres o trecho ocupa. */
    public int tamanho() {
        return fim - inicio;
    }

    /** Indica se o trecho encontrado é vazio. */
    public boolean vazia() {
        return inicio == fim;
    }

    @Override
    public String toString() {
        return "'" + texto + "' em [" + inicio + ", " + fim + ")";
    }
}
