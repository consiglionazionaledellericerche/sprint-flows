package it.cnr.si.flows.ng.exception;

public class FileFormatException extends Exception {

    private static final long serialVersionUID = -3264592410297608056L;

    public FileFormatException() {
        super();
    }

    public FileFormatException(Exception cause) {
        super(cause);
    }

    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(String message, Exception cause) {
        super(message, cause);
    }

}
