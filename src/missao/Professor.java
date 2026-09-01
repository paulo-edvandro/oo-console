package missao;

/**
 * Passageiro especializado: Professor. Atualmente é uma subclasse leve de
 * `Passageiro` que apenas define o tipo.
 */
public class Professor extends Passageiro {
    public Professor(String nome, int x, int y) {
        super(nome, "Professor", x, y);
    }

    @Override
    public int getPontuacao() {
        return 10;
    }
}
