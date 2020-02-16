package AnalizadorLexico;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

/*
 * Este módulo implementa el analizador lexico que reconooce tokens para el lenguaje subconjunto de Pascal
 */
public class AnalizadorLexico {

    private final BufferedReader programa;
    private int ultimoCaracterLeido; //cuando vale -1 indica que es el fin de archivo
    private final HashMap<String, Token> palabrasReservadas; //almacena las palabras reservadas del lenguaje
    private int contadorLineas; //indica que linea del archivo esta siendo leida actualmente
    private boolean readNextChar;//Si es "true", indica que se debe leer un nuevo caracter (por ejemplo si leyó '<' y luego 'a', no debe leer nuevo caracter)
    //private boolean hayPuntoFinal; //si es "true", significa que ya se ha leido el punto que denota el fin del programa 
    private boolean procesandoWrite;//si es "true", indica que se está leyendo el parámetro de la función "write()" (siempre y cuando sea texto)
    //private boolean procesandoComentario;//si es "true", indica que se está leyendo un comentario
    private boolean huboError; // es "true" cuando se produjo alguno de los siguientes errores: caracter invalido, comentario sin fin o excede fin de programa  ;

    /*
     * AnalizadorLexico:  constructor del analizador lexico, creando hasta las palabras reservadas
     */
    public AnalizadorLexico(BufferedReader fileIn) {

        programa = fileIn;
        ultimoCaracterLeido = 9999;
        contadorLineas = 1;
        readNextChar = true; // 
        //hayPuntoFinal = false; 	
        palabrasReservadas = new HashMap();
        this.inicializarPalabrasReservadas(palabrasReservadas);
        procesandoWrite = false;
        //procesandoComentario = false; 
        huboError = false;
    }

    public String getContadorLineas() {
        String a;
        if (contadorLineas < 10) {
            a = "0" + contadorLineas;
        } else {
            a = "" + contadorLineas;
        }
        return a;
    }

    public boolean huboError() {
        return huboError;
    }

    private void inicializarPalabrasReservadas(HashMap palabrasReservadas) {

        palabrasReservadas.put("and", new Token("AND", "", "and"));
        palabrasReservadas.put("begin", new Token("BEGIN", "", "begin"));
        palabrasReservadas.put("boolean", new Token("BOOLEAN", "", "boolean"));
        palabrasReservadas.put("do", new Token("DO", "", "do"));
        palabrasReservadas.put("else", new Token("ELSE", "", "else"));
        palabrasReservadas.put("end", new Token("END", "", "end"));
        palabrasReservadas.put("false", new Token("FALSE", "", "false"));
        palabrasReservadas.put("function", new Token("FUNCTION", "", "function"));
        palabrasReservadas.put("if", new Token("IF", "", "if"));
        palabrasReservadas.put("integer", new Token("INTEGER", "", "integer"));
        palabrasReservadas.put("not", new Token("NOT", "", "not"));
        palabrasReservadas.put("or", new Token("OR", "", "or"));
        palabrasReservadas.put("procedure", new Token("PROCEDURE", "", "procedure"));
        palabrasReservadas.put("program", new Token("PROGRAM", "", "program"));
        palabrasReservadas.put("read", new Token("READ", "", "read"));
        palabrasReservadas.put("then", new Token("THEN", "", "then"));
        palabrasReservadas.put("true", new Token("TRUE", "", "true"));
        palabrasReservadas.put("var", new Token("VAR", "", "var"));
        palabrasReservadas.put("while", new Token("WHILE", "", "while"));
        palabrasReservadas.put("write", new Token("WRITE", "", "write"));

    }

