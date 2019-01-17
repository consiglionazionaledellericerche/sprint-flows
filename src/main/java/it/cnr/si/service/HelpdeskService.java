package it.cnr.si.service;

import it.cnr.si.domain.ExternalProblem;
import it.cnr.si.flows.ng.exception.AwesomeException;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.flows.ng.utils.proxy.ResultProxy;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    ProxyService proxyService;
    @Inject
    Utils utils;
    @Inject
    AceService aceService;


    public Long newProblem(ExternalProblem hd) throws ServiceException {

        PersonaWebDto flowsUser = aceService.getPersonaByUsername(SecurityUtils.getCurrentUserLogin());
        hd.setLogin(flowsUser.getUsername());

        hd.setFirstName(flowsUser.getNome());
        hd.setFamilyName(flowsUser.getCognome());
//		todo: da mettere l`email quando sarà disponibile da ACE
        hd.setEmail(flowsUser.getUsername() + "@cnr.it");
//		todo: e confirmRequested: mettere a true per gli utenti non loggati in caso si voglia estendere l`helpdesk anche a loro
        hd.setConfirmRequested(false);

        String descrizione = hd.getDescrizione() + System.getProperty("line.separator") + System.getProperty("line.separator")
                + hd.getFirstName() + " " + hd.getFamilyName() + "  Email: " + hd.getEmail() +
                "  Data: " + utils.formattaDataOra(new Date());

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