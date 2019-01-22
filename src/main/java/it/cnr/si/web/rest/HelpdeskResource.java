package it.cnr.si.web.rest;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import it.cnr.si.domain.ExternalProblem;
import it.cnr.si.flows.ng.utils.JSONResponseEntity;
import it.cnr.si.service.HelpdeskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import it.cnr.si.security.AuthoritiesConstants;

import java.util.HashMap;

@RolesAllowed({AuthoritiesConstants.USER})
@RestController
@RequestMapping("/api")
public class HelpdeskResource {

	public static final String PREFISSO_TITOLO = "[FLOWS] - ";
	@Autowired
	private HelpdeskService helpdeskService;
	private final Logger log = LoggerFactory.getLogger(HelpdeskResource.class);


	@PostMapping(value = "/helpdesk/sendWithAttachment", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity sendWithAttachment(HttpServletRequest req, @RequestParam("file") MultipartFile uploadedMultipartFile) {
//		todo: riscrivere perchè hd viene inizializzato nel service
	    log.info("Invio mail helpdesk con allegato");
		ExternalProblem hd = new ExternalProblem();

		hd.setTitolo(PREFISSO_TITOLO + req.getParameter("titolo"));
		if (StringUtils.hasLength(req.getParameter("idHelpdesk"))){
			hd.setIdSegnalazione(new Long (req.getParameter("idHelpdesk")));
			hd.setNota(req.getParameter("nota"));
		}
		hd.setDescrizione(req.getParameter("descrizione"));
		hd.setCategoria(new Integer(req.getParameter("categoria")));
		hd.setCategoriaDescrizione(req.getParameter("categoriaDescrizione"));
		Long id = helpdeskService.newProblem(hd);
		helpdeskService.addAttachments(id, uploadedMultipartFile);
		
		return JSONResponseEntity.ok();
	}


	@PostMapping(value = "/helpdesk/sendWithoutAttachment", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity sendWithoutAttachment(@RequestBody ExternalProblem hdDataModel) {
		log.info("InvHashmaio mail helpdesk senza allegato");
		HashMap response = new HashMap();
		hdDataModel.setTitolo(PREFISSO_TITOLO + hdDataModel.getTitolo());
		response.put("segnalazioneId" ,helpdeskService.newProblem(hdDataModel));

		return JSONResponseEntity.ok(response);
	}
}
