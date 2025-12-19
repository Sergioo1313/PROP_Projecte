/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.upc.epsevg.prop.oust;

/**
 *
 * @author bernat
 */
public class Proves {
        public static void main(String[] args) {

            short test = -1;
                System.out.println("\u001B[31m Line 1\u001B[0m");
            
            short g = (short)0x0FFF;            
            g = (short) (g<< 4);
            System.out.println("Grup"+0x0FFF+":"+g);//Grup4095:65520

            //return (val & 0xFFF0)>>> 4;
            g=(short)((g & 0xFFF0)>>> 4);
            System.out.println("Grup"+0x0FFF+":"+g);//Grup4095:65520
            
            System.out.println((-1 >> 15) | 1);
            System.out.println((-1231 >> 15) | 1);
            System.out.println((1231 >> 15) | 1);
            System.out.println(">>>>>>>>>");
                    
            System.out.println((-1 >>> 15) & 1);
            System.out.println((-20 >>> 15) & 1);
            System.out.println( (1 >>> 15) & 1);

        }

}
