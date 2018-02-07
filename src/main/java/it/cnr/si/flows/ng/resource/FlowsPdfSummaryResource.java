package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.SummaryPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;

/**
 * Created by cirone on 13/06/17.
 */
@Controller
@RequestMapping("api")
public class FlowsPdfSummaryResource {

    @Inject
    private SummaryPdfService summaryPdfService;


    /**
     * Crea e restituisce il summary pdf del flusso.
     *
     * @param processInstanceId : processInstanceId del workflow di cui si vuole generare il summary
     * @return the response entity
     * @throws Exception the exception
     * @return: restituisce il pdf generato
     */
    @RequestMapping(value = "/summaryPdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
    @ResponseBody
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<byte[]> makePdf(
            @RequestParam("processInstanceId") String processInstanceId,
            HttpServletRequest req) throws Exception {

        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String fileName = summaryPdfService.createPdf(processInstanceId, outputStream);

            HttpHeaders headers = new HttpHeaders();
            ResponseEntity<byte[]> resp;
            headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.setContentLength(outputStream.toByteArray().length);
            resp = new ResponseEntity<byte[]>(outputStream.toByteArray(), headers, HttpStatus.OK);

            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}