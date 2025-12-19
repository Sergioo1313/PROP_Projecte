package edu.upc.epsevg.prop.oust;


import java.awt.Point;
/**
 *
 * @author bernat
 */
public class UnitTesting {
    
    
    
    public static void main(String[] args) {
        //game1();
        game2();
    }

    private static void game1() {
        GameStatus gs = new GameStatus(4);
        System.out.println(""+gs);
        
        gs.placeStone(new Point(1,2));System.out.println(""+gs);
        gs.placeStone(new Point(5,3));System.out.println(""+gs);
        gs.placeStone(new Point(4,2));System.out.println(""+gs);
        gs.placeStone(new Point(4,1));System.out.println(""+gs);
        gs.placeStone(new Point(3,2));System.out.println(""+gs);
    }
    
    private static void game2() {
        GameStatus gs = new GameStatus(4);
        System.out.println(""+gs);
        
        gs.placeStone(new Point(1,2));System.out.println(""+gs);
        gs.placeStone(new Point(5,3));System.out.println(""+gs);
        gs.placeStone(new Point(4,2));System.out.println(""+gs);
        gs.placeStone(new Point(4,1));System.out.println(""+gs);
        gs.placeStone(new Point(5,4));System.out.println(""+gs);
        gs.placeStone(new Point(5,2));System.out.println(""+gs);
        gs.placeStone(new Point(5,6));System.out.println(""+gs);
        gs.placeStone(new Point(4,4));System.out.println(""+gs);
        gs.placeStone(new Point(3,4));System.out.println(""+gs);
        gs.placeStone(new Point(6,4));System.out.println(""+gs);
        gs.placeStone(new Point(0,0));System.out.println(""+gs);
        gs.placeStone(new Point(5,5));System.out.println(""+gs);
        gs.placeStone(new Point(3,1));System.out.println(""+gs);
        
        gs.placeStone(new Point(4,2));System.out.println(""+gs);
        gs.placeStone(new Point(5,4));System.out.println(""+gs);
        gs.placeStone(new Point(1,1));System.out.println(""+gs);

    }
    
}
