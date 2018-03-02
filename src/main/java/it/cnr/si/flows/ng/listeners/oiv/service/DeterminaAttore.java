package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;
import javax.inject.Inject;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import it.cnr.si.service.UserService;




@Service
public class DeterminaAttore {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminaAttore.class);

    @Inject
    private UserService userService;

	public void determinaIstruttore(DelegateExecution execution) throws IOException, ParseException {
		
		String currentName = userService.getUserWithAuthorities().getFirstName() + " " + userService.getUserWithAuthorities().getLastName();
		String currentUsername = userService.getUserWithAuthorities().getLogin();

		execution.setVariable("nomeIstruttore", currentName);
		execution.setVariable("istruttoreIncaricato", currentUsername);
		LOGGER.debug("--- nomeIstruttore: {} - istruttoreIncaricato: {}", currentName, currentUsername);

	}

}
