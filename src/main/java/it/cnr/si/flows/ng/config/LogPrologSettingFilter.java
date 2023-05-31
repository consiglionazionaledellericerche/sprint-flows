package it.cnr.si.flows.ng.config;


import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.SecurityService;

import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class LogPrologSettingFilter extends GenericFilterBean {

    @Inject
    private SecurityService securityService;
    @Inject
    private SecurityUtils securityUtils;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (!securityUtils.getCurrentUserAuthorities().contains("PREVIOUS_ADMINISTRATOR"))
                MDC.put("currentUser", securityService.getCurrentUserLogin());
            else
                MDC.put("currentUser", "admin as " + securityService.getCurrentUserLogin());
        } catch (Exception e) {
            MDC.put("currentUser", "undefined");
        }
        chain.doFilter(request, response);
    }
}
