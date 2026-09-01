
package missao;

import java.util.Random;

public class Inimigo {

  private int x;
  private int y;

  public Inimigo(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean colideCom(Nave nave) {
    return this.x == nave.getX()
            && this.y == nave.getY();
  }


  public void mover(Random random) {
    int direcao = random.nextInt(4);

    switch (direcao) {
      case 0:
        x++;
        break;
      case 1:
        x--;
        break;
      case 2:
        y++;
        break;
      case 3:
        y--;
        break;
    }
  }
}
