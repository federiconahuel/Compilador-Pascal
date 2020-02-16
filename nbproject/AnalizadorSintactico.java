/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AnalizadorSintactico;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorLexico.Token;
import java.io.BufferedReader;
import java.util.ArrayList;

public class AnalizadorSintactico {

    private final AnalizadorLexico lex;
    private Token preanalisis;
    private boolean huboError;
    private TablaSimbolos tablaActual;

    public AnalizadorSintactico(BufferedReader fileIn) {
        lex = new AnalizadorLexico(fileIn);
        huboError = false;
        tablaActual = new TablaSimbolos(null);

    }

    void match(String t, String cartel) throws ErrorSintactico, ErrorLexico {
        String tokenActual = preanalisis.getNombreToken();

        //Si el nombre del token esperado es igual al del token recibido, se le pide el siguiente token al analizador
        //lexico. En caso contrario, existe error sintactico.
        if (tokenActual.equals(t)) {

            preanalisis = lex.obtenerToken();

            //Si el analizador léxico detectó error léxico, se lanza una excepcion de tipo "ErrorLexico"
            //En iniciar() se atrapa esta excepcion y se detiene el analisis sintactico
            if (lex.huboError()) {
                throw new ErrorLexico();
            }

        } else {

            System.out.print("ERROR DE SINTAXIS EN LINEA N° " + lex.getContadorLineas() + ": ");

            //Si el error sintactico del tipo 1 (es decir: FIN DE PROGRAMA INESPERADO). Ver informe
            if (tokenActual.equals("PUNTO") || tokenActual.equals("FIN")) {
                System.out.println("FIN DE PROGRAMA INESPERADO" + "\n");
            } else {
                if (t.equals("NUMERO_ENTERO")) {
                    System.out.println("EXPRESION ILEGAL");
                } else {
                    System.out.print("\n Se esperaba " + cartel + " pero se encontró \'" + "" + preanalisis.getLexema() + "\'" + "\n");
                }
            }

            throw new ErrorSintactico();

        }
    }

    void programa() {

        String nombrePrograma;

        try {
            preanalisis = lex.obtenerToken();
            match("PROGRAM", "la palabra reservada PROGRAM");
            nombrePrograma = preanalisis.getAtributo();
            match("IDENTIFICADOR", "el IDENTIFICADOR del nombre del programa");
            tablaActual.insertarElemento(nombrePrograma, "PROGRAM", null, null);
            match("PUNTO_COMA", ";");
            Declaraciones();
            //match("BEGIN", "la palabra reservada BEGIN que inicia el programa "+nombrePrograma);
            Sentencia_comp();
            //Cuerpo();
            /*if(preanalisis.getNombreToken().equals("PUNTO_COMA"))
             match("PUNTO_COMA",";");*/
            //match("END", "la palabra reservada END que finaliza el programa "+nombrePrograma);
            match("PUNTO", ".");

        } //Si se detecta un error sintactico o lexico durante el analisis sintactico, este es abortado.
        catch (ErrorSintactico | ErrorLexico ex) {
            huboError = true;
        }
    }

    public boolean huboError() {
        return huboError;
    }

    void Declaraciones() throws ErrorSintactico, ErrorLexico {

        while (preanalisis.getNombreToken().equals("VAR")) {
            Declaracion_variables();
        }
        while (preanalisis.getNombreToken().equals("FUNCTION") || preanalisis.getNombreToken().equals("PROCEDURE")) {
            Declaracion_func_subprograma();
        }

    }

    void Declaracion_func_subprograma() throws ErrorSintactico, ErrorLexico {

        TablaSimbolos newTabla = new TablaSimbolos(this.tablaActual);

        if (preanalisis.getNombreToken().equals("FUNCTION")) {
            Cab_func(newTabla);
        } else {
            Cab_proc(newTabla);
        }

        tablaActual = newTabla;
        Declaraciones();
        Sentencia_comp();
        match("PUNTO_COMA", ";");
        tablaActual = newTabla.getTablaPadre(); //Se elimina el ambiente actual
    }

