package AnalizadorLexico;

//Cada instancia de esta clase representa un token.
public class Token {

    private final String nombreToken;
    private final String atributo;
    private final String lexema;

    public Token(String nombre, String atrib, String lex) {
        nombreToken = nombre;
        atributo = atrib;
        lexema = lex;
    }

    public String getNombreToken() {
        return nombreToken;
    }

    public String getAtributo() {
        return atributo;
    }

    public String getLexema() {
        return lexema;
    }

    @Override
    public String toString() {
        return "<" + nombreToken + "," + atributo + "," + lexema + ">";
    }
}
