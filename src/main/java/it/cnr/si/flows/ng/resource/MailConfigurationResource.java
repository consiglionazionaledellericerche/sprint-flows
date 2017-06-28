package it.cnr.si.flows.ng.resource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.security.AuthoritiesConstants;

@RestController
@RequestMapping("api/mail")
public class MailConfigurationResource {

    @Autowired
    private MailConfguration mailConfig;

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> setUsers(@RequestParam("users") String users) {
        List<String> userList = Arrays.asList(users.split(","));
        mailConfig.setMailRecipients(userList);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<String> getUsers() {
        return ResponseEntity.ok(mailConfig.getMailRecipients().stream().collect(Collectors.joining(",")));
    }

    @RequestMapping(value = "/active", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> setActive(@RequestParam("active") Boolean active) {
        mailConfig.setMailActivated(active);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/active", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Boolean> setActive() {
        return ResponseEntity.ok(mailConfig.isMailActivated());
    }

}
