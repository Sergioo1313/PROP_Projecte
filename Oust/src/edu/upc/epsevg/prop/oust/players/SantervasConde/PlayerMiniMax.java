package edu.upc.epsevg.prop.oust.players.SantervasConde;

import edu.upc.epsevg.prop.oust.*;
import java.awt.Point;
import java.util.*;

/**
 * Jugador d'Oust basat en l'algorisme Minimax amb poda Alpha-Beta 
 * @author OustTeam
 */
public class PlayerMiniMax implements IPlayer, IAuto {

    private String name;
    private int profunditatMaxima;
    private int nodesExplored;
    private PlayerType myPlayer;
    private volatile boolean timeout;
    private Random random;

    /** Flag de taula de transposició: valor exacte */
    private static final int TT_EXACT = 0;
    /** Flag de taula de transposició: cota inferior (tall alpha) */
    private static final int TT_LOWER = 1;
    /** Flag de taula de transposició: cota superior (tall beta) */
    private static final int TT_UPPER = 2;

    /**
     * Entrada de la taula de transposició.
     * Emmagatzema el valor avaluat, la profunditat de cerca i el tipus de valor.
     */
    private static class TTEntry {
        /** Profunditat restant quan es va avaluar */
        int depth;
        /** Valor d'avaluació de la posició */
        int value;
        /** Flag indicant el tipus de valor (EXACT, LOWER, UPPER) */
        int flag;

        /**
         * Constructor d'una entrada de taula de transposició.
         * 
         * @param depth Profunditat restant de cerca
         * @param value Valor d'avaluació
         * @param flag Tipus de valor (TT_EXACT, TT_LOWER, TT_UPPER)
         */
        TTEntry(int depth, int value, int flag) {
            this.depth = depth;
            this.value = value;
            this.flag = flag;
        }
    }

    /** Taula de transposició per emmagatzemar posicions avaluades */
    private final Map<Long, TTEntry> transpositionTable = new HashMap<>(1 << 20);

    /** Taula Zobrist per generar hash de posicions: [jugador][fila][columna] */
    private static final long[][][] ZOBRIST;
    /** Hash Zobrist per indicar el torn actual */
    private static final long ZOBRIST_TURN;

