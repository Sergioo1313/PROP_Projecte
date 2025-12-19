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
 * Jugador Minimax amb poda Alpha-Beta per Oust
 * @author YourTeamName_Name1Name2
 */
public class MinimaxPlayer implements IPlayer, IAuto {

    private String name;
    private int maxDepth;
    private boolean timeout;
    private int nodesExplored;
    private int maxDepthReached;
    private PlayerType myPlayer;

    /**
     * Constructor
     * @param name Nom del jugador
     * @param depth Profunditat màxima de cerca
     */
    public MinimaxPlayer(String name, int depth) {
        this.name = name;
        this.maxDepth = depth;
    }

    @Override
    public void timeout() {
        timeout = true;
    }

    @Override
    public String getName() {
        return "Minimax(" + name + ")";
    }

    @Override
    public PlayerMove move(GameStatus s) {
        timeout = false;
        nodesExplored = 0;
        maxDepthReached = 0;
        myPlayer = s.getCurrentPlayer();

        // Busquem el millor moviment
        MoveResult result = minimax(s, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        
        if (result.path == null || result.path.isEmpty()) {
            // Si no hi ha millor moviment, fem un moviment aleatori
            return makeRandomMove(s);
        }

        return new PlayerMove(result.path, maxDepthReached, nodesExplored, SearchType.RANDOM);
    }

    /**
     * Algoritme Minimax amb poda Alpha-Beta
     */
    private MoveResult minimax(GameStatus state, int depth, int alpha, int beta, boolean maximizing) {
        nodesExplored++;
        
        if (depth > maxDepthReached) {
            maxDepthReached = depth;
        }

        // Condicions de parada
        if (timeout) {
            return new MoveResult(null, maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE);
        }

        if (state.isGameOver()) {
            return new MoveResult(null, evaluateTerminal(state));
        }

        if (depth >= maxDepth) {
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

                MoveResult result = minimax(newState, depth + 1, alpha, beta, false);
                
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

                MoveResult result = minimax(newState, depth + 1, alpha, beta, true);
                
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
        
        // Generem moviments recursivament per cobrir captures múltiples
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
                
                // Si seguim sent el mateix jugador, podem continuar capturant
                if (newState.getCurrentPlayer() == startPlayer && !newState.isGameOver()) {
                    generateMovesRecursive(newState, startPlayer, currentPath, allMoves);
                } else {
                    // Moviment complet
                    allMoves.add(new ArrayList<>(currentPath));
                }
            } catch (Exception e) {
                // Moviment invàlid, ignorem
            }
            
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * Aplica una seqüència de moviments a un estat
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
     * Funció d'avaluació heurística
     */
    private int evaluate(GameStatus state) {
        if (state.isGameOver()) {
            return evaluateTerminal(state);
        }

        int score = 0;
        
        // Comptem les peces de cada jugador
        int myPieces = countPieces(state, myPlayer);
        int opponentPieces = countPieces(state, getOpponent(myPlayer));
        
        // La diferència de peces és el factor més important
        score += (myPieces - opponentPieces) * 100;
        
        // Penalitzem si tenim menys peces
        if (myPieces < opponentPieces) {
            score -= 50;
        }
        
        // Bonificació per tenir més peces
        score += myPieces * 10;
        
        return score;
    }

    /**
     * Avaluació per estats terminals
     */
    private int evaluateTerminal(GameStatus state) {
        PlayerType winner = state.GetWinner();
        
        if (winner == null) {
            return 0; // Empat
        }
        
        if (winner == myPlayer) {
            return 1000000; // Victòria
        } else {
            return -1000000; // Derrota
        }
    }

    /**
     * Compta les peces d'un jugador al tauler
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
                    // Posició fora del tauler
                }
            }
        }
        
        return count;
    }

    /**
     * Obté la mida del tauler (assumim 7 per defecte)
     */
    private int getBoardSize(GameStatus state) {
        // Com no tenim accés directe a la mida, provem diferents mides
        for (int size = 3; size <= 10; size++) {
            try {
                state.getColor(2 * size - 2, 2 * size - 2);
                return size;
            } catch (Exception e) {
                continue;
            }
        }
        return 7; // Valor per defecte
    }

    /**
     * Obté l'oponent
     */
    private PlayerType getOpponent(PlayerType player) {
        return player == PlayerType.PLAYER1 ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
    }

    /**
     * Genera un moviment aleatori de fallback
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
     * Classe interna per emmagatzemar resultats
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