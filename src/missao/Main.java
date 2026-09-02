package missao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Aplicação principal do jogo "Missão Marte Unifor" em modo console.
 * <p>
 * Inicializa o jogo, gerencia o laço principal de execução, entrada do usuário,
 * pontuação e persistência do ranking em `ranking.json`.
 */
public class Main {
    /**
     * Ponto de entrada da aplicação.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        Random random = new Random();
        int minX = -5;
        int maxX = 5;
        int minY = -5;
        int maxY = 5;

        Path rankingPath = Paths.get("ranking.json");
        List<RankingEntry> ranking = loadRanking(rankingPath);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Qual o nome do Piloto?\n");
        String pilotoNome = scanner.nextLine().trim();
        if (pilotoNome.isEmpty()) {
            pilotoNome = "Piloto Anônimo";
        }

        // --- MENU DE DIFICULDADE ---
        System.out.println("\nSelecione o nível de dificuldade:");
        System.out.println("1 - FÁCIL (30 pts, 1 asteroide)");
        System.out.println("2 - MÉDIO (20 pts, 2 asteroides)");
        System.out.println("3 - DIFÍCIL (10 pts, 4 asteroides)");
        System.out.print("Opção: ");

        String opcaoDif = scanner.nextLine().trim();
        Dificuldade dificuldadeEscolhida;

        switch (opcaoDif) {
            case "1":
                dificuldadeEscolhida = Dificuldade.FACIL;
                break;
            case "3":
                dificuldadeEscolhida = Dificuldade.DIFICIL;
                break;
            default:
                dificuldadeEscolhida = Dificuldade.MEDIO;
                break;
        }
        System.out.println("Dificuldade definida para: " + dificuldadeEscolhida);

        // Cabeçalho e instruções iniciais
        System.out.println("================================================================");
        System.out.println("Missão Marte Unifor — Console");
        System.out.println();
        System.out.println("Ranking dos melhores pilotos:");
        if (ranking.isEmpty()) {
            System.out.println(" - Ainda não há pontuações registradas.");
        } else {
            for (int i = 0; i < Math.min(5, ranking.size()); i++) {
                RankingEntry entry = ranking.get(i);
                System.out.printf(" %d. %s: %d pontos%n", i + 1, entry.name, entry.score);
            }
        }
                
        System.out.println();
        System.out.println("Bem-vindo à Missão Marte Unifor!");
        System.out.println("Seu objetivo é localizar e embarcar todos os passageiros antes que o tempo (pontuação) chegue a zero.");
        System.out.println();
        System.out.println("Comandos: w/s/a/d (mover), c (embarcar), q (sair)");
        System.out.println();
        System.out.println("Pressione Enter para iniciar a missão...");
        scanner.nextLine();
        System.out.println("================================================================");

        boolean playAgain = true;
        while (playAgain) {
            System.out.print("Tamanho do mapa (-X a +X, mínimo 2): ");
            int tamanho = Integer.parseInt(scanner.nextLine());
            if (tamanho < 2) {
                tamanho = 2;
                System.out.println("Tamanho ajustado para 2.");
            }

            minX = -tamanho;
            maxX = tamanho;
            minY = -tamanho;
            maxY = tamanho;

            Missao missao = criarNovaMissao(random, minX, maxX, minY, maxY, dificuldadeEscolhida);
            Nave nave = missao.getNave();
            int score = dificuldadeEscolhida.getPontuacaoInicial();
            boolean running = true;

            while (running) {
                // Desenha o estado atual do mapa no console
                desenharMapa(missao, minX, maxX, minY, maxY, score, pilotoNome);
                System.out.printf("Nave em (%d,%d) | Pontos: %d | Passageiros a bordo: %d | Passageiros restantes: %d\n",
                        nave.getX(), nave.getY(), score, nave.getPassageiros().size(), missao.todosEmbarcados() ? 0 : missao.getPassageiros().size());

                if (missao.verificaColisao()) {
                    nave.perderVida();

                    if (missao.colidiuComAsteroide()) {
                        System.out.println("Bateu em um asteroide!");
                    }
                    if (missao.colidiuComInimigo()) {
                        System.out.println("Bateu em um inimigo!");
                    }

                    if (nave.getVidas() == 0) {
                        System.out.println("Game Over!");
                        break;
                    }
                    System.out.println("Vidas restantes: " + nave.getVidas());
                }


                System.out.print("Para onde ir? ");
                String line = scanner.nextLine().trim().toLowerCase();
                if (line.isEmpty()) continue;
                char cmd = line.charAt(0);
                switch (cmd) {
                    case 'w': nave.moveUp(); score--; break;
                    case 's': nave.moveDown(); score--; break;
                    case 'a': nave.moveLeft(); score--; break;
                    case 'd': nave.moveRight(); score--; break;
                    case 'c': {
                        Passageiro p = missao.passagemNaPosicao();
                        if (p == null) {
                            System.out.println("Nenhum passageiro nesta posição.");
                        } else {
                            boolean ok = missao.embarcarPassageiroNaPosicao();
                            if (ok) {
                                int bonus = p.getPontuacao();
                                score += bonus;
                                System.out.println("Passageiro embarcado. +" + bonus + " pontos!");
                            } else {
                                System.out.println("Nave cheia, não foi possível embarcar.");
                            }
                        }
                        break;
                    }
                    case 'q': running = false; break;
                    default: System.out.println("Comando desconhecido.");
                }

                missao.moverInimigos(random);

                if (score <= 0) {
                    System.out.println("Pontuação zerada. Missão perdida.");
                    break;
                }

                if (missao.todosEmbarcados()) {
                    System.out.println("Todos os passageiros embarcados! Missão concluída com sucesso.");
                    System.out.printf("Pontuação final: %d\n", score);
                    if (score > 0 && isTopScore(ranking, score)) {
                        String dataAtual = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        int passageirosSalvos = nave.getPassageiros().size();
                        String nivelStr = dificuldadeEscolhida.name();

                        ranking.add(new RankingEntry(pilotoNome, score, dataAtual, passageirosSalvos, nivelStr));
                        ranking = ranking.stream()
                                .sorted(Comparator.comparingInt((RankingEntry e) -> e.score).reversed())
                                .limit(5)
                                .collect(Collectors.toList());
                        saveRanking(rankingPath, ranking);
                        System.out.println("Novo ranking salvo! Você está entre os 5 maiores pontuadores.");
                    }
                    break;
                }
            }

            if (!ranking.isEmpty()) {
                System.out.println();
                System.out.println("Ranking Top 5:");
                printRanking(ranking);
            } else {
                System.out.println();
                System.out.println("Ranking vazio. Seja o primeiro a marcar pontos!");
            }

            System.out.print("Deseja iniciar nova missão? (s/n): ");
            String resposta = scanner.nextLine().trim().toLowerCase();
            if (resposta.equals("s") || resposta.equals("sim")) {
                System.out.println("Preparando nova missão...");
            } else {
                playAgain = false;
            }
        }

        scanner.close();
        System.out.println("Fim da execução.");
    }

    private static void printRanking(List<RankingEntry> ranking) {
        int position = 1;
        for (RankingEntry entry : ranking) {
            System.out.printf("%d. %s - %d pontos | Nível: %s | Salvos: %d/3 | Data: %s%n",
                    position++, entry.name, entry.score, entry.nivelDificuldade,
                    entry.passageirosResgatados, entry.dataHora);
        }
    }

    /**
     * Cria uma nova instância de `Missao` populando a nave, passageiros e
     * asteroides em posições aleatórias dentro dos limites especificados.
     *
     * @param random gerador aleatório reutilizável
     * @param minX limite mínimo X do mapa
     * @param maxX limite máximo X do mapa
     * @param minY limite mínimo Y do mapa
     * @param maxY limite máximo Y do mapa
     * @return nova `Missao` configurada
     */
    private static Missao criarNovaMissao(Random random, int minX, int maxX, int minY, int maxY,
            Dificuldade dificuldade) {
        Nave nave = new Nave("A-1", 5, 3);
        Missao missao = new Missao(nave);

        while (missao.getPassageiros().size() < 3) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            if (x == nave.getX() && y == nave.getY()) continue;
            if (posicaoOcupada(missao, x, y)) continue;
            if (missao.getPassageiros().isEmpty()) {
                missao.addPassageiro(new Professor("Dr. Silva", x, y));
            } else if (missao.getPassageiros().size() == 1) {
                missao.addPassageiro(new Engenheiro("Eng. Rosa", x, y));
            } else {
                missao.addPassageiro(new Astronauta("Ast. Lima", x, y));
            }
        }

