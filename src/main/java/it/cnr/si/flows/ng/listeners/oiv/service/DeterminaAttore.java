package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;
import javax.inject.Inject;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.UserService;




@Service
public class DeterminaAttore {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminaAttore.class);

    @Inject
    private FlowsUserService flowsUserService;

	public void determinaIstruttore(DelegateExecution execution) throws IOException, ParseException {
		
		String currentName = flowsUserService.getUserWithAuthorities().getFirstName() + " " + flowsUserService.getUserWithAuthorities().getLastName();
		String currentUsername = flowsUserService.getUserWithAuthorities().getLogin();
		String userPhone = "1";
		String sessoIstruttore = "m";
		if(flowsUserService.getUserWithAuthorities().getPhone() != null){
		    userPhone = flowsUserService.getUserWithAuthorities().getPhone();
		}
//		if(flowsUserService.getUserWithAuthorities().getGender() != null){
//			sessoIstruttore = flowsUserService.getUserWithAuthorities().getGender();
//		}

		execution.setVariable("nomeIstruttore", currentName);
		execution.setVariable("istruttoreIncaricato", currentUsername);
		execution.setVariable("telefonoIstruttore", userPhone);
		execution.setVariable("sessoIstruttore", sessoIstruttore);
		
		LOGGER.debug("--- nomeIstruttore: {} - istruttoreIncaricato: {} - telefonoIstruttore: {}- sessoIstruttore: {}", currentName, currentUsername, userPhone, sessoIstruttore);

	}

}

