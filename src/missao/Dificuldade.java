package missao;

/**
 * Define os níveis de dificuldade do jogo.
 * Cada level vai ter sua própria forma de calcular os pontos
 * Assim como a quantidade de obstáculos: +hard +asteróides
 */
public enum Dificuldade {
    // Cria as opções e dita a regra: +hard +cacareco
    FACIL(30, 1),
    MEDIO(20, 2),
    DIFICIL(10, 4);

    private final int pontuacaoInicial; // Final pra não ser reatribuída
    private final int qtdAsteroides;

    // construtor privado pois só serve para aqui mesmo, na main não cria nada, só acessa
    // definirmos as opções acima.
    Dificuldade(int pontuacaoInicial, int qtdAsteroides) {
        this.pontuacaoInicial = pontuacaoInicial;
        this.qtdAsteroides = qtdAsteroides;
    }

    // Os metodos que serão usados lá na Main
    public int getPontuacaoInicial() {
        return pontuacaoInicial;
    }

    public int getQtdAsteroides() {
        return qtdAsteroides;
    }
}