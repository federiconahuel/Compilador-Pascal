package AnalizadorSintactico;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorLexico.Token;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.*;
import java.util.ArrayList;

public class AnalizadorSintactico {

    private final AnalizadorLexico lex;
    private Token preanalisis;
    private boolean huboErrorSintactico;
    private boolean huboErrorSemantico;
    private TablaSimbolos tablaActual;
    private String nroLinea;
    private String path;
    private String nombreArchivoEntrada;

    private int nivelAnidamiento;
    private int contadorLabels;

    private BufferedWriter writer;
    private File archivo;

    public AnalizadorSintactico(BufferedReader fileIn, String name) {
        lex = new AnalizadorLexico(fileIn);
        huboErrorSintactico = false;
        huboErrorSemantico = false;
        tablaActual = new TablaSimbolos(null, 0);
        nroLinea = "01";
        nombreArchivoEntrada = name;
        nivelAnidamiento = 0;
        contadorLabels = 0;

        try {
            int posPunto = nombreArchivoEntrada.indexOf(".");
            String aux = nombreArchivoEntrada.substring(0, posPunto);
            //System.out.println(aux);
            path = (System.getProperty("user.dir") + "/" + aux +".mep");
            archivo = new File(path);
            writer = new BufferedWriter(new FileWriter(archivo));
        } catch (IOException ex) {
        }

    }

    public boolean huboErrorSintactico() {
        return huboErrorSintactico;
    }

    public boolean huboErrorSemantico() {
        return huboErrorSemantico;
    }

    public String getPath() {
        return this.path;
    }

    void match(String t, String cartel) throws ErrorSintactico, ErrorLexico {
        nroLinea = lex.getContadorLineas();
        String tokenActual = preanalisis.getNombreToken();

        //Si el nombre del token esperado es igual al del token recibido, se le pide el siguiente token al analizador
        //lexico. En caso contrario, existe error sintactico.
        if (tokenActual.equals(t)) {

            preanalisis = lex.obtenerToken();

            //Si el analizador léxico detectó error léxico, se lanza una excepcion de tipo "ErrorLexico"
            //En programa() se atrapa esta excepcion y se detiene el analisis sintactico
            if (lex.huboError()) {
                throw new ErrorLexico();
            }

        } else {

            System.out.print("ERROR DE SINTAXIS EN LINEA N° " + nroLinea + ": ");

            //Si el error sintactico del tipo 1 (es decir: FIN DE PROGRAMA INESPERADO). Ver informe
            switch (tokenActual) {

                case ("PUNTO"):
                    System.out.println("FIN DE PROGRAMA INESPERADO: Se encontró un punto que NO es el final" + "\n");
                    break;

                case ("FIN"):
                    System.out.println("FIN DE ARCHIVO INESPERADO: Se alcanzó el final del programa fuente antes de lo esperado" + "\n");
                    break;

                //Para los demas tipos de errores        
                default:

                    switch (t) {

                        case ("PARENTESIS_VACIOS"):
                            System.out.println("INVOCACIÓN ERRÓNEA: Se realizó una invocación de la función/procedimiento \'" + cartel + "\' con paréntesis vacíos () ");
                            break;

                        case ("NUMERO_ENTERO"):
                            System.out.println("EXPRESION ILEGAL");
                            break;

                        default:
                            System.out.print("Se esperaba " + cartel + " pero se encontró \'" + "" + preanalisis.getLexema() + "\'" + "\n");

                            break;
                    }

                    break;
            }

            throw new ErrorSintactico();

        }
    }

    void programa() throws IOException {

        String nombrePrograma;

        try {
            preanalisis = lex.obtenerToken();
            match("PROGRAM", "la palabra reservada PROGRAM");

            //Inicializacion de programa en MEPA
            writer.write("INPP" + "\n");

            nombrePrograma = preanalisis.getLexema();
            match("IDENTIFICADOR", "el IDENTIFICADOR del nombre del programa");
            //tablaActual.insertarElemento(nombrePrograma, "PROGRAM", null, null);
            match("PUNTO_COMA", ";");
            Declaraciones();
            Sentencia_comp();
            match("PUNTO", ".");
            writer.write("LMEM " + this.tablaActual.getCantidadVariables() + "\n");
            writer.write("PARA");
            writer.close();

        } //Si se detecta un error sintactico o lexico durante el analisis sintactico, este es abortado.
        catch (ErrorSintactico | ErrorLexico ex) {
            huboErrorSintactico = true;
            writer.close();

        }
    }