        while (missao.getAsteroides().size() < dificuldade.getQtdAsteroides()) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            if (x == nave.getX() && y == nave.getY()) continue;
            if (posicaoOcupada(missao, x, y)) continue;
            missao.addAsteroide(new Asteroide(x, y));
        }

        while (missao.getInimigos().size() < 2) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;

            if (posicaoOcupada(missao, x, y)) continue;

            missao.addInimigo(new Inimigo(x, y));
        }

        return missao;
    }

    private static boolean posicaoOcupada(Missao missao, int x, int y) {
        if (missao.getNave().getX() == x && missao.getNave().getY() == y) return true;
        for (Passageiro p : missao.getPassageiros()) {
            if (p.getX() == x && p.getY() == y) return true;
        }
        for (Asteroide a : missao.getAsteroides()) {
            if (a.getX() == x && a.getY() == y) return true;
        }
        for (Inimigo i : missao.getInimigos()) {
            if (i.getX() == x && i.getY() == y) return true;
        }
        return false;
    }

    private static void desenharMapa(Missao missao, int minX, int maxX, int minY, int maxY, int score, String pilotoNome) {
        System.out.println();
        System.out.printf("Mapa da Missão (Pontos: %d) - Piloto: %s%n", score, pilotoNome);
        System.out.print("    ");
        for (int x = minX; x <= maxX; x++) {
            System.out.printf(" %2d", x);
        }
        System.out.println();
        System.out.print("    ");
        for (int x = minX; x <= maxX; x++) {
            System.out.print(" __");
        }
        System.out.println();

        for (int y = minY; y <= maxY; y++) {
            System.out.printf("%3d|", y);
            for (int x = minX; x <= maxX; x++) {
                char symbol = '.';
                if (missao.getNave().getX() == x && missao.getNave().getY() == y) {
                    symbol = 'N';
                } else {
                    for (Passageiro p : missao.getPassageiros()) {
                        if (p.getX() == x && p.getY() == y) {
                            if (p instanceof Engenheiro) {
                                symbol = 'E';
                            } else if (p instanceof Astronauta) {
                                symbol = 'T';
                            } else {
                                symbol = 'P';
                            }
                            break;
                        }
                    }
                    if (symbol == '.') {
                        for (Asteroide a : missao.getAsteroides()) {
                            if (a.getX() == x && a.getY() == y) {
                                symbol = 'A';
                                break;
                            }
                        }
                    }
                    // se não havia asteroides, verifica inimigos
                    if (symbol == '.') {
                        for (Inimigo i : missao.getInimigos()) {
                            if (i.getX() == x && i.getY() == y) {
                                symbol = 'I';
                                break;
                            }
                        }
                    }
                }
                System.out.printf(" %2c", symbol);
            }
            System.out.println();
        }

        System.out.println("Legenda: N=Nave, P=Professor, E=Engenheiro, T=Astronauta, A=Asteroide, I=Inimigo, .=Vazio");
        System.out.println("Resumo de comandos: w(cima)/s(baixo)/a(esquerda)/d(direita) mover, c embarcar, q sair");
        System.out.println("Passageiros restantes:");
        for (Passageiro p : missao.getPassageiros()) {
            System.out.printf(" - %s (%s) em (%d,%d)\n", p.getNome(), p.getTipo(), p.getX(), p.getY());
        }
        System.out.println();
    }

    private static boolean isTopScore(List<RankingEntry> ranking, int score) {
        if (ranking.size() < 5) {
            return true;
        }
        return score > ranking.get(ranking.size() - 1).score;
    }

    private static List<RankingEntry> loadRanking(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            return parseRankingJson(json);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveRanking(Path path, List<RankingEntry> ranking) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < ranking.size(); i++) {
            RankingEntry entry = ranking.get(i);
            builder.append("{\"name\":\"").append(entry.name.replace("\"", "\\\""))
                   .append("\",\"score\":").append(entry.score)
                   .append(",\"dataHora\":\"").append(entry.dataHora)
                   .append("\",\"passageirosResgatados\":").append(entry.passageirosResgatados)
                   .append(",\"nivelDificuldade\":\"").append(entry.nivelDificuldade)
                   .append("\"}");
            if (i < ranking.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        try {
            Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Não foi possível salvar o ranking: " + e.getMessage());
        }
    }

    private static List<RankingEntry> parseRankingJson(String json) {
        List<RankingEntry> ranking = new ArrayList<>();
        if (json.isEmpty() || json.equals("[]")) {
            return ranking;
        }
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }

        int index = 0;
        while (index < json.length()) {
            int start = json.indexOf('{', index);
            if (start < 0) break;
            int end = json.indexOf('}', start);
            if (end < 0) break;
            String object = json.substring(start + 1, end);
            String name = null;
            Integer score = null;
            String dataHora = "Data Indisponível";
            int passageirosResgatados = 3;
            String nivelDificuldade = "DESCONHECIDO";

            for (String part : object.split(",")) {
                String[] pair = part.split(":", 2);
                if (pair.length != 2) continue;

                String key = pair[0].trim().replaceAll("\"", "");
                String value = pair[1].trim().replaceAll("\"", "");

                if (key.equals("name")) {
                    name = value;
                } else if (key.equals("score")) {
                    try { score = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                } else if (key.equals("dataHora")) {
                    dataHora = value;
                } else if (key.equals("passageirosResgatados")) {
                    try { passageirosResgatados = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                } else if (key.equals("nivelDificuldade")) {
                    nivelDificuldade = value;
                }
            }
            if (name != null && score != null) {
                ranking.add(new RankingEntry(name, score, dataHora, passageirosResgatados, nivelDificuldade));
            }
            index = end + 1;
        }

        ranking.sort(Comparator.comparingInt((RankingEntry e) -> e.score).reversed());
        return ranking;
    }

    private static class RankingEntry {
        private final String name;
        private final int score;
        private final String dataHora;
        private final int passageirosResgatados;
        private final String nivelDificuldade;

        private RankingEntry(String name, int score, String dataHora, int passageirosResgatados, String nivelDificuldade) {
            this.name = name;
            this.score = score;
            this.dataHora = dataHora;
            this.passageirosResgatados = passageirosResgatados;
            this.nivelDificuldade = nivelDificuldade;
        }
    }
}
