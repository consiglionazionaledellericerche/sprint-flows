package it.cnr.si.flows.ng.exception;

public class SiglaFailedException extends RuntimeException {

    private static final long serialVersionUID = -548409125727753652L;

    public SiglaFailedException(Exception cause) {
        super(cause);
    }

    public SiglaFailedException(String message) {
        super(message);
    }

    public SiglaFailedException(String message, Exception cause) {
        super(message, cause);
    }
}
