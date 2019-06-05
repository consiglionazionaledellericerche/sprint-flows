package it.cnr.si.flows.ng.exception;

public class UnexpectedResultException extends RuntimeException {

    public UnexpectedResultException() {
        super();
    }

    public UnexpectedResultException(Exception cause) {
        super(cause);
    }

    public UnexpectedResultException(String message) {
        super(message);
    }

    public UnexpectedResultException(String message, Exception cause) {super(message, cause);}
}
