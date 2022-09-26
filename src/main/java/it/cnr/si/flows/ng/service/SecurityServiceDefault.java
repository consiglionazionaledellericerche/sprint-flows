package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.CNRUser;
import it.cnr.si.model.UserInfoDto;
import it.cnr.si.service.SecurityService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Profile("iss")
public class SecurityServiceDefault implements SecurityService, InitializingBean {
    @Override
    public Optional<CNRUser> getUser() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMatricola() {
        return Optional.empty();
    }

    @Override
    public Optional<UserInfoDto> getUserInfo() {
        return Optional.empty();
    }

    @Override
    public String getCurrentUserLogin() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean isCurrentUserInRole(String s) {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
