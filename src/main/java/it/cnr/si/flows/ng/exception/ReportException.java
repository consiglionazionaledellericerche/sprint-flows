package it.cnr.si.flows.ng.exception;

public class ReportException extends RuntimeException {

    private static final long serialVersionUID = 1121291840032330160L;

    public ReportException(Exception cause) {
        super(cause);
    }

    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Exception cause) {
        super(message, cause);
    }

}
