package edu.upc.epsevg.prop.oust;

import edu.upc.epsevg.prop.oust.players.HumanPlayer;
import edu.upc.epsevg.prop.oust.players.RandomPlayer;
import edu.upc.epsevg.prop.oust.players.MinimaxPlayer;
import edu.upc.epsevg.prop.oust.players.IDSPlayer;


import javax.swing.SwingUtilities;

/**
 * Oust: el joc de taula.
 * @author Bernat
 */
public class Game {
    /**
     * @param args no usats
     */
    public static void main(String[] args) { 
         
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                //---------------------------------------------
                // Un parell de tontets jugant 
                //---------------------------------------------
                //IPlayer player1 = new RandomPlayer("Asterix");
                //IPlayer player2 = new RandomPlayer("Obelix");
                
                
                
                //---------------------------------------------
                // Deixem el tontet en mans d'una mala persona
                //---------------------------------------------
                //IPlayer player1 = new RandomPlayer("Asterix");
                //IPlayer player2 = new MalaOustiaPlayer();
                
                
                //---------------------------------------------
                // Enjoy!
                //---------------------------------------------
                IPlayer player1 = new MOustValuablePlayer();
                IPlayer player2 = new MinimaxPlayer(11);
                                
                
                //---------------------------------------------
                // Customitzeu els paràmetres
                //---------------------------------------------
                int midaCostat = 7;
                int timeoutEnSegons = 5;
                boolean pauseEnAutomatic = false;
                
                new Board(player1 , player2, midaCostat /*mida*/,  timeoutEnSegons/*s timeout*/, pauseEnAutomatic);
             }
        });
    }
}
