package it.cnr.si.flows.ng.exception;

public class FlowsPermissionException extends Exception {

    private static final long serialVersionUID = 8805422045768395788L;

    public FlowsPermissionException() {
        super();
    }

    public FlowsPermissionException(Exception cause) {
        super(cause);
    }

    public FlowsPermissionException(String message) {
        super(message);
    }

    public FlowsPermissionException(String message, Exception cause) {
        super(message, cause);
    }

}
