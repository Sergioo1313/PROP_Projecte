package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.IAuto;
import edu.upc.epsevg.prop.oust.IPlayer;
import edu.upc.epsevg.prop.oust.PlayerMove;
import edu.upc.epsevg.prop.oust.PlayerType;
import edu.upc.epsevg.prop.oust.SearchType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Jugador amb Iterative Deepening Search (IDS) per Oust
 * Explora profunditats creixents fins que s'exhaureix el temps
 * @author YourTeamName_Name1Name2
 */
public class IDSPlayer implements IPlayer, IAuto {

    private String name;
    private boolean timeout;
    private int nodesExplored;
    private int maxDepthReached;
    private PlayerType myPlayer;
    private List<Point> bestMoveFound;

    /**
     * Constructor
     * @param name Nom del jugador
     */
    public IDSPlayer(String name) {
        this.name = name;
    }

    @Override
    public void timeout() {
        timeout = true;
    }

    @Override
    public String getName() {
        return "IDS(" + name + ")";
    }

    @Override
    public PlayerMove move(GameStatus s) {
        timeout = false;
        nodesExplored = 0;
        maxDepthReached = 0;
        myPlayer = s.getCurrentPlayer();
        bestMoveFound = null;

        // Iterative Deepening: incrementem la profunditat fins que s'acabi el temps
        for (int depth = 1; depth <= 50 && !timeout; depth++) {
            MoveResult result = minimax(s, 0, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            
            if (!timeout && result.path != null && !result.path.isEmpty()) {
                bestMoveFound = result.path;
                maxDepthReached = depth;
            }
            
            // Si hem trobat una victòria segura, no cal continuar
            if (result.evaluation >= 1000000) {
                break;
            }
        }

        if (bestMoveFound == null || bestMoveFound.isEmpty()) {
            return makeRandomMove(s);
        }

        return new PlayerMove(bestMoveFound, maxDepthReached, nodesExplored, SearchType.RANDOM);
    }

    /**
     * Algoritme Minimax amb poda Alpha-Beta i límit de profunditat
     */
    private MoveResult minimax(GameStatus state, int currentDepth, int maxDepth, 
                              int alpha, int beta, boolean maximizing) {
        nodesExplored++;

        // Condicions de parada
        if (timeout) {
            return new MoveResult(null, maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE);
        }

        if (state.isGameOver()) {
            return new MoveResult(null, evaluateTerminal(state));
        }

        if (currentDepth >= maxDepth) {
            return new MoveResult(null, evaluate(state));
        }

        // Generem tots els moviments possibles
        List<List<Point>> allMoves = generateAllPossibleMoves(state);
        
        if (allMoves.isEmpty()) {
            return new MoveResult(null, evaluate(state));
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            List<Point> bestMove = null;

            for (List<Point> move : allMoves) {
                if (timeout) break;

                GameStatus newState = new GameStatus(state);
                applyMove(newState, move);

                MoveResult result = minimax(newState, currentDepth + 1, maxDepth, alpha, beta, false);
                
                if (result.evaluation > maxEval) {
                    maxEval = result.evaluation;
                    bestMove = move;
                }

                alpha = Math.max(alpha, maxEval);
                if (beta <= alpha) {
                    break; // Poda Beta
                }
            }

            return new MoveResult(bestMove, maxEval);
        } else {
            int minEval = Integer.MAX_VALUE;
            List<Point> bestMove = null;

            for (List<Point> move : allMoves) {
                if (timeout) break;

                GameStatus newState = new GameStatus(state);
                applyMove(newState, move);

                MoveResult result = minimax(newState, currentDepth + 1, maxDepth, alpha, beta, true);
                
                if (result.evaluation < minEval) {
                    minEval = result.evaluation;
                    bestMove = move;
                }

                beta = Math.min(beta, minEval);
                if (beta <= alpha) {
                    break; // Poda Alpha
                }
            }

            return new MoveResult(bestMove, minEval);
        }
    }

    /**
     * Genera tots els moviments possibles (incloent captures múltiples)
     */
    private List<List<Point>> generateAllPossibleMoves(GameStatus state) {
        List<List<Point>> allMoves = new ArrayList<>();
        PlayerType currentPlayer = state.getCurrentPlayer();
        
        generateMovesRecursive(state, currentPlayer, new ArrayList<>(), allMoves);
        
        return allMoves;
    }

    /**
     * Genera moviments recursivament per gestionar captures múltiples
     */
    private void generateMovesRecursive(GameStatus state, PlayerType startPlayer, 
                                       List<Point> currentPath, List<List<Point>> allMoves) {
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
                
                if (newState.getCurrentPlayer() == startPlayer && !newState.isGameOver()) {
                    generateMovesRecursive(newState, startPlayer, currentPath, allMoves);
                } else {
                    allMoves.add(new ArrayList<>(currentPath));
                }
            } catch (Exception e) {
                // Moviment invàlid
            }
            
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * Aplica una seqüència de moviments
     */
    private void applyMove(GameStatus state, List<Point> moves) {
        for (Point move : moves) {
            try {
                state.placeStone(move);
            } catch (Exception e) {
                // Ignorem errors
            }
        }
    }

    /**
     * Funció d'avaluació heurística millorada
     */
    private int evaluate(GameStatus state) {
        if (state.isGameOver()) {
            return evaluateTerminal(state);
        }

        int score = 0;
        
        // Comptem peces
        int myPieces = countPieces(state, myPlayer);
        int opponentPieces = countPieces(state, getOpponent(myPlayer));
        
        // Diferència de peces (factor principal)
        score += (myPieces - opponentPieces) * 100;
        
        // Penalització/bonificació addicional
        if (myPieces > opponentPieces) {
            score += 50; // Bonificació per avantatge
        } else if (myPieces < opponentPieces) {
            score -= 50; // Penalització per desavantatge
        }
        
        // Mobilitat: número de moviments possibles
        int myMobility = state.getMoves().size();
        score += myMobility * 5;
        
        // Bonificació per peces restants
        score += myPieces * 10;
        
        return score;
    }

    /**
     * Avaluació per estats terminals
     */
    private int evaluateTerminal(GameStatus state) {
        PlayerType winner = state.GetWinner();
        
        if (winner == null) {
            return 0;
        }
        
        if (winner == myPlayer) {
            return 1000000;
        } else {
            return -1000000;
        }
    }

    /**
     * Compta peces d'un jugador
     */
    private int countPieces(GameStatus state, PlayerType player) {
        int count = 0;
        int size = getBoardSize(state);
        
        for (int i = 0; i < 2 * size - 1; i++) {
            for (int j = 0; j < 2 * size - 1; j++) {
                try {
                    PlayerType color = state.getColor(i, j);
                    if (color == player) {
                        count++;
                    }
                } catch (Exception e) {
                    // Fora del tauler
                }
            }
        }
        
        return count;
    }

    /**
     * Determina la mida del tauler
     */
    private int getBoardSize(GameStatus state) {
        for (int size = 3; size <= 10; size++) {
            try {
                state.getColor(2 * size - 2, 2 * size - 2);
                return size;
            } catch (Exception e) {
                continue;
            }
        }
        return 7;
    }

    /**
     * Obté l'oponent
     */
    private PlayerType getOpponent(PlayerType player) {
        return player == PlayerType.PLAYER1 ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
    }

    /**
     * Moviment aleatori de fallback
     */
    private PlayerMove makeRandomMove(GameStatus s) {
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
        
        return new PlayerMove(path, 0, 0, SearchType.RANDOM);
    }

    /**
     * Classe interna per resultats
     */
    private class MoveResult {
        List<Point> path;
        int evaluation;

        MoveResult(List<Point> path, int evaluation) {
            this.path = path;
            this.evaluation = evaluation;
        }
    }
}