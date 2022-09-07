package it.cnr.si.flows.ng.resource;

import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.exception.FileFormatException;
import it.cnr.si.flows.ng.exception.FlowsPermissionException;
import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.exception.ReportException;
import it.cnr.si.flows.ng.service.FlowsFirmaService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.SecurityService;

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
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Map;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.service.FlowsFirmaService.ERRORI_ARUBA;

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
    private static final String ERROR_MESSAGE = "message";

    @Inject
    private SecurityService securityService;


    @ExceptionHandler(NullPointerException.class)
    protected ResponseEntity<Object> HandleNull(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "E' stato ricevuto un null pointer per la richiesta "+ request.getContextPath();
        LOGGER.error(bodyOfResponse, ex);

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
        String username = securityService.getCurrentUserLogin();
        String contextPath = request.getContextPath();
        LOGGER.error(username +" ha cercato di accedere a una risorsa "+ contextPath +" ma non ha i permessi necessari", ex);

        String bodyOfResponse = "L'utente non ha i permessi necessari per eseguire l'azione richiesta";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }


    @ExceptionHandler(BpmnError.class)
    protected ResponseEntity<Object> HandleUnknownException(BpmnError ex, WebRequest request) {
        String username = securityService.getCurrentUserLogin();
        String taskId = request.getParameter("taskId");
        String definitionId = request.getParameter("definitionId");

        //Se non riesco a completare il task rimuovo l'identityLink che indica "l'esecutore" del task e restituisco un INTERNAL_SERVER_ERROR
        if( ex.getErrorCode() == "412" ) {
            String errorMessage = String.format("%s", ex.getMessage());
            LOGGER.warn(errorMessage);
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Utils.mapOf(ERROR_MESSAGE, errorMessage));
        }

        LOGGER.error("L'utente {} ha cercato di a completare il task {} / avviare il flusso {}, ma c'e' stato un errore: {}", username, taskId, definitionId, ex.getMessage());
        return handleExceptionInternal(ex, Utils.mapOf("message", ex.getMessage()),
                new HttpHeaders(), Utils.getStatus(ex.getErrorCode()), request);
    }


    @ExceptionHandler(ReportException.class)
    protected ResponseEntity<Object> HandleMakePdfException(Exception ex, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore nella creazione del pdf ");

        Map<String, Object> res = Utils.mapOf("message", "Errore nella creazione del pdf. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(ArubaSignServiceException.class)
    protected ResponseEntity<Object> handleArubaSignException(ArubaSignServiceException e, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        Map<String, Object> res = Utils.mapOf(
                "message",
                ERRORI_ARUBA.getOrDefault(e.getMessage(),"Errore sconosciuto"));

        return handleExceptionInternal(e, res,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

    }

    @ExceptionHandler(FlowsPermissionException.class)
    protected ResponseEntity<Object> handlePermissionException(FlowsPermissionException e, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        Map<String, Object> res = Utils.mapOf("message", e.getMessage());

        return handleExceptionInternal(e, res,
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);

    }

    @ExceptionHandler(MultipartException.class)
    protected ResponseEntity<Object> handleMultipartException(MultipartException ex, WebRequest request) {

        if (ex.getMessage().contains("SizeLimitExceededException")) {

            Map<String, Object> res = Utils.mapOf("message", "I file allegati superano il limite massimo di grandezza (50MB)");
//            return handleExceptionInternal(ex, res,
//                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore non gestito con messaggio " + ex.getMessage(), ex);

        Map<String, Object> res = Utils.mapOf("message", "Errore non gestito. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    }

    /* --- DEFAULT EXCEPTION HANDLERS --- */

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> HandleUnknownException(Exception ex, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore non gestito per la richiesta "+ request.getContextPath() +" con messaggio " + ex.getMessage(), ex);

        Map<String, Object> res = Utils.mapOf("message", "Errore non gestito. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    
    @ExceptionHandler(FileFormatException.class)
    protected ResponseEntity<Object> handleFileFormatException(FileFormatException ex, WebRequest request) {

        Map<String, Object> res = Utils.mapOf("message", ex.getMessage());
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> HandleUnknownRuntimeException(RuntimeException ex, WebRequest request) {

        long rif = Instant.now().toEpochMilli();
        LOGGER.error("(Riferimento " + rif + ") Errore non gestito con messaggio " + ex.getMessage(), ex);
        Throwable e = ex;
        while (e.getCause() != null) {
            e = e.getCause();
            LOGGER.error("(Riferimento " + rif + ") Errore interno " + e.getMessage(), e);
        }

        Map<String, Object> res = Utils.mapOf("message", "Errore non gestito. Contattare gli amminstratori specificando il numero di riferimento: " + rif);
        return handleExceptionInternal(ex, res,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    }

}
