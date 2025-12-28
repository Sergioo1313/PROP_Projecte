package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.*;
import java.awt.Point;
import java.util.*;

/**
 * Minimax player with Alpha-Beta pruning + Transposition Table
 */
public class MinimaxPlayer implements IPlayer, IAuto {

    private String name;
    private int profunditatMaxima;
    private int nodesExplored;
    private PlayerType myPlayer;
    private volatile boolean timeout;
    private Random random;

    /* =========================
       TRANSPOSITION TABLE
       ========================= */

    private static final int TT_EXACT = 0;
    private static final int TT_LOWER = 1;
    private static final int TT_UPPER = 2;

    private static class TTEntry {
        int depth;
        int value;
        int flag;

        TTEntry(int depth, int value, int flag) {
            this.depth = depth;
            this.value = value;
            this.flag = flag;
        }
    }

    private final Map<Long, TTEntry> transpositionTable = new HashMap<>(1 << 20);

    /* =========================
       ZOBRIST HASHING
       ========================= */

    private static final long[][][] ZOBRIST;
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

    /* ========================= */

    public MinimaxPlayer(int profunditatMaxima) {
        this.name = "MiniMaxTT(" + profunditatMaxima + ")";
        this.profunditatMaxima = profunditatMaxima;
        this.random = new Random();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void timeout() {
        timeout = true;
    }

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

    /* =========================
       MINIMAX + ALPHA-BETA + TT
       ========================= */

    private MoveResult minimax(GameStatus state, int depth, int maxDepth,
                               int alpha, int beta, boolean maximizing) {

        nodesExplored++;

        if (timeout) {
            return new MoveResult(null,
                    maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE);
        }

        long hash = computeHash(state);
        int remainingDepth = maxDepth - depth;

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

            if (beta <= alpha) break;
        }

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

    /* =========================
       HASHING
       ========================= */

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

    /* =========================
       MOVE GENERATION
       ========================= */

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

    private void applyMove(GameStatus state, List<Point> moves) {
        for (Point p : moves) {
            try {
                state.placeStone(p);
            } catch (Exception ignored) {}
        }
    }

    /* =========================
       EVALUATION (unchanged)
       ========================= */

    private int evaluate(GameStatus state) {
        if (state.isGameOver()) {
            PlayerType winner = state.GetWinner();
            if (winner == null) return 0;
            return winner == myPlayer ? 1_000_000 : -1_000_000;
        }

        int score = random.nextInt(11) - 5;
        score += (countPieces(state, myPlayer)
                - countPieces(state, getOpponent(myPlayer))) * 150;

        return score;
    }

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

    private int getBoardSize(GameStatus state) {
        for (int size = 3; size <= 10; size++) {
            try {
                state.getColor(2 * size - 2, 2 * size - 2);
                return size;
            } catch (Exception ignored) {}
        }
        return 7;
    }

    private PlayerType getOpponent(PlayerType p) {
        return p == PlayerType.PLAYER1
                ? PlayerType.PLAYER2
                : PlayerType.PLAYER1;
    }

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

    private static class MoveResult {
        List<Point> path;
        int evaluation;

        MoveResult(List<Point> path, int evaluation) {
            this.path = path;
            this.evaluation = evaluation;
        }
    }
}
