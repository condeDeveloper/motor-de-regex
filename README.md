# motor-de-regex

Motor de expressões regulares escrito do zero em Java 21. Parser próprio,
construção de Thompson e simulação por conjunto de estados — sem retrocesso, e
portanto **imune ao ReDoS**.

```java
var regex = Regex.compilar("\\d{2}/\\d{2}/\\d{4}");

regex.casa("21/09/2026");                        // true
regex.encontrar("entregue em 21/09/2026, ok");   // '21/09/2026' em [12, 22)
regex.substituir("de 01/01/2026 a 31/12/2026", "…");
```

## Por que existe

O detalhe que quase ninguém conhece: a implementação de regex de Java, Python,
JavaScript, .NET e PCRE usa **retrocesso**. Isso dá recursos poderosos —
retrovisores, lookahead — ao custo de um comportamento exponencial em casos que
parecem inofensivos:

```java
Pattern.matches("(a+)+b", "a".repeat(30));   // java.util.regex: trava
Regex.compilar("(a+)+b").casa("a".repeat(30));  // este motor: instantâneo
```

Esse é o **ReDoS**, e já derrubou Stack Overflow e Cloudflare em produção. A
causa é o motor tentar todas as maneiras de repartir os `a` entre os dois
quantificadores. A alternativa, descrita por Ken Thompson em 1968, é caminhar
por **todos os estados possíveis ao mesmo tempo**: cada caractere do texto é
lido uma vez só, e o custo é o tamanho do texto vezes o número de estados.

O teste `semExplosaoExponencial` roda `(a+)+b` contra 2000 caracteres e exige
que termine em menos de dois segundos. Na prática leva milissegundos.

## Como funciona

```
expressão  →  Analisador  →  árvore  →  Construtor  →  autômato  →  simulação
```

1. **Analisador** — descida recursiva em três níveis: alternância, sequência e
   repetição. É essa ordem que faz `ab|cd` ser `(ab)|(cd)` e não `a(b|c)d`.
2. **Construtor** — cada operador tem um desenho fixo de estados. A alternância
   é uma bifurcação, a estrela é uma bifurcação que volta para si mesma, e a
   concatenação é ligar um pedaço no outro. Como nenhum desenho precisa saber o
   que veio antes ou depois, a montagem é toda de baixo para cima.
3. **Simulação** — o conjunto de estados ativos avança um caractere por vez. Um
   vetor de visitados impede que `(a*)*` gire para sempre nas transições vazias.

## O que a sintaxe aceita

| Escreva | Significa |
| --- | --- |
| `abc` | os caracteres, em sequência |
| `.` | qualquer coisa menos quebra de linha |
| `a\|b` | uma alternativa ou a outra |
| `(ab)` `(?:ab)` | agrupamento |
| `a*` `a+` `a?` | zero ou mais, uma ou mais, zero ou uma |
| `a{3}` `a{2,}` `a{2,5}` | contadores |
| `[a-z0-9_]` `[^abc]` | classes de caracteres |
| `\d \w \s` e `\D \W \S` | atalhos e seus opostos |
| `^` `$` | começo e fim do texto |
| `\b` `\B` | borda e não-borda de palavra |
| `\.` `\*` `\\` | escape de metacaractere |

Uma chave que não forma contador (`a{x}`) é tratada como caractere comum, que é
o que a maioria dos motores faz.

## API

```java
Regex regex = Regex.compilar("\\d+");

regex.casa("123");                  // o texto inteiro casa?
regex.contemEm("pedido 4471");      // aparece em algum lugar?
regex.encontrar("a 1 b 22");        // Optional<Correspondencia> — o primeiro
regex.encontrar("a 1 b 22", 3);     // a partir de uma posição
regex.encontrarTodos("a 1 b 22");   // todos, sem sobreposição
regex.substituir("a 1 b 22", "#");  // "a # b #"
regex.dividir("a1b22c");            // ["a", "b", "c"]
```

A busca é **leftmost-longest**: entre duas correspondências que começam no mesmo
ponto vale a mais longa; entre pontos diferentes, vale a mais à esquerda.

## Estrutura

```
sintaxe/ClasseDeCaracteres.java  conjuntos de caracteres, guardados por faixa
sintaxe/No.java                  a árvore (sealed interface + records)
sintaxe/Analisador.java          expressão → árvore
automato/Estado.java             quatro tipos: caractere, divisão, âncora, aceitação
automato/Fragmento.java          pedaço com saídas penduradas
automato/Construtor.java         árvore → autômato (Thompson)
automato/Automato.java           a simulação
Regex.java                       a fachada
```

## Rodando

```bash
mvn test
```

107 testes, nenhuma dependência de runtime — só JUnit e AssertJ para testar.

## Limites conhecidos

- **Não captura grupos.** Parênteses agrupam para efeito de precedência e
  quantificador, mas não há `grupo(1)`. Capturar exigiria um autômato com tags,
  e a simplicidade da simulação é justamente o ponto do projeto.
- Sem lookahead, lookbehind e retrovisores — todos dependem de retrocesso, que é
  exatamente o que não existe aqui.
- Sem quantificador preguiçoso (`a*?`): sem retrocesso, a busca é sempre a mais
  longa.
- Sem flags de caixa ou multilinha.
- `{n,m}` é expandido em cópias do corpo, então `a{1000}` gera mil trechos de
  autômato.

## Licença

MIT.
