package it.cnr.si.flows.ng.aop;

import java.util.List;

import org.activiti.engine.ManagementService;
import org.activiti.engine.impl.HistoricProcessInstanceQueryImpl;
import org.activiti.engine.impl.ManagementServiceImpl;

public class FlowsHistoricProcessInstanceQuery extends HistoricProcessInstanceQueryImpl {

    private static final long serialVersionUID = -6296421487043345841L;
    protected List<String> visibleToGroups;

    public FlowsHistoricProcessInstanceQuery(ManagementService managementService) {
        super(((ManagementServiceImpl) managementService).getCommandExecutor());
    }

    public List<String> getVisibleToGroups() {
        return visibleToGroups;
    }

    public FlowsHistoricProcessInstanceQuery setVisibleToGroups(List<String> visibleToGroups) {
        this.visibleToGroups = visibleToGroups;
        return this;
    }

}