    void borrarArchivo() {
        archivo.delete();
    }

    void Declaraciones() throws ErrorSintactico, ErrorLexico, IOException {

        int aux;

        while (preanalisis.getNombreToken().equals("VAR")) {
            //System.out.println("entra al while con " + preanalisis.getLexema());
            Declaracion_variables();
        }

        //System.out.println("sale del while con " + preanalisis.getLexema());
        if (preanalisis.getNombreToken().equals("FUNCTION") || preanalisis.getNombreToken().equals("PROCEDURE")) {

            nivelAnidamiento++;
            contadorLabels++;
            aux = contadorLabels;
            writer.write("DSVS L" + aux + "\n");

            Declaracion_func_subprograma();

            while (preanalisis.getNombreToken().equals("FUNCTION") || preanalisis.getNombreToken().equals("PROCEDURE")) {
                Declaracion_func_subprograma();
            }

            writer.write("L" + aux + " NADA" + "\n");
            nivelAnidamiento--;
        }
    }

    void Declaracion_func_subprograma() throws ErrorSintactico, ErrorLexico, IOException {

        TablaSimbolos newTabla = new TablaSimbolos(this.tablaActual, nivelAnidamiento);
        int cantParametros, aux;
        //boolean isFunction = false;

        contadorLabels++;
        aux = contadorLabels;
        writer.write("L" + aux + " NADA" + "\n");
        writer.write("ENPR " + nivelAnidamiento + "\n");

        if (preanalisis.getNombreToken().equals("FUNCTION")) {
            //identificador = 
            Cab_func(newTabla);
            cantParametros = newTabla.cantPares() - 1;
            //isFunction = true;
        } else {
            //identificador = 
            Cab_proc(newTabla);
            cantParametros = newTabla.cantPares();
        }

        tablaActual = newTabla; //Se crea la TS para el nuevo ambiente
        Declaraciones();
        Sentencia_comp();
        match("PUNTO_COMA", ";");

        //if(isFunction)
        //    writer.write("LMEM " + (tablaActual.getCantidadVariables()+1) + "\n");
        //else
            writer.write("LMEM " + tablaActual.getCantidadVariables() + "\n");
        
        writer.write("RTPR " + nivelAnidamiento + "," + cantParametros + "\n");
        //System.out.println(tablaActual.toString());
        tablaActual = newTabla.getTablaPadre(); //Se elimina el ambiente actual
        //writer.write("L"+aux+" NADA");
        //writer.newLine(); 
    }

    void Cab_func(TablaSimbolos newTabla) throws ErrorSintactico, ErrorLexico, IOException {

        String nombreFuncion, tipo;
        ArrayList tipoDatoArgs = null;

        match("FUNCTION", "la palabra reservada FUNCTION");
        nombreFuncion = preanalisis.getLexema();
        match("IDENTIFICADOR", "el IDENTIFICADOR del nombre de la función");

        if (preanalisis.getNombreToken().equals("PARENTESIS_APERTURA")) {
            match("PARENTESIS_APERTURA", "(");
            tipoDatoArgs = Param_Formales(newTabla, nombreFuncion);
            match("PARENTESIS_CIERRE", ")");
        }
        match("DOS_PUNTOS", ":");

        if (preanalisis.getNombreToken().equals("INTEGER")) {
            match("INTEGER", "el tipo de dato de retorno INTEGER");
            tipo = "INTEGER";
        } else {
            match("BOOLEAN", "el tipo de dato de retorno BOOLEAN/INTEGER");
            tipo = "BOOLEAN";
        }

        //Se inserta en la tabla de símbolos de la función la variable de retorno de la misma
        //newTabla.insertarElemento(nombreFuncion, "VAR_FUNC", tipo, tipoDatoArgs);
        newTabla.insertarElemento(nombreFuncion, "VAR_FUNC", tipo, tipoDatoArgs, -(newTabla.cantPares() + 3), contadorLabels);

        if (!this.tablaActual.checkSymbolExistence(nombreFuncion, false)) {
            this.tablaActual.insertarElemento(nombreFuncion, "FUNCTION", tipo, tipoDatoArgs, 0, contadorLabels);
        } else {
            huboErrorSemantico = true;
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.nroLinea + ": EL IDENTIFICADOR \'" + nombreFuncion + "\' YA FUE DECLARADO PREVIAMENTE");
        }

