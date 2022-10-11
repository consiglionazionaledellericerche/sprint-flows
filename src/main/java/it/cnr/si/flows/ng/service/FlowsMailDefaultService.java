package it.cnr.si.flows.ng.service;

import it.cnr.si.service.AceService;
import it.cnr.si.service.CnrgroupService;
import it.cnr.si.service.FlowsUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;


@Service
@Profile("!cnr")
public class FlowsMailDefaultService extends FlowsMailService {

    @Inject
    private CnrgroupService cnrgroupService;

    @Inject
    private FlowsUserService flowsUserService;

    @Override
    public String getGroupDisplayName(String groupName) {
        return cnrgroupService.findDisplayName(groupName);
    }

    @Override
    public String getEmaiByUser(String user) {
        return flowsUserService.getUserWithAuthoritiesByLogin(user).get().getEmail();
    }


        
}