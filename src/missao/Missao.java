package missao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Representa o estado da missão: nave, passageiros e asteroides.
 * Fornece operações para detectar colisões, localizar passageiros na posição
 * da nave e embarcar passageiros.
 */
public class Missao {
    private Nave nave;
    private List<Passageiro> passageiros = new ArrayList<>();
    private List<Asteroide> asteroides = new ArrayList<>();

    public Missao(Nave nave) {
        this.nave = nave;
    }

    public Nave getNave() {
        return nave;
    }

    public java.util.List<Passageiro> getPassageiros() {
        return passageiros;
    }

    public java.util.List<Asteroide> getAsteroides() {
        return asteroides;
    }

    /**
     * Adiciona um passageiro ao mapa da missão.
     *
     * @param p passageiro a adicionar
     */
    public void addPassageiro(Passageiro p) { passageiros.add(p); }

    /**
     * Adiciona um asteroide ao mapa da missão.
     *
     * @param a asteroide a adicionar
     */
    public void addAsteroide(Asteroide a) { asteroides.add(a); }

    public boolean verificaColisao() {
        // Percorre todos os asteroides e verifica se algum coincide com a
        // posição da nave. Uso de método em Asteroide encapsula a checagem.
        for (Asteroide a : asteroides) {
            if (a.colideCom(nave)) return true;
        }
        return false;
    }

    /**
     * Retorna o primeiro passageiro encontrado na mesma posição da nave,
     * ou `null` se não houver nenhum.
     *
     * @return `Passageiro` na posição da nave ou `null`
     */
    public Passageiro passagemNaPosicao() {
        // Retorna o primeiro passageiro encontrado na mesma posição da nave.
        for (Passageiro p : passageiros) {
            if (p.getX() == nave.getX() && p.getY() == nave.getY()) return p;
        }
        return null;
    }

    public boolean embarcarPassageiroNaPosicao() {
        // Itera usando Iterator para permitir remoção segura durante iteração.
        Iterator<Passageiro> it = passageiros.iterator();
        while (it.hasNext()) {
            Passageiro p = it.next();
            if (p.getX() == nave.getX() && p.getY() == nave.getY()) {
                // Tenta embarcar na nave; se bem-sucedido, remove do solo
                boolean ok = nave.embarcar(p);
                if (ok) it.remove();
                return ok;
            }
        }
        return false;
    }

    /**
     * Indica se todos os passageiros já foram embarcados.
     *
     * @return true se não restarem passageiros no solo
     */
    public boolean todosEmbarcados() { return passageiros.isEmpty(); }
}
