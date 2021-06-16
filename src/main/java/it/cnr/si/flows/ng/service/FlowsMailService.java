package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.Blacklist;
import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.service.AceService;
import it.cnr.si.service.BlacklistService;
import it.cnr.si.service.CnrgroupService;
import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Utils.formatoDataUF;


@Service
@Primary
public class FlowsMailService extends MailService {

	public static final String FLOW_NOTIFICATION = "notificaFlow.html";
    public static final String PROCESS_NOTIFICATION = "notificaProcesso.html";
	public static final String PROCESS_COMPLETED_NOTIFICATION = "notificaProcessoCompletato.html";
	public static final String TASK_NOTIFICATION = "notificaTask.html";
    public static final String TASK_ASSEGNATO_AL_GRUPPO = "taskAssegnatoAlGruppo.html";
    public static final String TASK_IN_CARICO_ALL_UTENTE = "taskInCaricoAllUtente.html";

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsMailService.class);
	@Inject
	private TemplateEngine templateEngine;
	@Inject
	private MailConfguration mailConfig;
	@Inject
	private Environment env;
	@Inject
	private CnrgroupService cnrgroupService;
	@Autowired(required = false)
	private AceBridgeService aceBridgeService;
	@Autowired(required = false) //TODO
	private AceService aceService;
	@Inject
	private FlowsUserService flowsUserService;
	@Autowired
	private UserDetailsService flowsUserDetailsService;
	@Autowired
	private BlacklistService blacklistService;


	@Async
	public void sendFlowEventNotification(String notificationType, Map<String, Object> variables, String taskName, String username, final String groupName) {
		try {
			
			LOGGER.info("Invio della mail all'utente "+ username);
			String key = (String)variables.get("key");
			
			Context ctx = new Context();
			ctx.setVariables(variables);
			ctx.setVariable("username", username);

			ctx.setVariable("taskLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId") +"&taskId="+ variables.get("nextTaskId"));
			ctx.setVariable("processLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId"));

			// ${serverUrl}/#/details?processInstanceId=${processInstanceId}&amp;taskId=${nextTaskId}}

			if (groupName != null) {
				if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {
					ctx.setVariable("groupname", Optional.ofNullable(aceBridgeService)
							.flatMap(aceBridgeService -> Optional.ofNullable(groupName))
							.map(s -> aceBridgeService.getExtendedGroupNome(s))
							.orElse(groupName));
				} else {
					ctx.setVariable("profile", "oiv");
					ctx.setVariable("groupname", cnrgroupService.findDisplayName(groupName));
				}
			}
			ctx.setVariable("taskName", taskName);
			if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {
				ctx.setVariable("profile", "cnr");
			} else if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
				ctx.setVariable("profile", "oiv");
			} else if (Arrays.asList(env.getActiveProfiles()).contains("showcase")) {
				ctx.setVariable("profile", "showcase");
			}
			
			LOGGER.info("Recupero dell'email per l'utente "+ username);

			String htmlContent = templateEngine.process(notificationType, ctx);
			String mailUtente = aceService.getUtente(username).getEmail();

			LOGGER.info("Invio della mail all'utente "+ username +" con indirizzo "+ mailUtente);

			String subject = getCustomSubject(variables, key);
			if (mailConfig.isMailActivated()) {
				// In produzione mando le email ai veri destinatari
			    String procDefId = variables.get("processDefinitionId").toString().split(":")[0];
			    Blacklist bl = blacklistService.findOneByEmailAndKey(mailUtente, procDefId);
			    if (bl != null) {
			        LOGGER.info("L'utente {} ha richiesto di non ricevere notifiche per il flusso {}", mailUtente, key);
			    } else {
    				if(mailUtente != null) {
    					sendEmail(mailUtente,
								subject,
    							htmlContent,
    							false,
    							true);
    				} else {
    				    LOGGER.warn("L'utente {} non ha un'email associata", username);
    				}
			    }
			}
			
			// Per le prove mando *tutte* le email agli indirizzi di prova (e non ai veri destinatari)
			mailConfig.getMailRecipients().stream()
			.filter(s -> !s.isEmpty())
			.forEach(s -> {
				LOGGER.debug("Invio mail a {} con titolo Notifica relativa al flusso {} del tipo {} nello stato {} e con contenuto {}",
						s,
						key,
						notificationType,
						variables.get("stato"),
						StringUtils.abbreviate(htmlContent, 30));
				LOGGER.trace("Corpo email per intero: {}", htmlContent);
				sendEmail(s, subject, htmlContent, false, true);
			});
		} catch (Exception e) {
			LOGGER.error("Errore nell'invio della mail", e);
			throw e;
		}
	}

	private String getCustomSubject(Map<String, Object> variables, String key) {
		String subject = "Notifica relativa al flusso " + key; //subject di default
		String processDefinition = ((String)variables.get("processDefinitionId")).split(":")[0];
		switch (processDefinition){
			case "covid19":
//Notifica FLUSSO Monitoraggio SW - PROGRAMMAZIONE maggio 2021 di massimo fraticelli
				subject = "Notifica FLUSSO Monitoraggio SW - " + variables.get("tipoAttivita") +
						" " + variables.get("mese") + " " + variables.get("anno") +
						" di " + variables.get("nomeCognomeUtente");
				break;
			case "missioni":
//Notifica FLUSSO MISSIONI - ORDINE missione di massimo fraticelli in data 21-4-2020
				subject = "Notifica FLUSSO MISSIONI - " + ((String)variables.get("tipologiaMissione")).toUpperCase() +
						" missione di " + variables.get("userNameUtenteMissione") +
						" in data " + formatoDataUF.format((Date) variables.get("startDate"));
				break;
			case "accordi-internazionali-domande":
				if(((String)variables.get("stato")).equalsIgnoreCase("validazione"))
//Notifica per sola conoscenza FLUSSO Accordi Internazionali - VALIDAZIONE (Bando: CNR/CAS (Rep. Ceca) - triennio 2022-2024) di massimo fraticelli
					subject = "Notifica per sola conoscenza FLUSSO Accordi Internazionali - VALIDAZIONE" +
							" (Bando: " + variables.get("bando") +
							") di " + variables.get("nomeCognomeRichiedente");
				else
//Notifica FLUSSO Accordi Internazionali - APPROVAZIONE (Bando: CNR/CAS (Rep. Ceca) - triennio 2022-2024) di massimo fraticelli
					subject = "Notifica FLUSSO Accordi Internazionali - APPROVAZIONE" +
							" (Bando: " + variables.get("bando") +
							") di " + variables.get("nomeCognomeRichiedente");

				break;
		}
		return subject;
	}
}