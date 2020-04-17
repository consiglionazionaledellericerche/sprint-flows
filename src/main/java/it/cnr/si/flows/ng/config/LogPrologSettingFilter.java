package it.cnr.si.flows.ng.config;

import it.cnr.si.flows.ng.utils.SecurityUtils;
import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class LogPrologSettingFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (!SecurityUtils.getCurrentUserAuthorities().contains("PREVIOUS_ADMINISTRATOR"))
                MDC.put("currentUser", SecurityUtils.getCurrentUserLogin());
            else
                MDC.put("currentUser", "admin as " + SecurityUtils.getCurrentUserLogin());
        } catch (Exception e) {
            MDC.put("currentUser", "undefined");
        }
        chain.doFilter(request, response);
    }
}
