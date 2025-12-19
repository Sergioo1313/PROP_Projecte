package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.IPlayer;
import edu.upc.epsevg.prop.oust.PlayerMove;
import edu.upc.epsevg.prop.oust.PlayerType;
import edu.upc.epsevg.prop.oust.SearchType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Jugador amb Minimax amb poda Alpha-Beta i profunditat limitada
 */
public class PlayerMiniMax implements IPlayer {

    private String name;
    private int profunditatMaxima;
    private int nodesExplored;
    private PlayerType myPlayer;

    public PlayerMiniMax(int profunditatMaxima) {
        this.name = "MiniMax(" + profunditatMaxima + ")";
        this.profunditatMaxima = profunditatMaxima;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlayerMove move(GameStatus s) {
        nodesExplored = 0;
        myPlayer = s.getCurrentPlayer();

        MoveResult result = minimax(s, 0, profunditatMaxima, Integer.MIN_VALUE, Integer.MAX_VALUE, true);

        List<Point> bestMove = result.path;
        if (bestMove == null || bestMove.isEmpty()) {
            bestMove = makeRandomMove(s);
        }

        return new PlayerMove(bestMove, profunditatMaxima, nodesExplored, SearchType.RANDOM);
    }

    private MoveResult minimax(GameStatus state, int currentDepth, int maxDepth, int alpha, int beta, boolean maximizing) {
        nodesExplored++;

        if (state.isGameOver() || currentDepth >= maxDepth) {
            return new MoveResult(null, evaluate(state));
        }

        List<List<Point>> allMoves = generateAllPossibleMoves(state);
        if (allMoves.isEmpty()) {
            return new MoveResult(null, evaluate(state));
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            List<Point> bestMove = null;

            for (List<Point> move : allMoves) {
                GameStatus newState = new GameStatus(state);
                applyMove(newState, move);

                MoveResult result = minimax(newState, currentDepth + 1, maxDepth, alpha, beta, false);
                if (result.evaluation > maxEval) {
                    maxEval = result.evaluation;
                    bestMove = move;
                }

                alpha = Math.max(alpha, maxEval);
                if (beta <= alpha) break; // poda Beta
            }

            return new MoveResult(bestMove, maxEval);
        } else {
            int minEval = Integer.MAX_VALUE;
            List<Point> bestMove = null;

            for (List<Point> move : allMoves) {
                GameStatus newState = new GameStatus(state);
                applyMove(newState, move);

                MoveResult result = minimax(newState, currentDepth + 1, maxDepth, alpha, beta, true);
                if (result.evaluation < minEval) {
                    minEval = result.evaluation;
                    bestMove = move;
                }

                beta = Math.min(beta, minEval);
                if (beta <= alpha) break; // poda Alpha
            }

            return new MoveResult(bestMove, minEval);
        }
    }

    private List<List<Point>> generateAllPossibleMoves(GameStatus state) {
        List<List<Point>> allMoves = new ArrayList<>();
        PlayerType currentPlayer = state.getCurrentPlayer();
        generateMovesRecursive(state, currentPlayer, new ArrayList<>(), allMoves);
        return allMoves;
    }

    private void generateMovesRecursive(GameStatus state, PlayerType startPlayer,
                                        List<Point> currentPath, List<List<Point>> allMoves) {
        List<Point> moves = state.getMoves();
        if (moves.isEmpty()) {
            if (!currentPath.isEmpty()) allMoves.add(new ArrayList<>(currentPath));
            return;
        }

        for (Point move : moves) {
            GameStatus newState = new GameStatus(state);
            currentPath.add(move);
            try {
                newState.placeStone(move);
                if (newState.getCurrentPlayer() == startPlayer && !newState.isGameOver()) {
                    generateMovesRecursive(newState, startPlayer, currentPath, allMoves);
                } else {
                    allMoves.add(new ArrayList<>(currentPath));
                }
            } catch (Exception e) {
                // ignoramos movimientos inválidos
            }
            currentPath.remove(currentPath.size() - 1);
        }
    }

    private void applyMove(GameStatus state, List<Point> moves) {
        for (Point move : moves) {
            try {
                state.placeStone(move);
            } catch (Exception e) {}
        }
    }

    private int evaluate(GameStatus state) {
        if (state.isGameOver()) return evaluateTerminal(state);

        int score = 0;
        int myPieces = countPieces(state, myPlayer);
        int opponentPieces = countPieces(state, getOpponent(myPlayer));
        score += (myPieces - opponentPieces) * 100;
        if (myPieces > opponentPieces) score += 50;
        else if (myPieces < opponentPieces) score -= 50;
        score += state.getMoves().size() * 5;
        score += myPieces * 10;
        return score;
    }

    private int evaluateTerminal(GameStatus state) {
        PlayerType winner = state.GetWinner();
        if (winner == null) return 0;
        return winner == myPlayer ? 1000000 : -1000000;
    }

    private int countPieces(GameStatus state, PlayerType player) {
        int count = 0;
        int size = getBoardSize(state);
        for (int i = 0; i < 2 * size - 1; i++) {
            for (int j = 0; j < 2 * size - 1; j++) {
                try {
                    if (state.getColor(i, j) == player) count++;
                } catch (Exception e) {}
            }
        }
        return count;
    }

    private int getBoardSize(GameStatus state) {
        for (int size = 3; size <= 10; size++) {
            try {
                state.getColor(2 * size - 2, 2 * size - 2);
                return size;
            } catch (Exception e) {}
        }
        return 7;
    }

    private PlayerType getOpponent(PlayerType player) {
        return player == PlayerType.PLAYER1 ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
    }

    private List<Point> makeRandomMove(GameStatus s) {
        List<Point> path = new ArrayList<>();
        PlayerType currentPlayer = s.getCurrentPlayer();
        GameStatus aux = new GameStatus(s);
        do {
            List<Point> moves = aux.getMoves();
            if (moves.isEmpty()) break;
            Point m = moves.get(0);
            aux.placeStone(m);
            path.add(m);
        } while (currentPlayer == aux.getCurrentPlayer());
        return path;
    }

    private class MoveResult {
        List<Point> path;
        int evaluation;
        MoveResult(List<Point> path, int evaluation) {
            this.path = path;
            this.evaluation = evaluation;
        }
    }
}