    //!caracterInvalido && !hayPuntoFinal && ultimoCaracterLeido != -1 
    /*
     * obtenerToken: retorna el token posterior al último token leido previamente. 
     */
    public Token obtenerToken() {

        String aux;

        //Si todavia no se alcanzo el punto final, tampoco el final del archivo y no se han detectado errores léxicos, se vuelve a iterar
        try {
            //while (!huboError && ultimoCaracterLeido != -1 && !hayPuntoFinal) {
            while (!huboError && ultimoCaracterLeido != -1) {

                if (readNextChar) {
                    ultimoCaracterLeido = programa.read();
                } else {
                    readNextChar = true;
                }

                if (ultimoCaracterLeido != ' ' && ultimoCaracterLeido != '\t' && ultimoCaracterLeido != '\r') {

                    switch (ultimoCaracterLeido) {

                        //Reconocimiento de símbolos especiales
                        case '.':
                            //hayPuntoFinal = true;
                            ultimoCaracterLeido = programa.read();
                            return new Token("PUNTO", "", ".");

                        case '_':
                            return new Token("GUION BAJO", "", "_");

                        case '(':
                            return new Token("PARENTESIS_APERTURA", "", "(");

                        case ')':
                            return new Token("PARENTESIS_CIERRE", "", ")");

                        case ',':
                            return new Token("COMA", "", ",");

                        case '[':
                            return new Token("CORCHETE_APERTURA", "", "[");

                        case ']':
                            return new Token("CORCHETE_CIERRE", "", "]");

                        case ';':
                            return new Token("PUNTO_COMA", "", ";");

                        case '\'':
                            if (!this.procesandoWrite) {
                                consumirTexto();
                                this.procesandoWrite = true;
                                this.readNextChar = false;
                            } else {
                                this.procesandoWrite = false;
                                //this.readNextChar = true;
                            }
                            return new Token("COMILLA_SIMPLE", "", "\'");

                        case ':':
                            ultimoCaracterLeido = programa.read();
                            if (ultimoCaracterLeido == '=') {
                                return new Token("ASIGNACION", "", ":=");
                            } else {
                                readNextChar = false;
                                return new Token("DOS_PUNTOS", "", ":");
                            }

                        case '\n':
                            contadorLineas++;//aumento el contador de lineas
                            break;

                        //Reconocimiento de comentarios    
                        case '{':
                            /*if(!this.procesandoComentario){
                             this.procesandoComentario = true;
                             this.readNextChar = false;
                             return new Token("LLAVE_APERTURA", "");
                             }*/
                            //else{
                            //this.readNextChar = true;
                            descartarComentario();
                            //this.procesandoComentario = false;
                            break;

                        case '}':
                            //this.readNextChar = true;
                            //return new Token("LLAVE_CIERRE", "");
                            this.huboError = true;
                            System.out.println("\n" + "COMENTARIO CERRADO SIN ABRIR EN LINEA " + this.contadorLineas);
                            break;

                        //Reconocimiento de operadores aritmeticos
                        case '-':
                            return new Token("OP_ARIT_MENOR_PRECEDENCIA", "OPERADOR_RESTA", "-");

                        case '+':
                            return new Token("OP_ARIT_MENOR_PRECEDENCIA", "OPERADOR_SUMA", "+");

                        case '*':
                            return new Token("OP_ARIT_MAYOR_PRECEDENCIA", "OPERADOR_MULTIPLICACION", "*");

                        case '/':
                            return new Token("OP_ARIT_MAYOR_PRECEDENCIA", "OPERADOR_DIVISION", "/");

                        //Reconocimiento de operadores relacionales
                        case '<':
                            ultimoCaracterLeido = programa.read();
                            if (ultimoCaracterLeido == '=') {
                                return new Token("OP_RELACIONAL", "OPERADOR_MENOR_IGUAL", "<=");
                            } else {
                                if (ultimoCaracterLeido == '>') {
                                    return new Token("OP_RELACIONAL", "OPERADOR_DISTINTO", "<>");
                                } else {
                                    readNextChar = false;
                                    return new Token("OP_RELACIONAL", "OPERADOR_MENOR", "<");
                                }
                            }

                        case '=':
                            return new Token("OP_RELACIONAL", "OPERADOR_IGUAL", "=");

                        case '>':
                            ultimoCaracterLeido = programa.read();
                            if (ultimoCaracterLeido == '=') {
                                return new Token("OP_RELACIONAL", "OPERADOR_MAYOR_IGUAL", ">=");
                            } else {
                                readNextChar = false;
                                return new Token("OP_RELACIONAL", "OPERADOR_MAYOR", ">");
                            }

                        default:
                            //Reconocimiento de números    

                            if (ultimoCaracterLeido >= '0' && ultimoCaracterLeido <= '9') {
                                aux = leerNumero();
                                readNextChar = false;
                                return new Token("NUMERO_ENTERO", "", aux);
                            } else {
                                //Reconocimiento de identificadores (palabras reservadas inclusive)
                                if (((ultimoCaracterLeido >= 'A') && (ultimoCaracterLeido <= 'Z')) || ((ultimoCaracterLeido >= 'a') && (ultimoCaracterLeido <= 'z'))) {

                                    aux = leerIdentificador().toLowerCase();
                                    readNextChar = false;

                                    if (this.palabrasReservadas.containsKey(aux.toLowerCase())) {
                                        return this.palabrasReservadas.get(aux.toLowerCase());

                                    } else {
                                        return new Token("IDENTIFICADOR", "", aux);
                                    }
                                } else if (ultimoCaracterLeido != -1) {

                                    //El caracter leido no pertenece al alfabeto del lenguaje
                                    //this.caracterInvalido = true;
                                    this.huboError = true;
                                    System.out.println("\n" + "CARACTER INVALIDO " + (char) ultimoCaracterLeido + " EN LINEA " + this.contadorLineas);
                                }
                            }
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Error de lectura del archivo");
            this.huboError = true;
        }

        /*if (hayPuntoFinal) {

         //Para la detección de caracteres "visibles" posteriores al final del final del programa
         while (ultimoCaracterLeido == ' ' || ultimoCaracterLeido == '\n' || ultimoCaracterLeido == '\t' || ultimoCaracterLeido == '\r') {
         ultimoCaracterLeido = programa.read();
         if(ultimoCaracterLeido == '\n')
         this.contadorLineas++;
         }
         if (ultimoCaracterLeido != -1) {
         //this.excedePrograma = true;
         System.out.println("\n" +"PROGRAMA EXCEDIDO EN LINEA " + this.contadorLineas);
         this.huboError = true;

         }
         }*/
        return new Token("FIN", "", "");
    }

    /*
     * "leerNumero" retorna un String conformado por una secuencia de dígitos. Al encontrar un caracter que no es un digito,
     *  el metodo finaliza.
     */
    private String leerNumero() throws IOException {
        String aux = "";
        while ((ultimoCaracterLeido >= '0') && (ultimoCaracterLeido <= '9')) {
            aux = aux + (char) ultimoCaracterLeido;
            ultimoCaracterLeido = programa.read();
        }

        return aux;
    }


    /*
     * "leerIdentificador" retorna un String conformado por una secuencia de caracteres válidos que comienza con una letra y que continúa con '_', letras o bien digitos. 
     */
    private String leerIdentificador() throws IOException {
        String aux = "";
        while ((ultimoCaracterLeido != -1) && (((ultimoCaracterLeido >= 'A') && (ultimoCaracterLeido <= 'Z')) || ((ultimoCaracterLeido >= 'a') && (ultimoCaracterLeido <= 'z'))
                || (ultimoCaracterLeido == '_') || ((ultimoCaracterLeido >= '0') && (ultimoCaracterLeido <= '9')))) {
            aux = aux + (char) ultimoCaracterLeido;
            ultimoCaracterLeido = programa.read();
        }
        return aux;
    }


    /*
     * "descartarComentario" simplemente recorre los caracteres entre '{' y '}' sin realizar otra acción. Si se alcanza el caracter de fin de archivo sin 
     * previamente haber encontrado un '}', se detiene la ejecución del método.  
     */
    private void descartarComentario() throws IOException {
        int iniComentarioSinCerrar = this.contadorLineas;
        while ((ultimoCaracterLeido != '}') && (ultimoCaracterLeido != -1)) {
            if (ultimoCaracterLeido == '\n') {
                contadorLineas++;
            }
            ultimoCaracterLeido = programa.read();
        }
        if (ultimoCaracterLeido == -1) {
            //this.comentarioSinFin = true;
            System.out.println("\n" + "COMENTARIO SIN FIN INICIADO EN LINEA " + iniComentarioSinCerrar);
            this.huboError = true;
        }
        /*else{
         this.readNextChar = false;
         }*/
    }

    /*
     * "consumirTexto" cumple la misma función que "descartarComentario", pero se utiliza para el caso de lectura de texto entre comillas simples en las operaciones
     * "write"
     */
    private void consumirTexto() throws IOException {
        int iniTexto = this.contadorLineas;
        ultimoCaracterLeido = programa.read();
        while ((ultimoCaracterLeido != '\'') && (ultimoCaracterLeido != -1) && (ultimoCaracterLeido != '\n')) {
            ultimoCaracterLeido = programa.read();
        }
        if (ultimoCaracterLeido == '\n') {
            System.out.println("\n" + "STRING EXCEDE LINEA EN LINEA " + iniTexto);
            this.huboError = true;
        }

    }

}
