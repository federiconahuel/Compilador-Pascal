package AnalizadorLexico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Permite ejecutar el analizador léxico, y visualizar por pantalla los tokens reconocidos.
 * Su única finalidad es facilitar el testing del analizador léxico.
 * El código fuente a analizar debe ser recibido como parámetro de "main()"
 */
public class LexicoTester {

    public static void main(String[] args) {

        Token tokenActual;
        AnalizadorLexico analizadorLex;
        File input;
        BufferedReader programa;
        int counter = 1;
    
        
        if(args.length != 1){
            System.out.println("Cantidad de parámetros inadmisible");
        }
        
        else{

            try {
                input = new File(args[0]);
                programa = new BufferedReader(new FileReader(input));
                
                analizadorLex = new AnalizadorLexico(programa);

                System.out.println("\n"+"//////TOKENS RECONOCIDOS//////"+"\n");

                System.out.println("\n" + "// <NombreToken - ValorAtributo> //" + "\n");

                do {                   
                    tokenActual = analizadorLex.obtenerToken();                  
                    if (!tokenActual.getNombreToken().equals("FIN")) {
                        System.out.println(counter + ") < " + tokenActual.getNombreToken() + " - " + tokenActual.getAtributo() + " >");
                        counter++;
                    }
                } while (!tokenActual.getNombreToken().equals("FIN"));

                if(analizadorLex.huboError())
                    System.out.println("\n" + "//////RECONOCIMIENTO DE TOKENS INTERRUMPIDO//////");
                else
                    System.out.println("\n" + "//////FIN DE RECONOCIMIENTO DE TOKENS//////");
                
                } catch (FileNotFoundException ex) {
                    System.out.println("No se puede encontrar el archivo especificado");
                }                
                
            
            }
            

        

    }
}