    static {
        Random r = new Random(123456);
        ZOBRIST = new long[2][19][19];
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < 19; i++) {
                for (int j = 0; j < 19; j++) {
                    ZOBRIST[p][i][j] = r.nextLong();
                }
            }
        }
        ZOBRIST_TURN = r.nextLong();
    }

    /**
     * Constructor del jugador Minimax.
     * 
     * @param profunditatMaxima Profunditat màxima de cerca en l'arbre de joc
     */
    public PlayerMiniMax(int profunditatMaxima) {
        this.name = "MiniMaxTT(" + profunditatMaxima + ")";
        this.profunditatMaxima = profunditatMaxima;
        this.random = new Random();
    }

    /**
     * Retorna el nom del jugador.
     * 
     * @return Nom identificatiu del jugador
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Notifica al jugador que s'ha exhaurit el temps de reflexió.
     * Activa el flag de timeout per aturar la cerca immediatament.
     */
    @Override
    public void timeout() {
        timeout = true;
    }

    /**
     * Calcula i retorna el millor moviment per a l'estat actual del joc.
     * Utilitza l'algorisme Minimax amb poda Alpha-Beta i taula de transposició.
     * 
     * @param s Estat actual del joc
     * @return Moviment seleccionat amb la seqüència de punts, profunditat i nodes explorats
     */
    @Override
    public PlayerMove move(GameStatus s) {
        transpositionTable.clear();
        nodesExplored = 0;
        timeout = false;
        myPlayer = s.getCurrentPlayer();

        MoveResult result = minimax(
                s, 0, profunditatMaxima,
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                true
        );

        List<Point> bestMove = result.path;
        if (bestMove == null || bestMove.isEmpty()) {
            bestMove = makeRandomMove(s);
        }

        return new PlayerMove(bestMove, profunditatMaxima, nodesExplored, SearchType.MINIMAX);
    }

    /**
     * Algorisme Minimax amb poda Alpha-Beta i taula de transposició.
     * Explora recursivament l'arbre de joc fins a la profunditat màxima.
     * 
     * @param state Estat actual del joc
     * @param depth Profunditat actual en l'arbre de cerca
     * @param maxDepth Profunditat màxima permesa
     * @param alpha Valor alpha per a la poda (millor valor garantit per MAX)
     * @param beta Valor beta per a la poda (millor valor garantit per MIN)
     * @param maximizing True si és torn del jugador maximitzador
     * @return Resultat amb el millor moviment i la seva avaluació
     */
    private MoveResult minimax(GameStatus state, int depth, int maxDepth,
                               int alpha, int beta, boolean maximizing) {

        nodesExplored++;

        if (timeout) {
            return new MoveResult(null,
                    maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE);
        }

        long hash = computeHash(state);
        int remainingDepth = maxDepth - depth;

        // Consulta la taula de transposició
        TTEntry tt = transpositionTable.get(hash);
        if (tt != null && tt.depth >= remainingDepth) {
            if (tt.flag == TT_EXACT) {
                return new MoveResult(null, tt.value);
            } else if (tt.flag == TT_LOWER) {
                alpha = Math.max(alpha, tt.value);
            } else if (tt.flag == TT_UPPER) {
                beta = Math.min(beta, tt.value);
            }
            if (alpha >= beta) {
                return new MoveResult(null, tt.value);
            }
        }

        // Condicions de parada
        if (state.isGameOver() || depth >= maxDepth) {
            return new MoveResult(null, evaluate(state));
        }

        List<List<Point>> moves = generateAllPossibleMoves(state);
        if (moves.isEmpty()) {
            return new MoveResult(null, evaluate(state));
        }

        int bestValue = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        List<Point> bestMove = null;
        int alphaOrig = alpha;
        int betaOrig = beta;

        // Explora tots els moviments
        for (List<Point> move : moves) {
            if (timeout) break;

            GameStatus newState = new GameStatus(state);
            applyMove(newState, move);

            MoveResult r = minimax(
                    newState, depth + 1, maxDepth,
                    alpha, beta, !maximizing
            );

            if (maximizing) {
                if (r.evaluation > bestValue) {
                    bestValue = r.evaluation;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestValue);
            } else {
                if (r.evaluation < bestValue) {
                    bestValue = r.evaluation;
                    bestMove = move;
                }
                beta = Math.min(beta, bestValue);
            }

            if (beta <= alpha) break; // Poda Alpha-Beta
        }

        // Determina el flag per a la taula de transposició
        int flag;
        if (bestValue <= alphaOrig) {
            flag = TT_UPPER;
        } else if (bestValue >= betaOrig) {
            flag = TT_LOWER;
        } else {
            flag = TT_EXACT;
        }

        transpositionTable.put(
                hash,
                new TTEntry(remainingDepth, bestValue, flag)
        );

        return new MoveResult(bestMove, bestValue);
    }

    /**
     * Calcula el hash Zobrist d'un estat del joc.
     * Utilitza XOR per combinar els valors de cada peça i el torn actual.
     * 
     * @param state Estat del joc
     * @return Hash de 64 bits que representa la posició
     */
    private long computeHash(GameStatus state) {
        long h = 0;
        int size = getBoardSize(state);
        int board = 2 * size - 1;

        for (int i = 0; i < board; i++) {
            for (int j = 0; j < board; j++) {
                try {
                    PlayerType c = state.getColor(i, j);
                    if (c != null) {
                        int p = (c == PlayerType.PLAYER1) ? 0 : 1;
                        h ^= ZOBRIST[p][i][j];
                    }
                } catch (Exception ignored) {}
            }
        }

        if (state.getCurrentPlayer() == PlayerType.PLAYER1) {
            h ^= ZOBRIST_TURN;
        }

        return h;
    }

    /**
     * Genera tots els moviments possibles per a l'estat actual,
     * incloent seqüències de captures múltiples.
     * 
     * @param state Estat del joc
     * @return Llista de moviments, on cada moviment és una llista de punts
     */
    private List<List<Point>> generateAllPossibleMoves(GameStatus state) {
        List<List<Point>> allMoves = new ArrayList<>();
        generateMovesRecursive(
                state,
                state.getCurrentPlayer(),
                new ArrayList<>(),
                allMoves
        );
        return allMoves;
    }

    /**
     * Genera moviments recursivament per gestionar les captures múltiples.
     * Continua explorant mentre el mateix jugador pugui seguir capturant.
     * 
     * @param state Estat actual del joc
     * @param startPlayer Jugador que va iniciar el torn
     * @param currentPath Camí actual de moviments (captures encadenades)
     * @param allMoves Llista on s'afegeixen els moviments complets
     */
    private void generateMovesRecursive(GameStatus state, PlayerType startPlayer,
                                        List<Point> currentPath,
                                        List<List<Point>> allMoves) {

        List<Point> moves = state.getMoves();
        if (moves.isEmpty()) {
            if (!currentPath.isEmpty()) {
                allMoves.add(new ArrayList<>(currentPath));
            }
            return;
        }

        for (Point move : moves) {
            GameStatus newState = new GameStatus(state);
            currentPath.add(move);
            try {
                newState.placeStone(move);
                if (newState.getCurrentPlayer() == startPlayer
                        && !newState.isGameOver()) {
                    generateMovesRecursive(newState, startPlayer,
                            currentPath, allMoves);
                } else {
                    allMoves.add(new ArrayList<>(currentPath));
                }
            } catch (Exception ignored) {}
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * Aplica una seqüència de moviments a un estat del joc.
     * 
     * @param state Estat del joc on aplicar els moviments
     * @param moves Llista de punts que representen la seqüència de moviments
     */
    private void applyMove(GameStatus state, List<Point> moves) {
        for (Point p : moves) {
            try {
                state.placeStone(p);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Funció d'avaluació heurística de l'estat del joc.
     * Utilitza una aproximació minimalista basada en la diferència de peces
     * amb soroll aleatori per trencar empats.
     * 
     * @param state Estat del joc a avaluar
     * @return Valor heurístic (positiu favorable, negatiu desfavorable)
     */
    private int evaluate(GameStatus state) {
        if (state.isGameOver()) {
            PlayerType winner = state.GetWinner();
            if (winner == null) return 0;
            return winner == myPlayer ? 1_000_000 : -1_000_000;
        }

        int score = random.nextInt(11) - 5; // Soroll aleatori ±5
        score += (countPieces(state, myPlayer)
                - countPieces(state, getOpponent(myPlayer))) * 150;

        return score;
    }

    /**
     * Compta el nombre de peces d'un jugador al tauler.
     * 
     * @param state Estat del joc
     * @param player Jugador del qual comptar les peces
     * @return Nombre de peces del jugador especificat
     */
    private int countPieces(GameStatus state, PlayerType player) {
        int count = 0;
        int size = getBoardSize(state);
        for (int i = 0; i < 2 * size - 1; i++) {
            for (int j = 0; j < 2 * size - 1; j++) {
                try {
                    if (state.getColor(i, j) == player) count++;
                } catch (Exception ignored) {}
            }
        }
        return count;
    }

    /**
     * Determina la mida del tauler explorant les coordenades vàlides.
     * 
     * @param state Estat del joc
     * @return Mida del costat del tauler hexagonal
     */
    private int getBoardSize(GameStatus state) {
        for (int size = 3; size <= 10; size++) {
            try {
                state.getColor(2 * size - 2, 2 * size - 2);
                return size;
            } catch (Exception ignored) {}
        }
        return 7;
    }

    /**
     * Retorna l'oponent d'un jugador donat.
     * 
     * @param p Jugador actual
     * @return L'altre jugador
     */
    private PlayerType getOpponent(PlayerType p) {
        return p == PlayerType.PLAYER1
                ? PlayerType.PLAYER2
                : PlayerType.PLAYER1;
    }

    /**
     * Genera un moviment aleatori com a fallback quan no es troba millor opció.
     * 
     * @param s Estat del joc
     * @return Llista de punts que representen un moviment aleatori
     */
    private List<Point> makeRandomMove(GameStatus s) {
        List<Point> path = new ArrayList<>();
        PlayerType p = s.getCurrentPlayer();
        GameStatus aux = new GameStatus(s);
        do {
            List<Point> moves = aux.getMoves();
            if (moves.isEmpty()) break;
            Point m = moves.get(0);
            aux.placeStone(m);
            path.add(m);
        } while (p == aux.getCurrentPlayer());
        return path;
    }

    /**
     * Classe interna per emmagatzemar el resultat d'una cerca Minimax.
     * Conté el millor camí trobat i la seva avaluació.
     */
    private static class MoveResult {
        /** Seqüència de punts que representen el millor moviment */
        List<Point> path;
        /** Valor d'avaluació del moviment */
        int evaluation;

        /**
         * Constructor del resultat d'un moviment.
         * 
         * @param path Camí del moviment
         * @param evaluation Avaluació del moviment
         */
        MoveResult(List<Point> path, int evaluation) {
            this.path = path;
            this.evaluation = evaluation;
        }
    }
}
