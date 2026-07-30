package it.cnr.si.service;

import it.cnr.si.domain.ExternalProblem;
import it.cnr.si.flows.ng.exception.AwesomeException;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.flows.ng.utils.proxy.ResultProxy;

import it.cnr.si.service.dto.anagrafica.scritture.PersonaDto;
import it.cnr.si.service.dto.anagrafica.scritture.UtenteDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;

import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;


@Service
public class HelpdeskService {

    private static final String APP_HELPDESK = "OIL";
    private final static String REST_OIL_NEW_PROBLEM =  "pest/HDSiper";
    private final Logger log = LoggerFactory.getLogger(HelpdeskService.class);
    @Inject
    private ProxyService proxyService;
    @Inject
    private Utils utils;
    @Autowired(required = false)
    private AceService aceService;
    @Inject
    private SecurityService securityService;

    public Long newProblem(ExternalProblem hd, String browser) throws ServiceException {

//    	PersonaWebDto flowsUser = aceService.getPersonaByUsername(securityService.getCurrentUserLogin());
        SimpleUtenteWebDto flowsUser = aceService.getUtente(securityService.getCurrentUserLogin());
        hd.setLogin(flowsUser.getUsername());

        SimplePersonaWebDto persona = flowsUser.getPersona();
        String nomeCognomeString;
        if(persona != null) {
            hd.setFirstName(persona.getNome());
            hd.setFamilyName(persona.getCognome());
            nomeCognomeString = "Nome: " + hd.getFirstName() + " Cognome: " + hd.getFamilyName();
        } else {
//            todo: per gli assegnisti, non avendo l' oggetto persona in flowsUser, carico le uniche informazioni che ho
            hd.setFirstName(flowsUser.getUsername());
            hd.setFamilyName(flowsUser.getEmail());
            nomeCognomeString = "Nome e Cognome: " + flowsUser.getUsername();
        }
//		todo: da mettere l`email quando sarà disponibile da ACE
        hd.setEmail(flowsUser.getEmail());
//		todo: e confirmRequested: mettere a true per gli utenti non loggati in caso si voglia estendere l`helpdesk anche a loro
        hd.setConfirmRequested(false);

        String descrizione = hd.getDescrizione() + System.getProperty("line.separator") + System.getProperty("line.separator") +
                nomeCognomeString + System.getProperty("line.separator") +
                "Email: " + hd.getEmail() + System.getProperty("line.separator")  +
                "Data: " + utils.formattaDataOra(new Date()) + System.getProperty("line.separator") +
                "Browser: " + browser ;

        hd.setDescrizione(descrizione);

        ResultProxy result = proxyService.process(HttpMethod.PUT, hd, APP_HELPDESK, REST_OIL_NEW_PROBLEM, null, null);
        Long segnalazioneId = new Long(result.getBody());
        log.info("Segnalazione Helpdesk aperta con Response Code {}", segnalazioneId);
        return segnalazioneId;
    }

    public void addAttachments(long id, MultipartFile uploadedMultipartFile) throws ServiceException {

        String url = REST_OIL_NEW_PROBLEM + "/" + id;
        try {
            ResultProxy result = proxyService.processWithFile(HttpMethod.POST, null, APP_HELPDESK, url, null, null, uploadedMultipartFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AwesomeException("Errore per l'allegato " + e);
        }
    }
}