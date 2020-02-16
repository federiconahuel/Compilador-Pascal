package AnalizadorSintactico;

public class ErrorLexico extends Exception {

    public ErrorLexico() {
    }

    public ErrorLexico(String msg) {
        super(msg);
    }
    
}
