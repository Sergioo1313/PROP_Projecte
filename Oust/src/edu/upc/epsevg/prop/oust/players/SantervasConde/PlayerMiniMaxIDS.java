package edu.upc.epsevg.prop.oust.players.SantervasConde;

import edu.upc.epsevg.prop.oust.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Jugador d'Oust basat en Iterative Deepening Search (IDS)
 * @author OustTeam 
 */
public class PlayerMiniMaxIDS implements IPlayer, IAuto {

    private String name;
    private volatile boolean timeout;
    private int nodesExplored;
    private int maxDepthReached;
    private PlayerType myPlayer;
    private List<Point> bestMoveFound;

    /**
     * Constructor del jugador IDS.
     * Inicialitza el jugador amb un nom per defecte.
     */
    public PlayerMiniMaxIDS() {
        this.name = "JugadorPorDefecto";
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
     * Retorna el nom del jugador.
     * 
     * @return Nom identificatiu del jugador amb prefix "IDS"
     */
    @Override
    public String getName() {
        return "IDS(" + name + ")";
    }

    /**
     * Calcula i retorna el millor moviment utilitzant Iterative Deepening Search.
     * @param s Estat actual del joc
     * @return Moviment seleccionat amb estadístiques de cerca
     */
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
     * @param state Estat actual del joc
     * @param currentDepth Profunditat actual en l'arbre de cerca
     * @param maxDepth Profunditat màxima permesa per aquesta iteració
     * @param alpha Millor valor garantit per al jugador maximitzador
     * @param beta Millor valor garantit per al jugador minimitzador
     * @param maximizing True si és el torn del jugador maximitzador
     * @return Resultat amb el millor moviment i la seva avaluació
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
     * Genera tots els moviments possibles per a l'estat actual,
     * incloent seqüències de captures múltiples.
     * 
     * @param state Estat del joc
     * @return Llista de moviments possibles, on cada moviment és una seqüència de punts
     */
    private List<List<Point>> generateAllPossibleMoves(GameStatus state) {
        List<List<Point>> allMoves = new ArrayList<>();
        PlayerType currentPlayer = state.getCurrentPlayer();
        
        generateMovesRecursive(state, currentPlayer, new ArrayList<>(), allMoves);
        
        return allMoves;
    }

    /**
     * Genera moviments recursivament per gestionar les captures encadenades.
     * @param state Estat actual del joc
     * @param startPlayer Jugador que va iniciar el torn
     * @param currentPath Camí actual de captures encadenades
     * @param allMoves Llista on s'afegeixen els moviments complets
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
                // Moviment invàlid, l'ignorem
            }
            
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * Aplica una seqüència de moviments a un estat del joc.
     * 
     * @param state Estat del joc on aplicar els moviments
     * @param moves Seqüència de punts que representen el moviment complet
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
     * Funció d'avaluació heurística per a posicions no terminals.
     * @param state Estat del joc a avaluar
     * @return Valor heurístic (positiu si és favorable, negatiu si és desfavorable)
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
        
        // Bonificació/penalització addicional
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
     * Avaluació per a estats terminals del joc.
     * 
     * @param state Estat terminal del joc
     * @return +1.000.000 si guanyem, -1.000.000 si perdem, 0 si empat
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
     * Compta el nombre de peces d'un jugador al tauler.
     * 
     * @param state Estat del joc
     * @param player Jugador del qual comptar les peces
     * @return Nombre total de peces del jugador
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
     * Determina la mida del costat del tauler hexagonal.
     * 
     * @param state Estat del joc
     * @return Mida del costat del tauler (per defecte 7)
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
     * Retorna el jugador oponent.
     * 
     * @param player Jugador actual
     * @return El jugador contrari
     */
    private PlayerType getOpponent(PlayerType player) {
        return player == PlayerType.PLAYER1 ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
    }

    /**
     * Genera un moviment aleatori com a mesura de seguretat.
     * S'utilitza quan no s'ha pogut trobar cap moviment vàlid.
     * 
     * @param s Estat del joc
     * @return Moviment aleatori vàlid
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
     * Classe interna per emmagatzemar el resultat d'una cerca Minimax.
     * Encapsula el camí del moviment i la seva avaluació.
     */
    private class MoveResult {
        /** Seqüència de punts que representen el moviment */
        List<Point> path;
        /** Valor heurístic del moviment */
        int evaluation;

        /**
         * Constructor del resultat d'un moviment.
         * 
         * @param path Camí del moviment (seqüència de punts)
         * @param evaluation Avaluació heurística del moviment
         */
        MoveResult(List<Point> path, int evaluation) {
            this.path = path;
            this.evaluation = evaluation;
        }
    }
}
