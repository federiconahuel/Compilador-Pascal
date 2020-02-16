package AnalizadorSintactico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TablaSimbolos {

    private final HashMap<String, Object[]> tabla;
    private final TablaSimbolos padre;
    private int cantVariables;
    private final int nivelProfundidad;

    public TablaSimbolos(TablaSimbolos padre, int nivelProfundidad) {
        this.tabla = new HashMap();
        this.padre = padre;
        this.nivelProfundidad = nivelProfundidad;
        this.cantVariables = 0;
    }
    
    public int cantPares(){
        return this.tabla.size();
    }

    @Override
    public String toString() {
        String res = "", clave, valores;

        for (Map.Entry<String, Object[]> entry : tabla.entrySet()) {
            clave = entry.getKey();
            Object[] value = entry.getValue();

            valores = "\t " + (String) value[0] + "\t" + (String) value[1] + "\t \t" + ((ArrayList) value[2]) + "\t \t \t" + (Integer) value[3];

            res += clave + "\t" + valores + "\n";
        }

        return res;
    }

    //Categoría indica si el símbolo corresponde a una variable/programa/funcion/procedimiento/numero...
    //En el caso de una variable, tipoDeDato indica su tipo. En el caso de una funcion, tipoDeDato indica el tipo de su retorno. Para cualquier otro caso, es null
    //En el caso de procedimientos o funciones, tipoDatoArgs almacena el tipo de cada uno de sus argumentos. En cualquier otro caso, es null
    //En el caso de procedimientos o funciones, padre almacena un puntero a la tabla de símbolos del llamador. En cualquier otro caso, es null
    public void insertarElemento(String simbolo, String categoria, String tipoDeDato, ArrayList tipoDatoArgs, int desplazamiento, int etiqueta) {
        Object[] aux = new Object[6];
        aux[0] = categoria;
        aux[1] = tipoDeDato;
        aux[2] = tipoDatoArgs;
        if (tipoDatoArgs != null) {
            aux[3] = tipoDatoArgs.size();
        } else {
            aux[3] = 0;
        }
        aux[4] = desplazamiento;
        aux[5] = etiqueta;
        this.tabla.put(simbolo, aux);
        //aux[5] = punteroAPadre;

    }

    public void setCantidadVariables(int cant){
        this.cantVariables = cant;
    }
    
    
    public int getCantidadVariables(){
       return this.cantVariables;
    }

    public String getEtiqueta(String simbolo){
        String output = "";

        if (this.tabla.get(simbolo) != null) {
            output =  this.tabla.get(simbolo)[5]+"";
        } else if (padre != null) {
            output = this.padre.getEtiqueta(simbolo);
        }
        return output;   
    }

    public String getCategoria(String simbolo) {
        String output = "";

        if (this.tabla.get(simbolo) != null) {
            output = (String) this.tabla.get(simbolo)[0];
        } else if (padre != null) {
            output = this.padre.getCategoria(simbolo);
        }
        return output;
    }

    public String getProfundidad(String simbolo) {
        String output = "";

        if (this.tabla.get(simbolo) != null) {
            output = this.nivelProfundidad+"";
        } else if (padre != null) {
            output = this.padre.getProfundidad(simbolo);
        }
        return output;
    }
    
    public String getDesplazamiento(String simbolo) {
        String output = "";

        if (this.tabla.get(simbolo) != null) {
            output = this.tabla.get(simbolo)[4]+"";
        } else if (padre != null) {
            output = this.padre.getProfundidad(simbolo);
        }
        return output;
    }

    public String getTipoDeDato(String simbolo) {
        String output = "";

        if (this.tabla.get(simbolo) != null) {
            output = (String) this.tabla.get(simbolo)[1];
        } else if (padre != null) {
            output = this.padre.getTipoDeDato(simbolo);
        }
        return output;
    }

    public boolean checkSymbolExistence(String symbol, boolean checkStaticChain) {
        boolean existe = false;
        Object[] datosProc = this.tabla.get(symbol);
        if (datosProc != null) {
            existe = true;
        } else if (checkStaticChain && this.padre != null) {
            existe = this.padre.checkSymbolExistence(symbol, checkStaticChain);
        }
        return existe;
    }

    public String isFuncProcCallValid(String nombreFuncProc, ArrayList tipoDatosArgsInvocacion) {

        String output = "INEXISTENTE", aux;
        int i = 0;
        Object[] datosFunction = this.tabla.get(nombreFuncProc);

        //Si el identificador existe y es una funcion o un procedimiento, continua revisando
        if (datosFunction != null && ((((String) datosFunction[0]).equals("FUNCTION")) || (((String) datosFunction[0]).equals("VAR_FUNC")) || (((String) datosFunction[0]).equals("PROCEDURE")))) {

            if (tipoDatosArgsInvocacion == null) {
                if ((int) datosFunction[3] == 0) { //Parametros formales 0 y 0 argumentos de invocacion -> OK
                    output = "OK";
                } else { //Se invoca sin argumentos pero se requerian parametros formales
                    output = "ARGUMENTOS_INCOMPATIBLES";
                }

            } else {
                output = "ARGUMENTOS_INCOMPATIBLES";
                if ((int) datosFunction[3] == tipoDatosArgsInvocacion.size()) { //Coincide la cantidad, se chequean los tipos
                    //output = "ARGUMENTOS_INCOMPATIBLES";
                    do {
                        output = "ARGUMENTOS_INCOMPATIBLES";
                        String tipoInvocacion = (String) tipoDatosArgsInvocacion.get(i);
                        String tipoDeclaracion = ((String) (((ArrayList) datosFunction[2]).get(i)));

                        if (tipoInvocacion.equals(tipoDeclaracion)) {
                            output = "OK";
                        }
                        i++;
                    } while (i < (int) datosFunction[3] && output.equals("OK"));
                }

            }

        }

        if (!output.equals("OK") && this.padre != null) {
            aux = this.padre.isFuncProcCallValid(nombreFuncProc, tipoDatosArgsInvocacion);
            if (aux.equals("OK") || aux.equals("ARGUMENTOS_INCOMPATIBLES")) {
                output = aux;
            }
        }

        return output;
    }

    public TablaSimbolos getTablaPadre() {
        return this.padre;
    }

    public ArrayList getTiposDeDatoArgs(String simbolo) {
        ArrayList output = null;

        if (this.tabla.get(simbolo) != null) {
            output = (ArrayList) this.tabla.get(simbolo)[2];
        } else if (padre != null) {
            output = this.padre.getTiposDeDatoArgs(simbolo);
        }
        return output;
    }

}
