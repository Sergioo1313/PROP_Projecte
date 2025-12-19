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
import java.util.Random;

/**
 * Jugador aleatori
 * @author bernat
 */
public class RandomPlayer implements IPlayer, IAuto {

    private String name;
    

    public RandomPlayer(String name) {
        this.name = name;
    }

    @Override
    public void timeout() {
        // Nothing to do! I'm so fast, I never timeout 8-)
        
    }

    /**
     * Decideix el moviment del jugador donat un tauler i un color de peça que
     * ha de posar.
     *
     * @param s Tauler i estat actual de joc.
     * @return el moviment que fa el jugador.
     */
    @Override
    public PlayerMove move(GameStatus s1) {

        
        List<Point> path = new ArrayList<>();
        PlayerType currentPlayer = s1.getCurrentPlayer();
        GameStatus aux = new GameStatus(s1);
        
        do {
            
            List<Point> moves = aux.getMoves();
            if(moves.size()==0) break;

            Random rand = new Random();
            Point m = moves.get(rand.nextInt(moves.size()));
            aux.placeStone(m);
            path.add(m);
            
        } while(currentPlayer == aux.getCurrentPlayer());
            
        return new PlayerMove(path,0,0,SearchType.RANDOM);        
    }

    /**
     * Ens avisa que hem de parar la cerca en curs perquè s'ha exhaurit el temps
     * de joc.
     */
    @Override
    public String getName() {
        return "Random(" + name + ")";
    }

}
