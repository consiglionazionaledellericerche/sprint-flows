package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.Blacklist;
import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.inject.Inject;
import java.util.*;

import static it.cnr.si.flows.ng.utils.Utils.formatoDataUF;


@Service
@Profile("cnr")
public class FlowsMailCnrService extends FlowsMailService {

    private AceBridgeService aceBridgeService;
    @Autowired(required = false) //TODO
    private AceService aceService;

    @Override
    public String getGroupDisplayName(String groupName) {
        return Optional.ofNullable(aceBridgeService)
                .flatMap(aceBridgeService -> Optional.ofNullable(groupName))
                .map(s -> aceBridgeService.getExtendedGroupNome(s))
                .orElse(groupName);
    }

    @Override
    public String getEmaiByUser(String user) {
        return aceService.getUtente(user).getEmail();
    }


        
}