    void Cab_func(TablaSimbolos newTabla) throws ErrorSintactico, ErrorLexico {

        String nombreFuncion, tipo;
        ArrayList tipoDatoArgs = null;

        match("FUNCTION", "la palabra reservada FUNCTION");

        nombreFuncion = preanalisis.getAtributo();

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
        newTabla.insertarElemento(nombreFuncion, "VARIABLE", tipo, null);

        if (!this.tablaActual.checkSymbolExistence(nombreFuncion, false)) {
            this.tablaActual.insertarElemento(nombreFuncion, "FUNCTION", tipo, tipoDatoArgs);
        } else {
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.lex.getContadorLineas() + ": EL IDENTIFICADOR \n" + nombreFuncion + "\n YA FUE DECLARADO PREVIAMENTE");
        }
        match("PUNTO_COMA", ";");

    }

    void Cab_proc(TablaSimbolos newTabla) throws ErrorSintactico, ErrorLexico {

        ArrayList tipoDatoArgs = null;
        String nombreProc;
        match("PROCEDURE", "la palabra reservada PROCEDURE");
        nombreProc = this.preanalisis.getAtributo();
        match("IDENTIFICADOR", "el IDENTIFICADOR del nombre del procedimiento");
        if (preanalisis.getNombreToken().equals("PARENTESIS_APERTURA")) {
            match("PARENTESIS_APERTURA", "(");
            tipoDatoArgs = Param_Formales(newTabla, "99999");//En procedimientos no es necesario chequear que ninguno de los param. formales tenga el mismo nombre que el proc.
            match("PARENTESIS_CIERRE", ")");
        }
        match("PUNTO_COMA", ";");
        if (!this.tablaActual.checkSymbolExistence(nombreProc, false)) {
            this.tablaActual.insertarElemento(nombreProc, "PROCEDURE", null, tipoDatoArgs);
        } else {
            //ERROR!!!
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.lex.getContadorLineas() + ": EL IDENTIFICADOR \n" + nombreProc + "\n YA FUE DECLARADO PREVIAMENTE");

        }

    }

    void Declaracion_variables() throws ErrorSintactico, ErrorLexico {

        String idActual, tipo;
        ArrayList<String> identificadores;

        match("VAR", "la palabra reservada VAR");
        do {
            identificadores = new ArrayList();
            idActual = preanalisis.getAtributo();
            identificadores.add(idActual);
            match("IDENTIFICADOR", "un IDENTIFICADOR de variable");
            while (preanalisis.getNombreToken().equals("COMA")) {
                match("COMA", ",");
                idActual = preanalisis.getAtributo();
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

            for (int i = 0; i < identificadores.size(); i++) {
                idActual = identificadores.get(i);
                if (!this.tablaActual.checkSymbolExistence(idActual, false)) {
                    this.tablaActual.insertarElemento(idActual, "VARIABLE", tipo, null);
                } else {
                    //ERROR!!! VARIABLE DEFINIDA PREVIAMENTE(DUPLICADA)
                    System.out.println("ERROR SEMÁNTICO EN LINEA N° " + this.lex.getContadorLineas() + ": LA VARIABLE \n" + idActual + "\n YA FUE DECLARADA PREVIAMENTE");
                    //System.out.println(tablaActual.toString());
                }
            }

            //if (!preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
            match("PUNTO_COMA", ";");
            //}

        } while (preanalisis.getNombreToken().equals("IDENTIFICADOR"));

    }

    ArrayList Param_Formales(TablaSimbolos newTabla, String nombreFuncProc) throws ErrorSintactico, ErrorLexico {

        ArrayList typeArgs = new ArrayList();
        ArrayList<String> identificadores = new ArrayList();
        String idActual, tipo;

        while (preanalisis.getNombreToken().equals("IDENTIFICADOR")) {

            idActual = preanalisis.getAtributo();
            match("IDENTIFICADOR", "un IDENTIFICADOR de parámetro formal");
            identificadores.add(idActual);

            while (preanalisis.getNombreToken().equals("COMA")) {
                match("COMA", ",");
                idActual = preanalisis.getAtributo();
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

            for (int i = 0; i < identificadores.size(); i++) {
                idActual = identificadores.get(i);
                if (!idActual.equals(nombreFuncProc)) {

                    if (!newTabla.checkSymbolExistence(idActual, false)) {
                        typeArgs.add(tipo);
                        newTabla.insertarElemento(idActual, "VARIABLE", tipo, null);
                    } else {
                        //ERROR!!! PARAMETRO YA DEFINIDO ANTERIORMENTE EN LA LISTA DE PARAM. FORMALES ACTUAL
                        System.out.println("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + ": EL PARÁMETRO \'" + idActual + "\' FUE "
                                + "DECLARADO PREVIAMENTE EN LA LISTA DE LISTA DE PARAMETROS DE LA FUNCION/PROCEDIMIENTO \'" + nombreFuncProc + "\'");
                    }
                } else {
                    //ERROR!!! UNO DE LOS PARAMETROS FORMALES TIENE EL MISMO NOMBRE QUE LA FUNCION
                    System.out.println("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + ": EL PARÁMETRO \'" + idActual + " NO PUEDE TENER EL"
                            + "MISMO NOMBRE QUE LA FUNCIÓN/PROCEDIMIENTO AL QUE PERTENECE");
                }
            }

            if (!preanalisis.getNombreToken().equals("PARENTESIS_CIERRE")) {
                match("PUNTO_COMA", ";");
                identificadores.clear();
            }

        }

        return typeArgs;

    }


    /*void Tipo_dato() throws ErrorSintactico, ErrorLexico{

     if(preanalisis.getNombreToken().equals("INTEGER")){
     match("INTEGER","la palabra reservada INTEGER");
     }
     else{
     match("BOOLEAN");
     }
     }*/
    void Sentencia_comp() throws ErrorSintactico, ErrorLexico {
        match("BEGIN", "la palabra reservada BEGIN");
        Cuerpo();
        match("END", "la palabra reservada END");
        //if(!preanalisis.getNombreToken().equals("END")&& !preanalisis.getNombreToken().equals("PUNTO")&&!preanalisis.getNombreToken().equals("ELSE"))
        //match("PUNTO_COMA",";");
    }

    void Cuerpo() throws ErrorSintactico, ErrorLexico {
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

    void Sentencia() throws ErrorSintactico, ErrorLexico {

        String aux, tipoSentencia, auxTipo;

        switch (preanalisis.getNombreToken()) {

            case "IDENTIFICADOR":
                aux = preanalisis.getAtributo();
                match("IDENTIFICADOR", "un IDENTIFICADOR para comenzar la sentencia");
                //Aux_Asign_Func_Proc_Call(aux);
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

    
    void Aux_Asignacion(String identificador) throws ErrorSintactico, ErrorLexico {
        String tipo = "VOID", tipoLadoIzq, tipoLadoDer;

        match("ASIGNACION", "una  ASIGNACION :=");

        tipoLadoIzq = this.tablaActual.getTipoDeDato(identificador);
        tipoLadoDer = Expresion();

        if (this.tablaActual.getCategoria(identificador).equals("VARIABLE")) {
            if (!tipoLadoIzq.equals(tipoLadoDer) && !tipoLadoDer.equals("ERROR")) {
                tipo = "ERROR";
                System.out.println("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + " LA EXPRESIÓN DEL LADO DERECHO DEBE SER DE TIPO \'" + tipoLadoIzq
                        + "\' PERO SE ENCONTRÓ QUE ES DE TIPO \'" + tipoLadoDer + "\'");
            }
        } else {
            tipo = "ERROR"; //VARIABLE INEXISTENTE
            System.out.println("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + ": IDENTIFICADOR \'" + identificador + "\' NO FUE DEFINIDO PREVIAMENTE");

        }

        //return tipo;
    }

    String Aux_Func_Proc_Call(String identificador) throws ErrorSintactico, ErrorLexico {
        String tipo = "ERROR", aux;
        ArrayList tiposArgsInvocacion = null, tiposArgsEsperados;
        
        if (preanalisis.getNombreToken().equals("PARENTESIS_APERTURA")) {
            match("PARENTESIS_APERTURA", "(");
            if (!preanalisis.getNombreToken().equals("PARANTESIS_CIERRE")) {
                tiposArgsInvocacion = new ArrayList();
                tipo = Expresion();
                tiposArgsInvocacion.add(tipo);

                while (preanalisis.getNombreToken().equals("COMA")) {
                    match("COMA", ",");
                    tipo = Expresion();
                    tiposArgsInvocacion.add(tipo);
                }
            }
            match("PARENTESIS_CIERRE", ")");
        }

        aux = this.tablaActual.isFuncProcCallValid(identificador, tiposArgsInvocacion);

        if (aux.equals("OK")) {
            tipo = this.tablaActual.getTipoDeDato(identificador);
        } else {
            if (aux.equals("INEXISTENTE")) {
                System.out.println("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + ": IDENTIFICADOR \'" + identificador + "\' NO FUE DEFINIDO PREVIAMENTE");
            } else {
                if ((tiposArgsInvocacion != null && !tiposArgsInvocacion.contains("ERROR")) || (tiposArgsInvocacion == null)) {
                    System.out.print("ERROR SEMÁNTICO EN LINEA N° " + lex.getContadorLineas() + ": ARGUMENTOS DE ENTRADA INCOMPATIBLES: SE ESPERABA (");
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
                    	System.out.print("Y SE ENCONTRO (");
                        for (int i = 0; i < tiposArgsInvocacion.size(); i++) {
                            System.out.print(tiposArgsInvocacion.get(i));
                            if ((i + 1) < tiposArgsInvocacion.size()) {
                                System.out.print(" ");
                            }
                        }
                        System.out.print(")");
                    }else{
                    	System.out.print("PERO NO SE ENCONTRARON ARGUMENTOS DE ENTRADA EN LA INVOCACIÓN");
                    }
                    
                }

            }

        }

        return tipo;
    }

    
    void Funciones_In_Out() throws ErrorSintactico, ErrorLexico {
        if (preanalisis.getNombreToken().equals("WRITE")) {
            match("WRITE", "la palabra reservada WRITE");
            match("PARENTESIS_APERTURA", "(");
            if (preanalisis.getNombreToken().equals("COMILLA_SIMPLE")) {
                match("COMILLA_SIMPLE", "'");
                //El contenido entre comillas simples no se considera como token
                match("COMILLA_SIMPLE", "'");
            } else {
                Expresion();
            }
            match("PARENTESIS_CIERRE", ")");
        } else {
            match("READ", "la palabra reservada READ");
            match("PARENTESIS_APERTURA", "(");
            match("IDENTIFICADOR", "un IDENTIFICADOR de variable para su lectura");
            match("PARENTESIS_CIERRE", ")");
        }
    }

    void Estructura_control() throws ErrorSintactico, ErrorLexico {
        if (preanalisis.getNombreToken().equals("IF")) {
            Alternativa();
        } else {
            Repetitiva();
        }
    }

    void Alternativa() throws ErrorSintactico, ErrorLexico {
        match("IF", "la palabra reservada IF");
        Expresion();
        match("THEN", "la palabra reservada THEN");
        if (preanalisis.getNombreToken().equals("BEGIN")) {
            Sentencia_comp();
        } else {
            Sentencia();
        }

        if (preanalisis.getNombreToken().equals("ELSE")) {
            match("ELSE", "la palabra reservada ELSE");
            if (preanalisis.getNombreToken().equals("BEGIN")) {
                //match("BEGIN");
                Sentencia_comp();
                //match("END");
            } else {
                Sentencia();
            }
        }
    }

    void Repetitiva() throws ErrorSintactico, ErrorLexico {
        match("WHILE", "la palabra reservada WHILE");
        Expresion();
        match("DO", "la palabra reservada DO");
        if (preanalisis.getNombreToken().equals("BEGIN")) {
            //Cuerpo();
            Sentencia_comp();
        } else {
            Sentencia();
        }

    }

    String Expresion() throws ErrorSintactico, ErrorLexico {
        String tipoOperando1, tipoOperando2, tipoExp;

        tipoOperando1 = subExpresion1();
        if (preanalisis.getNombreToken().equals("OR")) {
            match("OR", "el operador lógico OR");
            tipoOperando2 = Expresion();
            if (tipoOperando1.equals("BOOLEAN") && tipoOperando2.equals("BOOLEAN")) {
                tipoExp = "BOOLEAN";
            } else {
                tipoExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'OR\' REQUIERE OPERADORES DE TIPO "
                            + "\'BOOLEAN\' Y \'BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoExp = tipoOperando1;
        }

        return tipoExp;
    }

    String subExpresion1() throws ErrorSintactico, ErrorLexico {
        String tipoOperando1, tipoOperando2, tipoSubExp;

        tipoOperando1 = subExpresion2();
        if (preanalisis.getNombreToken().equals("AND")) {
            match("AND", "el operador lógico  AND");
            tipoOperando2 = subExpresion1();
            if (tipoOperando1.equals("BOOLEAN") && tipoOperando2.equals("BOOLEAN")) {
                tipoSubExp = "BOOLEAN";
            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'AND\' REQUIERE OPERADORES DE TIPO "
                            + "\'BOOLEAN\' Y \'BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoSubExp = tipoOperando1;
        }
        return tipoSubExp;
    }

    String subExpresion2() throws ErrorSintactico, ErrorLexico {
        String tipoOperando1, tipoOperando2, tipoSubExp = "ERROR", operador;
        boolean r = true;
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
                }
            }

            if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR")) {

                switch (operador) {
                    case "=":
                        System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                                + "\'INTEGER/BOOLEAN\' E \'INTEGER/BOOLEAN\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                        break;

                    default:
                        System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                                + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                        break;
                }

            }
      
        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String subExpresion3() throws ErrorSintactico, ErrorLexico {
        String tipoOperando1, tipoOperando2, tipoSubExp, operador;

        tipoOperando1 = subExpresion4();
        if (preanalisis.getNombreToken().equals("OP_ARIT_MENOR_PRECEDENCIA")) {
            operador = preanalisis.getLexema();
            match("OP_ARIT_MENOR_PRECEDENCIA", "un operador aritmético (+,-)");
            tipoOperando2 = subExpresion3();
            if (tipoOperando1.equals("INTEGER") && tipoOperando2.equals("INTEGER")) {
                tipoSubExp = "INTEGER";
            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                            + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }
        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String subExpresion4() throws ErrorSintactico, ErrorLexico {
        String tipoOperando1, tipoOperando2, tipoSubExp, operador;

        tipoOperando1 = Factor();
        if (preanalisis.getNombreToken().equals("OP_ARIT_MAYOR_PRECEDENCIA")) {
            operador = preanalisis.getLexema();
            match("OP_ARIT_MAYOR_PRECEDENCIA", "un operador aritmético (*,/)");
            tipoOperando2 = subExpresion4();
            if (tipoOperando1.equals("INTEGER") && tipoOperando2.equals("INTEGER")) {
                tipoSubExp = "INTEGER";
            } else {
                tipoSubExp = "ERROR";
                if (!tipoOperando1.equals("ERROR") && !tipoOperando2.equals("ERROR"))//{
                {
                    System.out.println("ERROR SEMANTICO EN LINEA N° " + lex.getContadorLineas() + ": EL OPERADOR BINARIO \'" + operador + "\' REQUIERE OPERADORES DE TIPO "
                            + "\'INTEGER\' E \'INTEGER\', PERO SE ENCONTRÓ QUE EL PRIMER OPERADOR ES DE TIPO \'" + tipoOperando1 + "\' Y EL SEGUNDO ES DE TIPO \'" + tipoOperando2 + "\'");
                }
            }

        } else {
            tipoSubExp = tipoOperando1;
        }

        return tipoSubExp;
    }

    String Factor() throws ErrorSintactico, ErrorLexico {

        String tipo = "", identificador;

        switch (preanalisis.getNombreToken()) {

            case "NOT":
                match("NOT", "el operador lógico NOT");
                if (Expresion().equals("BOOLEAN")) {
                    tipo = "BOOLEAN";
                }
                break;

            case "TRUE":
                match("TRUE", "la constante booleana TRUE");
                tipo = "BOOLEAN";
                break;

            case "FALSE":
                match("FALSE", "la constante booleana FALSE");
                tipo = "BOOLEAN";
                break;

            case "PARENTESIS_APERTURA":
                match("PARENTESIS_APERTURA", "(");
                tipo = Expresion();
                match("PARENTESIS_CIERRE", ")");
                break;

            default:
                if (preanalisis.getNombreToken().equals("OP_ARIT_MENOR_PRECEDENCIA")) {
                    match("OP_ARIT_MENOR_PRECEDENCIA", "un operador aritmético (+,-)");
                }
                switch (preanalisis.getNombreToken()) {

                    case "IDENTIFICADOR":
                        identificador = preanalisis.getAtributo();
                        match("IDENTIFICADOR", "un IDENTIFICADOR");
                        if (this.tablaActual.getCategoria(identificador).equals("VARIABLE")) {
                            tipo = this.tablaActual.getTipoDeDato(identificador);
                        } else {
                            tipo = Aux_Func_Proc_Call(identificador);
                            if (tipo == null) {
                                System.out.println("ERROR SEMANTICO EN LINEA N°" + lex.getContadorLineas() + ": PROCEDIMIENTO \'" + identificador + ""
                                        + "\' NO PUEDE SER USADO COMO OPERADOR NI FORMAR PARTE DE UNA ASIGNACION PORQUE NO RETORNA VALOR ALGUNO");
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
                        match("NUMERO_ENTERO", "");
                        tipo = "INTEGER";
                        break;
                }
        }

        return tipo;
    }

// Si hay un NT que no es opcional ent se llama al procedimiento de ese NT()
//Si hay un T que es opcional se coloca if then de la forma if(preanalisis.getNombreToken().equals() t) then match(t);
//Todos los terminales pasan derecho al match(nombre_token)
}