        match("PUNTO_COMA", ";");

        //return nombreFuncion;
    }

    void Cab_proc(TablaSimbolos newTabla) throws ErrorSintactico, ErrorLexico {

        ArrayList tipoDatoArgs = null;
        String nombreProc;
        match("PROCEDURE", "la palabra reservada PROCEDURE");
        nombreProc = this.preanalisis.getLexema();
        match("IDENTIFICADOR", "el IDENTIFICADOR del nombre del procedimiento");
        if (preanalisis.getNombreToken().equals("PARENTESIS_APERTURA")) {
            match("PARENTESIS_APERTURA", "(");
            tipoDatoArgs = Param_Formales(newTabla, nombreProc);//En procedimientos no es necesario chequear que ninguno de los param. formales tenga el mismo nombre que el proc.
            match("PARENTESIS_CIERRE", ")");
        }
        match("PUNTO_COMA", ";");
        if (!this.tablaActual.checkSymbolExistence(nombreProc, false)) {
            this.tablaActual.insertarElemento(nombreProc, "PROCEDURE", null, tipoDatoArgs, 0, contadorLabels);
        } else {
            huboErrorSemantico = true;
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.nroLinea + ": EL IDENTIFICADOR \'" + nombreProc + "\' YA FUE DECLARADO PREVIAMENTE");
        }

        //return nombreProc;
    }

    void Declaracion_variables() throws ErrorSintactico, ErrorLexico, IOException {

        String idActual, tipo;
        ArrayList<String> identificadores;
        int cantVariables = 0, i;

        match("VAR", "la palabra reservada VAR");
        do {
            i = 0;

            identificadores = new ArrayList();
            idActual = preanalisis.getLexema();
            identificadores.add(idActual);
            match("IDENTIFICADOR", "un IDENTIFICADOR de variable");
            while (preanalisis.getNombreToken().equals("COMA")) {
                match("COMA", ",");
                idActual = preanalisis.getLexema();
                match("IDENTIFICADOR", "un IDENTIFICADOR de variable");
                identificadores.add(idActual);
            }
            match("DOS_PUNTOS", ":");

            if (preanalisis.getNombreToken().equals("INTEGER")) {
                match("INTEGER", "el tipo de dato INTEGER");
                tipo = "INTEGER";
            } else {
                match("BOOLEAN", "el tipo de dato BOOLEAN/INTEGER");
                tipo = "BOOLEAN";
            }

            //for (int i = 0; i < identificadores.size(); i++) 

            while (i < identificadores.size()) {

                idActual = identificadores.get(i);

                if (!this.tablaActual.checkSymbolExistence(idActual, false)) {
                    this.tablaActual.insertarElemento(idActual, "VARIABLE", tipo, null, cantVariables + i, 0);
                    i++;
                } else {
                    identificadores.remove(i);
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.nroLinea + ": LA VARIABLE \'" + idActual + "\' YA FUE DECLARADA PREVIAMENTE");
                }
            }

            cantVariables = cantVariables + (identificadores.size());
            match("PUNTO_COMA", ";");

        } while (preanalisis.getNombreToken().equals("IDENTIFICADOR"));

        this.tablaActual.setCantidadVariables(cantVariables);

        writer.write("RMEM " + cantVariables + "\n");

    }

    ArrayList Param_Formales(TablaSimbolos newTabla, String nombreFuncProc) throws ErrorSintactico, ErrorLexico {
        ArrayList typeArgs = new ArrayList();
        ArrayList<String> identificadores = new ArrayList();
        ArrayList<String> tipos = new ArrayList();

        String idActual, tipo;
        int i, n, cantTotalVariables, cantActual;
        n = 0;

        do {
            i = 0;
            cantActual = 0;
            idActual = preanalisis.getLexema();
            match("IDENTIFICADOR", "un IDENTIFICADOR de parámetro formal");
            identificadores.add(idActual);
            cantActual++;

            while (preanalisis.getNombreToken().equals("COMA")) {
                match("COMA", ",");
                idActual = preanalisis.getLexema();
                match("IDENTIFICADOR", "un IDENTIFICADOR de parámetro formal");
                identificadores.add(idActual);
                cantActual++;
            }

            match("DOS_PUNTOS", ":");

            if (preanalisis.getNombreToken().equals("INTEGER")) {
                match("INTEGER", "el tipo de dato INTEGER");
                tipo = "INTEGER";
            } else {
                match("BOOLEAN", "el tipo de dato BOOLEAN/INTEGER");
                tipo = "BOOLEAN";
            }

            while (i < cantActual) {
                idActual = identificadores.get(i);
                if (!idActual.equals(nombreFuncProc)) {

                    if (newTabla.checkSymbolExistence(idActual, false)) {
                        //newTabla.insertarElemento(idActual, "VARIABLE", tipo, null);
                        identificadores.remove(i);
                        huboErrorSemantico = true;
                        System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": EL PARÁMETRO \'" + idActual + "\' FUE "
                                + "DECLARADO PREVIAMENTE EN LA LISTA DE PARAMETROS DE LA FUNCION/PROCEDIMIENTO \'" + nombreFuncProc + "\'");
                    } else {

                        tipos.add(tipo); //Agrego un tipo por cada identificador
                    }
                } else {
                    identificadores.remove(i);
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": EL PARÁMETRO \'" + idActual + "\' NO PUEDE TENER EL"
                            + "MISMO NOMBRE QUE LA FUNCIÓN/PROCEDIMIENTO AL QUE PERTENECE");
                }
                i++;
                typeArgs.add(tipo);
            }

            if (!preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
                match("PUNTO_COMA", ";");
            }

        } while (preanalisis.getNombreToken().equals("IDENTIFICADOR"));

        cantTotalVariables = identificadores.size();

        for (i = 1; i <= cantTotalVariables; i++) {
            idActual = identificadores.get(i - 1);
            newTabla.insertarElemento(idActual, "VARIABLE", tipos.get(i - 1), null, -(cantTotalVariables + 3 - i), 0);
        }

        if (!preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
            match("PUNTO_COMA", ";");
            identificadores.clear();
        }

        return typeArgs;

    }

    /* ArrayList Param_Formales(TablaSimbolos newTabla, String nombreFuncProc) throws ErrorSintactico, ErrorLexico {
     ArrayList typeArgs = new ArrayList();
     ArrayList<String> identificadores = new ArrayList();
     String idActual, tipo;
     int i, n, cantTotalVariables = 0;
     n= 0;

     do {
     i = 0;
     idActual = preanalisis.getLexema();
     match("IDENTIFICADOR", "un IDENTIFICADOR de parámetro formal");
     identificadores.add(idActual);

     while (preanalisis.getNombreToken().equals("COMA")) {
     match("COMA", ",");
     idActual = preanalisis.getLexema();
     match("IDENTIFICADOR", "un IDENTIFICADOR de parámetro formal");
     identificadores.add(idActual);
     }

     match("DOS_PUNTOS", ":");

     if (preanalisis.getNombreToken().equals("INTEGER")) {
     match("INTEGER", "el tipo de dato INTEGER");
     tipo = "INTEGER";
     } else {
     match("BOOLEAN", "el tipo de dato BOOLEAN/INTEGER");
     tipo = "BOOLEAN";
     }

     //for (int i = 0; i < identificadores.size(); i++) 
     while (i < identificadores.size()) {
     idActual = identificadores.get(i);
     if (!idActual.equals(nombreFuncProc)) {

     if (newTabla.checkSymbolExistence(idActual, false)) {
     //newTabla.insertarElemento(idActual, "VARIABLE", tipo, null);
     identificadores.remove(i);
     huboErrorSemantico = true;
     System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": EL PARÁMETRO \'" + idActual + "\' FUE "
     + "DECLARADO PREVIAMENTE EN LA LISTA DE LISTA DE PARAMETROS DE LA FUNCION/PROCEDIMIENTO \'" + nombreFuncProc + "\'");
     } else {
     i++;
     }
     } else {
     identificadores.remove(i);
     huboErrorSemantico = true;
     System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": EL PARÁMETRO \'" + idActual + "\' NO PUEDE TENER EL"
     + "MISMO NOMBRE QUE LA FUNCIÓN/PROCEDIMIENTO AL QUE PERTENECE");
     }
     typeArgs.add(tipo);
     }

     //n =  identificadores.size();

     for (i = 1; i <= identificadores.size(); i++) {
     idActual = identificadores.get(i - 1);
     newTabla.insertarElemento(idActual, "VARIABLE", tipo, null, -(n + 3 - i), 0);
     n++;
     //newTabla.insertarElemento(idActual, "VARIABLE", tipo, null, -(n + 3 - i + cantTotalVariables), 0);
     }

     cantTotalVariables = cantTotalVariables + n;

     if (!preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
     match("PUNTO_COMA", ";");
     identificadores.clear();
     }

     } while (preanalisis.getNombreToken().equals("IDENTIFICADOR"));
     return typeArgs;

     }*/
    void Sentencia_comp() throws ErrorSintactico, ErrorLexico, IOException {
        match("BEGIN", "la palabra reservada BEGIN");
        Cuerpo();
        match("END", "la palabra reservada END");
    }

    void Cuerpo() throws ErrorSintactico, ErrorLexico, IOException {
        do {
            if (preanalisis.getNombreToken().equals("BEGIN")) {
                Sentencia_comp();
            } else {
                Sentencia();
            }

            if (!preanalisis.getNombreToken().equals("END")) {
                match("PUNTO_COMA", ";");
            }
        } while (!preanalisis.getNombreToken().equals("END"));
    }

    void Sentencia() throws ErrorSintactico, ErrorLexico, IOException {

        String aux;

        switch (preanalisis.getNombreToken()) {

            case "IDENTIFICADOR":
                aux = preanalisis.getLexema();
                match("IDENTIFICADOR", "un IDENTIFICADOR para comenzar la sentencia");
                if (preanalisis.getNombreToken().equals("ASIGNACION")) {
                    Aux_Asignacion(aux);
                } else {
                    Aux_Func_Proc_Call(aux);
                }
                break;

            case "WRITE":
            case "READ":
                Funciones_In_Out();
                break;

            case "IF":
            case "WHILE":
                Estructura_control();
                break;

            default:
                break;
        }
    }

    void Aux_Asignacion(String identificador) throws ErrorSintactico, ErrorLexico, IOException {
        String tipoLadoIzq, tipoLadoDer;

        match("ASIGNACION", "una  ASIGNACION :=");

        tipoLadoIzq = this.tablaActual.getTipoDeDato(identificador);
        tipoLadoDer = Expresion();

        if (this.tablaActual.getCategoria(identificador).equals("VARIABLE") || this.tablaActual.getCategoria(identificador).equals("VAR_FUNC")) {
            if (!tipoLadoIzq.equals(tipoLadoDer) && !tipoLadoDer.equals("ERROR")) {
                huboErrorSemantico = true;
                System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + " LA EXPRESIÓN DEL LADO DERECHO DEBE SER DE TIPO \'" + tipoLadoIzq
                        + "\' PERO SE ENCONTRÓ QUE ES DE TIPO \'" + tipoLadoDer + "\'");
            } else {

                writer.write("ALVL " + this.tablaActual.getProfundidad(identificador) + "," + this.tablaActual.getDesplazamiento(identificador) + "\n");
            }
        } else {
            huboErrorSemantico = true;
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": IDENTIFICADOR \'" + identificador + "\' NO FUE DEFINIDO PREVIAMENTE");

        }

    }

    String Aux_Func_Proc_Call(String identificador) throws ErrorSintactico, ErrorLexico, IOException {
        String tipo, aux;
        ArrayList tiposArgsInvocacion = null, tiposArgsEsperados, args;

        if (preanalisis.getNombreToken().equals("PARENTESIS_APERTURA")) {
            match("PARENTESIS_APERTURA", "(");
            if (preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
                match("PARENTESIS_VACIOS", identificador);
            }

            tiposArgsInvocacion = new ArrayList();
            //args = new ArrayList();
            writer.write("RMEM 1" + "\n");
            tipo = Expresion();
            tiposArgsInvocacion.add(tipo);

            while (preanalisis.getNombreToken().equals("COMA")) {
                match("COMA", ",");
                tipo = Expresion();
                tiposArgsInvocacion.add(tipo);
            }
            match("PARENTESIS_CIERRE", ")");
        }

        aux = this.tablaActual.isFuncProcCallValid(identificador, tiposArgsInvocacion);

        if (aux.equals("OK")) {
            tipo = this.tablaActual.getTipoDeDato(identificador);

            writer.write("LLPR L" + this.tablaActual.getEtiqueta(identificador) + "\n");
        } else {
            tipo = "ERROR";
            if (aux.equals("INEXISTENTE")) {
                huboErrorSemantico = true;
                System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": IDENTIFICADOR \'" + identificador + "\' NO FUE DEFINIDO PREVIAMENTE");
            } else {
                if ((tiposArgsInvocacion != null && !tiposArgsInvocacion.contains("ERROR")) || (tiposArgsInvocacion == null)) {
                    huboErrorSemantico = true;
                    System.out.print("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": ARGUMENTOS DE ENTRADA INCOMPATIBLES: SE ESPERABA (");
                    tiposArgsEsperados = this.tablaActual.getTiposDeDatoArgs(identificador);
                    if (tiposArgsEsperados != null) {
                        for (int i = 0; i < tiposArgsEsperados.size(); i++) {
                            System.out.print(tiposArgsEsperados.get(i));
                            if ((i + 1) < tiposArgsEsperados.size()) {
                                System.out.print(" ");
                            }
                        }
                    }
                    System.out.print(")");
                    if (tiposArgsInvocacion != null) {
                        System.out.print(" Y SE ENCONTRO (");
                        for (int i = 0; i < tiposArgsInvocacion.size(); i++) {
                            System.out.print(tiposArgsInvocacion.get(i));
                            if ((i + 1) < tiposArgsInvocacion.size()) {
                                System.out.print(" ");
                            }
                        }
                        System.out.print(") \n");
                    } else {
                        System.out.print("PERO NO SE ENCONTRARON ARGUMENTOS DE ENTRADA EN LA INVOCACIÓN \n");
                    }

                }

            }

        }

        return tipo;
    }

    void Funciones_In_Out() throws ErrorSintactico, ErrorLexico, IOException {
        if (preanalisis.getNombreToken().equals("WRITE")) {
            match("WRITE", "la palabra reservada WRITE");
            match("PARENTESIS_APERTURA", "(");
            //if (preanalisis.getNombreToken().equals("COMILLA_SIMPLE")) {
            //match("COMILLA_SIMPLE", "'");
            //El contenido entre comillas simples no se considera como token
            //match("COMILLA_SIMPLE", "'");

            //} else {
            Expresion();

            //}
            match("PARENTESIS_CIERRE", ")");
            writer.write("IMLN" + "\n");
        } else {
            match("READ", "la palabra reservada READ");
            match("PARENTESIS_APERTURA", "(");
            String id = preanalisis.getLexema();
            match("IDENTIFICADOR", "un IDENTIFICADOR de variable para su lectura");
            match("PARENTESIS_CIERRE", ")");
            writer.write("LEER" + "\n");
            writer.write("ALVL " + this.tablaActual.getProfundidad(id) + "," + this.tablaActual.getDesplazamiento(id) + "\n");
        }
    }

    void Estructura_control() throws ErrorSintactico, ErrorLexico, IOException {
        if (preanalisis.getNombreToken().equals("IF")) {
            Alternativa();
        } else {
            Repetitiva();
        }
    }

    void Alternativa() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoCondicion;
        int aux1, aux2;
        match("IF", "la palabra reservada IF");
        tipoCondicion = Expresion();

        if (tipoCondicion.equals("INTEGER")) {
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": LA EXPRESION USADA COMO CONDICIÓN EN \'IF\' NO ES DE TIPO \'BOOLEAN\'");
            huboErrorSemantico = true;
        }//else{

        contadorLabels++;
        writer.write("DSVF L" + contadorLabels + "\n");
        aux1 = contadorLabels;
        //}

        match("THEN", "la palabra reservada THEN");
        if (preanalisis.getNombreToken().equals("BEGIN")) {
            Sentencia_comp();
        } else {
            Sentencia();
        }
        contadorLabels++;
        aux2 = contadorLabels;
        writer.write("DSVS L" + aux2 + "\n");
        writer.write("L" + aux1 + " NADA" + "\n");
        //if(preanalisis.getNombreToken().equals("PUNTO_COMA"))
        //        match("PUNTO_COMA",";");
        if (preanalisis.getNombreToken().equals("ELSE")) {
            match("ELSE", "la palabra reservada ELSE");
            if (preanalisis.getNombreToken().equals("BEGIN")) {
                Sentencia_comp();
            } else {
                Sentencia();
            }
        }
        writer.write("L" + aux2 + " NADA" + "\n");
    }

    void Repetitiva() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoCondicion;
        int aux1, aux2;
        match("WHILE", "la palabra reservada WHILE");

        contadorLabels++;
        writer.write("L" + contadorLabels + " NADA" + "\n");
        aux1 = contadorLabels;

        tipoCondicion = Expresion();
        if (tipoCondicion.equals("INTEGER")) {
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + nroLinea + ": LA EXPRESION USADA COMO CONDICIÓN EN \'WHILE\' NO ES DE TIPO \'BOOLEAN\'");
            huboErrorSemantico = true;
        }
        //else{
        contadorLabels++;
        writer.write("DSVF L" + contadorLabels + "\n");
        aux2 = contadorLabels;
        //}
        match("DO", "la palabra reservada DO");
        if (preanalisis.getNombreToken().equals("BEGIN")) {
            Sentencia_comp();
        } else {
            Sentencia();
        }

        writer.write("DSVS L" + aux1 + "\n");
        writer.write("L" + aux2 + " NADA" + "\n");
    }

    String Expresion() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoOperando1, tipoOperando2, tipoExp;

        tipoOperando1 = subExpresion1();
        if (preanalisis.getNombreToken().equals("OR")) {
            match("OR", "el operador lógico OR");
            tipoOperando2 = Expresion();
            if (tipoOperando1.equals("BOOLEAN") && tipoOperando2.equals("BOOLEAN")) {
                tipoExp = "BOOLEAN";
                writer.write("DISJ" + "\n");
            } else {
                tipoExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'OR\' REQUIERE OPERADORES DE TIPO "
                            + "\'BOOLEAN\' Y \'BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoExp = tipoOperando1;
        }

        return tipoExp;
    }

    String subExpresion1() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoOperando1, tipoOperando2, tipoSubExp;

        tipoOperando1 = subExpresion2();
        if (preanalisis.getNombreToken().equals("AND")) {
            match("AND", "el operador lógico  AND");
            tipoOperando2 = subExpresion1();
            if (tipoOperando1.equals("BOOLEAN") && tipoOperando2.equals("BOOLEAN")) {
                tipoSubExp = "BOOLEAN";
                writer.write("CONJ" + "\n");
            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'AND\' REQUIERE OPERADORES DE TIPO "
                            + "\'BOOLEAN\' Y \'BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoSubExp = tipoOperando1;
        }
        return tipoSubExp;
    }

    String subExpresion2() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoOperando1, tipoOperando2, tipoSubExp = "ERROR", operador;
        tipoOperando1 = subExpresion3();
        if (preanalisis.getNombreToken().equals("OP_RELACIONAL")) {
            operador = preanalisis.getLexema();
            match("OP_RELACIONAL", "un operador relacional");
            tipoOperando2 = subExpresion2();

            //Si las expresiones coinciden en tipo pero NO son erroneas
            if (tipoOperando1.equals(tipoOperando2) && !tipoOperando1.equals("ERROR")) {
                if (tipoOperando1.equals("BOOLEAN") && !operador.equals("=")) {
                    tipoSubExp = "ERROR";

                } else {
                    tipoSubExp = "BOOLEAN";
                    switch (operador) {
                        case "<=":
                            writer.write("CMNI");
                            break;
                        case ">=":
                            writer.write("CMYI");
                            break;
                        case "<":
                            writer.write("CMME");
                            break;
                        case ">":
                            writer.write("CMMA");
                            break;
                        case "<>":
                            writer.write("CMDG");
                            break;
                        case "=":
                            writer.write("CMIG");
                            break;
                    }
                    writer.newLine();
                }
            }

            if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR") && tipoSubExp.equals("ERROR")) {
                switch (operador) {
                    case "=":
                        huboErrorSemantico = true;
                        System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                                + "\'INTEGER/BOOLEAN\' E \'INTEGER/BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                        break;

                    default:
                        huboErrorSemantico = true;
                        System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                                + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                        break;
                }

            }

        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String subExpresion3() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoOperando1, tipoOperando2, tipoSubExp, operador;

        tipoOperando1 = subExpresion4();
        if (preanalisis.getNombreToken().equals("OP_ARIT_MENOR_PRECEDENCIA")) {
            operador = preanalisis.getLexema();
            match("OP_ARIT_MENOR_PRECEDENCIA", "un operador aritmético (+,-)");
            tipoOperando2 = subExpresion3();
            if (tipoOperando1.equals("INTEGER") && tipoOperando2.equals("INTEGER")) {
                tipoSubExp = "INTEGER";
                if (operador.equals("+")) {
                    writer.write("SUMA");
                } else {
                    writer.write("SUST");
                }
                writer.newLine();

            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                            + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String subExpresion4() throws ErrorSintactico, ErrorLexico, IOException {
        String tipoOperando1, tipoOperando2, tipoSubExp, operador;

        tipoOperando1 = Factor();
        if (preanalisis.getNombreToken().equals("OP_ARIT_MAYOR_PRECEDENCIA")) {
            operador = preanalisis.getLexema();
            match("OP_ARIT_MAYOR_PRECEDENCIA", "un operador aritmético (*,/)");
            tipoOperando2 = subExpresion4();
            if (tipoOperando1.equals("INTEGER") && tipoOperando2.equals("INTEGER")) {
                tipoSubExp = "INTEGER";
                if (operador.equals("*")) {
                    writer.write("MULT");
                } else {
                    writer.write("DIVI");
                }
                writer.newLine();

            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    huboErrorSemantico = true;
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                            + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }

        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String Factor() throws ErrorSintactico, ErrorLexico, IOException {

        String tipo, identificador, signo = null;

        switch (preanalisis.getNombreToken()) {

            case "NOT":
                match("NOT", "el operador lógico NOT");
                tipo = Expresion();
                if (!tipo.equals("BOOLEAN") && !tipo.equals("ERROR")) {
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + nroLinea + ": EL OPERADOR UNARIO \'NOT\' REQUIERE OPERADOR DE TIPO \'BOOLEAN\' PERO SE ENCONTRÓ O"
                            + "OPERADOR DE TIPO \'" + tipo + "\'");
                    tipo = "ERROR";
                } else {
                    writer.write("NEGA" + "\n");
                }

                break;

            case "TRUE":
                match("TRUE", "la constante booleana TRUE");
                tipo = "BOOLEAN";
                writer.write("APCT 1" + "\n");
                break;

            case "FALSE":
                match("FALSE", "la constante booleana FALSE");
                tipo = "BOOLEAN";
                writer.write("APCT 0" + "\n");
                break;

            case "PARENTESIS_APERTURA":
                match("PARENTESIS_APERTURA", "(");
                tipo = Expresion();
                match("PARENTESIS_CIERRE", ")");
                break;

            default:
                if (preanalisis.getNombreToken().equals("OP_ARIT_MENOR_PRECEDENCIA")) {
                    match("OP_ARIT_MENOR_PRECEDENCIA", "un operador aritmético (+,-)");
                    signo = preanalisis.getLexema();
                }

                switch (preanalisis.getNombreToken()) {

                    case "IDENTIFICADOR":
                        identificador = preanalisis.getLexema();
                        match("IDENTIFICADOR", "un IDENTIFICADOR");
                        if (this.tablaActual.getCategoria(identificador).equals("VARIABLE")) {
                            tipo = this.tablaActual.getTipoDeDato(identificador);
                            writer.write("APVL " + this.tablaActual.getProfundidad(identificador) + "," + this.tablaActual.getDesplazamiento(identificador) + "\n");
                        } else {
                            tipo = Aux_Func_Proc_Call(identificador);
                            if (tipo == null) {
                                huboErrorSemantico = true;
                                System.out.println("ERROR SEMANTICO EN LINEA N°" + nroLinea + ": PROCEDIMIENTO \'" + identificador + ""
                                        + "\' NO PUEDE SER USADO NI COMO OPERADOR NI FORMAR PARTE DE UNA ASIGNACION NI COMO ARGUMENTO DE UNA INVOCACIÓN PORQUE NO RETORNA VALOR ALGUNO");
                                tipo = "ERROR";
                            }
                        }

                        break;

                    case "PARENTESIS APERTURA":
                        match("PARENTESIS_APERTURA", "(");
                        tipo = Expresion();
                        match("PARENTESIS_CIERRE", ")");
                        break;

                    default:
                        String aux = preanalisis.getLexema();
                        match("NUMERO_ENTERO", "");
                        tipo = "INTEGER";
                        writer.write("APCT " + aux + "\n");
                        break;
                }

                if (signo != null && (signo.equals("+") || signo.equals("-"))) {
                    if (signo.equals("-")) {
                        writer.write("NEGA" + "\n");
                    }
                }

        }

        return tipo;
    }
}
