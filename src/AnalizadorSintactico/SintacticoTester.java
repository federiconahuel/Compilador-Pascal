/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AnalizadorSintactico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nahuel
 */
public class SintacticoTester {

    public static void main(String[] args) {

        File input;
        BufferedReader programa;
        //int counter = 1;
        AnalizadorSintactico sint;

        if (args.length != 1) {
            //if (false) {
            System.out.println("Cantidad de parámetros inadmisible");
        } else {

            try {
                System.out.println("******************************************************************");
                System.out.println("*                           ACLARACIÓN                           *");
                System.out.println("* El archivo de entrada debe incluir la extensión. Por ej: p.pas *");
                System.out.println("******************************************************************");
                System.out.println("");
                System.out.println("*******************************************************");
                System.out.println("*       Compilador para Subconjunto de PASCAL         *");
                System.out.println("* Desarrollado por Alvarez Candelaria y Mamani Nahuel *");
                System.out.println("*******************************************************");
                
                
                String nombreArchivoEntrada = args[0];
                input = new File(nombreArchivoEntrada);
                //input = new File(System.getProperty("user.dir") + "/src/AnalizadorLexico/p.pas");
                programa = new BufferedReader(new FileReader(input));
                sint = new AnalizadorSintactico(programa, nombreArchivoEntrada);

                try {
                    sint.programa();
                } catch (IOException ex) {
                }

                if (sint.huboErrorSintactico()) {
                    System.out.println("\n" + "//////ANALISIS SINTÁCTICO ABORTADO//////");
                    sint.borrarArchivo();
                } else {
                    // System.out.println("\n" +"//////ANALISIS SINTÁCTICO FINALIZADO EXITOSAMENTE//////");
                    if (!sint.huboErrorSemantico()) {
                        //  System.out.println("\n //////ANALISIS SEMÁNTICO FINALIZADO. NO SE HAN DETECTADO ERRORES SEMÁNTICOS//////");
                        String aux = sint.getPath();
                        System.out.println("CÓDIGO INTERMEDIO GENERADO EXITOSAMENTE EN \n" + aux);
                    } else {
                        System.out.println("\n //////ANALISIS SEMÁNTICO FINALIZADO CON ERRORES SEMÁNTICOS DETECTADOS//////");
                        sint.borrarArchivo();

                    }
                }
            } catch (FileNotFoundException ex) {
                System.out.println("No se puede encontrar el archivo especificado");
            }

        }
    }
}
