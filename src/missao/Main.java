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
     * <p>
     * Inicializa recursos, exibe instruções, e executa o loop de jogo até o
     * usuário escolher não jogar novamente.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        Random random = new Random();
        // Gerador aleatório usado para posicionar passageiros e asteroides
        int minX = -5;
        int maxX = 5;
        int minY = -5;
        int maxY = 5;

        // Caminho para o arquivo que persiste o ranking de pontuações
        Path rankingPath = Paths.get("ranking.json");
        // Carrega ranking existente (se houver) para exibir e possivelmente atualizar
        List<RankingEntry> ranking = loadRanking(rankingPath);

        // Scanner para leitura de entradas do usuário via console
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o nome do piloto: ");
        String pilotoNome = scanner.nextLine().trim();
        if (pilotoNome.isEmpty()) {
            pilotoNome = "Piloto Anônimo";
        }

        // Cabeçalho e instruções iniciais do jogo
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
        System.out.println(
                "Bem-vindo à Missão Marte Unifor! Sua nave foi selecionada para uma expedição de resgate e pesquisa na superfície marciana.");
        System.out.println(
                "Seu objetivo é localizar e embarcar todos os passageiros necessários para completar a missão antes que o seu tempo (pontuação) chegue a zero.");
        System.out.println();
        System.out.println("Objetivo:");
        System.out.println(" - Mover a nave pelo mapa");
        System.out.println(" - Encontrar e embarcar todos os passageiros");
        System.out.println(" - Evitar colisões com asteroides");
        System.out.println(" - Manter a pontuação acima de zero");
        System.out.println();
        System.out.println("Comandos:");
        System.out.println(" - w: mover para cima");
        System.out.println(" - s: mover para baixo");
        System.out.println(" - a: mover para a esquerda");
        System.out.println(" - d: mover para a direita");
        System.out.println(" - c: embarcar passageiro na posição atual");
        System.out.println(" - q: sair do jogo");
        System.out.println();
        System.out
                .println("Pontuação inicial: 20 pontos. Cada movimento custa 1 ponto. Cada embarque vale +10 pontos.");
        System.out.println();
        System.out.println("Pressione Enter para iniciar a missão...");
        // Aguarda o usuário pressionar Enter antes de iniciar o primeiro jogo
        scanner.nextLine();
        System.out.println("================================================================");

        // Loop externo: permite jogar várias missões até o usuário optar por sair
        boolean playAgain = true;
        while (playAgain) {
            Missao missao = criarNovaMissao(random, minX, maxX, minY, maxY);
            Nave nave = missao.getNave();
            int score = 20;
            boolean running = true;

            while (running) {
                // Desenha o estado atual do mapa no console
                desenharMapa(missao, -5, 5, -5, 5, score, pilotoNome);
                System.out.printf(
                        "Nave em (%d,%d) | Pontos: %d | Passageiros a bordo: %d | Passageiros restantes: %d\n",
                        nave.getX(), nave.getY(), score, nave.getPassageiros().size(),
                        missao.todosEmbarcados() ? 0 : missao.getPassageiros().size());

                if (missao.verificaColisao()) {
                    // Se a nave estiver na mesma posição de um asteroide, fim da missão
                    System.out.println("Colisão com asteroide! Missão abortada.");
                    break;
                }

                System.out.print("Para onde ir? ");
                // Leitura do comando do jogador (w/s/a/d/c/q)
                String line = scanner.nextLine().trim().toLowerCase();
                if (line.isEmpty())
                    continue;
                char cmd = line.charAt(0);
                switch (cmd) {
                    case 'w':
                        nave.moveUp();
                        score--;
                        break;
                    case 's':
                        nave.moveDown();
                        score--;
                        break;
                    case 'a':
                        nave.moveLeft();
                        score--;
                        break;
                    case 'd':
                        nave.moveRight();
                        score--;
                        break;
                    case 'c': {
                        // Tenta embarcar um passageiro se houver um na posição atual
                        Passageiro p = missao.passagemNaPosicao();
                        if (p == null) {
                            System.out.println("Nenhum passageiro nesta posição.");
                        } else {
                            // `embarcarPassageiroNaPosicao` remove o passageiro do solo
                            // apenas se o embarque na nave for bem-sucedido
                            boolean ok = missao.embarcarPassageiroNaPosicao();
                            if (ok) {
                                score += 10; // bônus por embarque
                                System.out.println("Passageiro embarcado. +10 pontos!");
                            } else {
                                System.out.println("Nave cheia, não foi possível embarcar.");
                            }
                        }
                        break;
                    }
                    case 'q':
                        running = false;
                        break;
                    default:
                        System.out.println("Comando desconhecido.");
                }

                if (score <= 0) {
                    // Se a pontuação chegar a zero, a missão é perdida
                    System.out.println("Pontuação zerada. Missão perdida.");
                    break;
                }

                if (missao.todosEmbarcados()) {
                    // Caso todos os passageiros tenham sido embarcados, a missão é concluída
                    System.out.println("Todos os passageiros embarcados! Missão concluída com sucesso.");
                    System.out.printf("Pontuação final: %d\n", score);
                    if (score > 0 && isTopScore(ranking, score)) {
                        // Atualiza ranking e persiste no disco
                        ranking.add(new RankingEntry(pilotoNome, score));
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

    /**
     * Imprime o ranking formatado no console.
     *
     * @param ranking lista ordenada de `RankingEntry` a ser exibida
     */
    private static void printRanking(List<RankingEntry> ranking) {
        int position = 1;
        for (RankingEntry entry : ranking) {
            System.out.printf("%d. %s - %d pontos%n", position++, entry.name, entry.score);
        }
    }

    /**
     * Cria uma nova instância de `Missao` populando a nave, passageiros e
     * asteroides em posições aleatórias dentro dos limites especificados.
     *
     * @param random gerador aleatório reutilizável
     * @param minX   limite mínimo X do mapa
     * @param maxX   limite máximo X do mapa
     * @param minY   limite mínimo Y do mapa
     * @param maxY   limite máximo Y do mapa
     * @return nova `Missao` configurada
     */
    private static Missao criarNovaMissao(Random random, int minX, int maxX, int minY, int maxY) {
        Nave nave = new Nave("A-1", 5);
        Missao missao = new Missao(nave);

        // Cria 3 passageiros em posições aleatórias dentro dos limites
        while (missao.getPassageiros().size() < 3) {
            // escolhe coordenadas aleatórias incluindo os limites
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            // evita posicionar um passageiro exatamente na posição inicial da nave
            if (x == nave.getX() && y == nave.getY())
                continue;
            // evita sobreposição com outras entidades já posicionadas
            if (posicaoOcupada(missao, x, y))
                continue;
            // adiciona tipos diferentes em ordem: Professor, Engenheiro, Professor
            if (missao.getPassageiros().isEmpty()) {
                missao.addPassageiro(new Professor("Dr. Silva", x, y));
            } else if (missao.getPassageiros().size() == 1) {
                missao.addPassageiro(new Engenheiro("Eng. Rosa", x, y));
            } else {
                missao.addPassageiro(new Astronauta("Ast. Lima", x, y));
            }
        }

        // Cria 2 asteroides em posições aleatórias sem colidir com a nave nem com
        // passageiros
        while (missao.getAsteroides().size() < 2) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int y = random.nextInt(maxY - minY + 1) + minY;
            if (x == nave.getX() && y == nave.getY())
                continue;
            if (posicaoOcupada(missao, x, y))
                continue;
            missao.addAsteroide(new Asteroide(x, y));
        }

        return missao;
    }

    /**
     * Verifica se a posição (x,y) já está ocupada por qualquer entidade da
     * missão (nave, passageiros ou asteroides).
     *
     * @param missao missão que contém entidades
     * @param x      coordenada X
     * @param y      coordenada Y
     * @return true se ocupada, false caso contrário
     */
    private static boolean posicaoOcupada(Missao missao, int x, int y) {
        // verifica se a própria nave está na posição
        if (missao.getNave().getX() == x && missao.getNave().getY() == y)
            return true;
        // verifica cada passageiro
        for (Passageiro p : missao.getPassageiros()) {
            if (p.getX() == x && p.getY() == y)
                return true;
        }
        // verifica cada asteroide
        for (Asteroide a : missao.getAsteroides()) {
            if (a.getX() == x && a.getY() == y)
                return true;
        }
        // posição livre
        return false;
    }

    /**
     * Renderiza no console um mapa textual com a posição da nave, passageiros
     * e asteroides, além de legenda e resumo de comandos.
     *
     * @param missao     estado atual da missão
     * @param minX       limite mínimo X para renderização
     * @param maxX       limite máximo X para renderização
     * @param minY       limite mínimo Y para renderização
     * @param maxY       limite máximo Y para renderização
     * @param score      pontuação atual do jogador
     * @param pilotoNome nome do piloto para exibição
     */
    private static void desenharMapa(Missao missao, int minX, int maxX, int minY, int maxY, int score,
            String pilotoNome) {
        System.out.println();
        System.out.printf("Mapa da Missão (Pontos: %d) - Piloto: %s%n", score, pilotoNome);
        System.out.print("    ");
        // cabeçalho das colunas (coordenadas X)
        for (int x = minX; x <= maxX; x++) {
            System.out.printf(" %2d", x);
        }
        System.out.println();
        System.out.print("    ");
        // linha separadora do cabeçalho
        for (int x = minX; x <= maxX; x++) {
            System.out.print(" __");
        }
        System.out.println();

        for (int y = minY; y <= maxY; y++) {
            System.out.printf("%3d|", y);
            for (int x = minX; x <= maxX; x++) {
                char symbol = '.';
                if (missao.getNave().getX() == x && missao.getNave().getY() == y) {
                    symbol = '@';
                } else {
                    // verifica passageiros primeiro (preferência de desenho)
                    for (Passageiro p : missao.getPassageiros()) {
                        if (p.getX() == x && p.getY() == y) {
                            // diferencia engenheiro de professor pelo símbolo
                            if (p instanceof Engenheiro) {
                                symbol = 'E';
                            } else if (p instanceof Astronauta) {
                                symbol = 'T';
                            } else {
                                symbol = 'P';
                            }
                            break; // encontrou um passageiro nesta célula
                        }
                    }
                    // se não havia passageiro, verifica asteroides
                    if (symbol == '.') {
                        for (Asteroide a : missao.getAsteroides()) {
                            if (a.getX() == x && a.getY() == y) {
                                symbol = '#';
                                break;
                            }
                        }
                    }
                }
                System.out.printf(" %2c", symbol);
            }
            System.out.println();
        }

        System.out.println("Legenda: N=Nave, P=Professor, E=Engenheiro, A=Asteroide, .=Vazio");
        System.out.println("Resumo de comandos: w(cima)/s(baixo)/a(esquerda)/d(direita) mover, c embarcar, q sair");
        System.out.println("Passageiros restantes:");
        for (Passageiro p : missao.getPassageiros()) {
            System.out.printf(" - %s (%s) em (%d,%d)\n", p.getNome(), p.getTipo(), p.getX(), p.getY());
        }
        System.out.println();
    }

    /**
     * Retorna se a pontuação informada entra no ranking top-5.
     *
     * @param ranking lista atual de pontuações (ordenada desc)
     * @param score   pontuação a avaliar
     * @return true se for top-5, false caso contrário
     */
    private static boolean isTopScore(List<RankingEntry> ranking, int score) {
        if (ranking.size() < 5) {
            return true;
        }
        return score > ranking.get(ranking.size() - 1).score;
    }

    /**
     * Carrega o ranking a partir do arquivo JSON se existir; caso contrário
     * retorna uma lista vazia.
     *
     * @param path caminho para `ranking.json`
     * @return lista de `RankingEntry`
     */
    private static List<RankingEntry> loadRanking(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            // lê todo o arquivo como UTF-8 e passa para o parser simples
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            return parseRankingJson(json);
        } catch (IOException e) {
            // em caso de erro de I/O, retorna ranking vazio para não quebrar o jogo
            return new ArrayList<>();
        }
    }

    /**
     * Salva a lista de ranking no arquivo informado em formato JSON simples.
     *
     * @param path    caminho destino do arquivo
     * @param ranking lista de `RankingEntry` a gravar
     */
    private static void saveRanking(Path path, List<RankingEntry> ranking) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < ranking.size(); i++) {
            RankingEntry entry = ranking.get(i);
            builder.append("{\"name\":\"")
                    .append(entry.name.replace("\"", "\\\""))
                    .append("\",\"score\":")
                    .append(entry.score)
                    .append("}");
            if (i < ranking.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        // grava o JSON resultante no disco; usa UTF-8 explicitamente
        try {
            Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // não interrompe o jogo; apenas informa falha ao usuário
            System.out.println("Não foi possível salvar o ranking: " + e.getMessage());
        }
    }

    /**
     * Parser simples para o formato mínimo de `ranking.json` utilizado pela
     * aplicação. É tolerante, mas não substitui um parser JSON completo.
     *
     * @param json conteúdo bruto do arquivo
     * @return lista de `RankingEntry` ordenada por score desc
     */
    private static List<RankingEntry> parseRankingJson(String json) {
        List<RankingEntry> ranking = new ArrayList<>();
        if (json.isEmpty() || json.equals("[]")) {
            return ranking;
        }
        // remove espaços e colchetes externos
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }

        // itera por objetos JSON simples {"name":"...","score":N}
        int index = 0;
        while (index < json.length()) {
            int start = json.indexOf('{', index);
            if (start < 0)
                break; // sem mais objetos
            int end = json.indexOf('}', start);
            if (end < 0)
                break; // objeto incompleto
            String object = json.substring(start + 1, end);
            String name = null;
            Integer score = null;
            // divide por vírgulas e tenta extrair pares chave:valor
            for (String part : object.split(",")) {
                String[] pair = part.split(":", 2);
                if (pair.length != 2)
                    continue;
                String key = pair[0].trim().replaceAll("\"", "");
                String value = pair[1].trim();
                if (key.equals("name")) {
                    // remove aspas ao redor do valor de name e desfaz escape de aspas
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        name = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                    }
                } else if (key.equals("score")) {
                    try {
                        score = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        // valor inválido; ignora e não adiciona essa entrada
                    }
                }
            }
            if (name != null && score != null) {
                ranking.add(new RankingEntry(name, score));
            }
            // avança para procurar o próximo objeto
            index = end + 1;
        }

        // ordena por score decrescente
        ranking.sort(Comparator.comparingInt((RankingEntry e) -> e.score).reversed());
        return ranking;
    }

    /**
     * Representa uma entrada simples do ranking com nome e pontuação.
     */
    private static class RankingEntry {
        private final String name;
        private final int score;

        private RankingEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
