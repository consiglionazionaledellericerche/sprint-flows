package it.cnr.si.flows.ng.exception;

public class TaskFailedException extends RuntimeException {

    private static final long serialVersionUID = -548409125727753652L;

    public TaskFailedException(Exception cause) {
        super(cause);
    }

    public TaskFailedException(String message) {
        super(message);
    }

    public TaskFailedException(String message, Exception cause) {
        super(message, cause);
    }

}
