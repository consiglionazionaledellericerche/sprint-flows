package it.cnr.si.flows.ng.service;

import org.activiti.engine.impl.RuntimeServiceImpl;


public class FlowsRuntimeService extends RuntimeServiceImpl {
	@Override
	  public void addGroupIdentityLink(String processInstanceId, String groupId, String identityLinkType) {
		  super.deleteGroupIdentityLink(processInstanceId, groupId, identityLinkType);
		  super.addGroupIdentityLink(processInstanceId, groupId, identityLinkType);
	  }

}
