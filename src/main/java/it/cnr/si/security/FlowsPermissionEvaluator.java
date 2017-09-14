package it.cnr.si.security;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import com.google.common.collect.Lists;


@Slf4j @Configuration
public class FlowsPermissionEvaluator implements PermissionEvaluator {

	@Inject
	KieBase kieBase;

	@Override
	public boolean hasPermission(Authentication authentication, Object target,
			Object permission) {
		log.info("hasPermission({}, {}, {}) called", authentication, target,
				permission);
		StatelessKieSession session = null;
		final PermissionCheck check = new PermissionCheck(target, permission);

		session = kieBase.newStatelessKieSession();
		final List<Command<?>> commands = Lists.newArrayList();
		commands.add(CommandFactory.newInsert(target));
		commands.add(CommandFactory.newInsert(check));
		commands.add(CommandFactory.newInsert(authentication));

		log.debug("session is {}", session);
		log.debug("authentication = {}, principal = {}, details = {}", 
		    authentication, authentication.getPrincipal(), authentication.getDetails());
		session.execute(CommandFactory.newBatchExecution(commands));

		return check.isGranted();
	}

	@Override
	public boolean hasPermission(Authentication authentication,
			Serializable targetId, String targetType, Object permission) {
		log.error("hasPermission(Authentication, Serializable, String, Object) called");
		throw new RuntimeException("ID based permission evaluation currently not supported.");
	}
}
