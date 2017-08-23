package it.cnr.si.flows.ng.resource;

import java.time.Instant;
import java.util.Map;

import org.activiti.engine.delegate.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.SecurityUtils;

/**
 * Gestore centrale delle eccezioni dell'applicazione
 *
 * Tutte le Exception generate nell'applicazione vanno mappate qui, in un unico posto centrale
 *
 * In questo modo abbiamo un posto unico e una maniera uniforme di gestire tutte le eccezioni
 * (semplifica anche lo sviluppo del frontend)
 *
 * Eccezioni non gestite verranno catturate dai due metodi per Exception e RuntimeException
 * Spring selezionera' l'eccezione piu' specifica, e se non ne trova ripiega su queste due
 *
 * @author mtrycz
 */
@ControllerAdvice
public class FlowsRestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsRestExceptionHandler.class);


    @ExceptionHandler(NullPointerException.class)
    protected ResponseEntity<Object> HandleNull(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "E' stato ricevuto un null pointer";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    @ExceptionHandler(ProcessDefinitionAndTaskIdEmptyException.class)
    protected ResponseEntity<Object> HandleProcessDefinitionAndTaskIdEmpty(ProcessDefinitionAndTaskIdEmptyException ex, WebRequest request) {
        String bodyOfResponse = "Fornire almeno un taskId o un definitionId";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

    }


    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> HandleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String username = SecurityUtils.getCurrentUserLogin();
        String contextPath = request.getContextPath();
        LOGGER.error(username +" ha cercato di accedere a una risorsa "+ contextPath +" ma non ha i permessi necessari", ex);

        String bodyOfResponse = "L'utente non ha i permessi necessari per eseguire l'azione richiesta";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }


    @ExceptionHandler(BpmnError.class)
    protected ResponseEntity<Object> HandleUnknownException(BpmnError ex, WebRequest request) {
        String username = SecurityUtils.getCurrentUserLogin();
        String taskId = request.getParameter("taskId");
        String definitionId = request.getParameter("definitionId");

        LOGGER.error("L'utente {} ha cercato di a completare il task {} / avviare il flusso {}, ma c'e' stato un errore: {}", username, taskId, definitionId, ex.getMessage());
        return handleExceptionInternal(ex, Utils.mapOf("message", ex.getMessage()),
                new HttpHeaders(), Utils.getStatus(ex.getErrorCode()), request);
    }

    /* --- DEFAULT EXCEPTION HANDLERS --- */

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> HandleUnknownException(Exception ex, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore non gestito con messaggio " + ex.getMessage(), ex);

        Map<String, Object> res = Utils.mapOf("message", "Errore non gestito. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> HandleUnknownRuntimeException(RuntimeException ex, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore non gestito con messaggio " + ex.getMessage(), ex);

        Map<String, Object> res = Utils.mapOf("message", "Errore non gestito. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    }
}
