package it.cnr.si.flows.ng.repository;

import java.util.Collections;
import java.util.List;

import org.activiti.engine.ManagementService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.HistoricProcessInstanceQueryImpl;
import org.activiti.engine.impl.ManagementServiceImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.CommandContext;

/**
 *
 * @author mtrycz + cironepa
 *
 * Questa classe ci serve come estensione della questy che offre Activity
 * Abbiamo voluto usare questo approccio per questioni di performance, ma activiti non forniva tutte le query che ci piaceva avere
 * in particolare per quel che riguarda la visibilita' sui flussi (che salviamo negli IdentityLinks)
 *
 * Un esempio di utilizzo di questa classe e' in it.cnr.si.flows.ng.serviceFlowsProcessInstanceService.search
 *
 *
 *
 * Questo file e' appaiato con src/main/resources/mapper/FlowsHistoricProcessInstanceMapper.xml
 *
 */

public class FlowsHistoricProcessInstanceQuery extends HistoricProcessInstanceQueryImpl {

    private static final long serialVersionUID = -6296421487043345841L;
    protected List<String> visibleToGroups;
    protected String visibleToUser;

    public String getVisibleToUser() {
        return visibleToUser;
    }

    public void setVisibleToUser(String visibleToUser) {
        this.visibleToUser = visibleToUser;
    }

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

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        ensureVariablesInitialized();
        if (visibleToGroups != null) {
            return executeCountWithVisibility(this);
        } else
            return commandContext
              .getHistoricProcessInstanceEntityManager()
              .findHistoricProcessInstanceCountByQueryCriteria(this);
      }

    public List<HistoricProcessInstance> executeList(CommandContext commandContext, Page page) {
        checkQueryOk();
        ensureVariablesInitialized();
        List<HistoricProcessInstance> results = null;
        if (visibleToGroups != null || visibleToUser != null) {
            results = executeListWithVisibility(this);
        } else if (includeProcessVariables) {
            results = commandContext.getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesAndVariablesByQueryCriteria(this);
        } else {
            results = commandContext.getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesByQueryCriteria(this);
        }

        for (HistoricProcessInstance processInstance : results) {
            localize(processInstance, commandContext);
        }

        return results;
    }



    private long executeCountWithVisibility(FlowsHistoricProcessInstanceQuery flowsHistoricProcessInstanceQuery) {
        return (Long) getDbSession().selectOne("selectFlowsHistoricProcessInstanceCountByQueryCriteria", flowsHistoricProcessInstanceQuery);

    }

    @SuppressWarnings("unchecked")
    private List<HistoricProcessInstance> executeListWithVisibility(FlowsHistoricProcessInstanceQuery flowsHistoricProcessInstanceQuery) {

        // paging doesn't work for combining process instances and variables due to an outer join, so doing it in-memory
        if (flowsHistoricProcessInstanceQuery.getFirstResult() < 0 || flowsHistoricProcessInstanceQuery.getMaxResults() <= 0) {
            return Collections.EMPTY_LIST;
        }

        int firstResult = flowsHistoricProcessInstanceQuery.getFirstResult();
        int maxResults = flowsHistoricProcessInstanceQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (flowsHistoricProcessInstanceQuery.getProcessInstanceVariablesLimit() != null) {
            flowsHistoricProcessInstanceQuery.setMaxResults(flowsHistoricProcessInstanceQuery.getProcessInstanceVariablesLimit());
        } else {
            flowsHistoricProcessInstanceQuery.setMaxResults(Context.getProcessEngineConfiguration().getHistoricProcessInstancesQueryLimit());
        }
        flowsHistoricProcessInstanceQuery.setFirstResult(0);

        List<HistoricProcessInstance> instanceList = getDbSession().selectListWithRawParameterWithoutFilter("selectFlowsHistoricProcessInstancesWithVariablesByQueryCriteria",
                flowsHistoricProcessInstanceQuery, flowsHistoricProcessInstanceQuery.getFirstResult(), flowsHistoricProcessInstanceQuery.getMaxResults());

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = Math.min(maxResults, instanceList.size());
                return instanceList.subList(0, toIndex);
            }
        }

        return Collections.EMPTY_LIST;
    }

    private DbSqlSession getDbSession() {
        return Context.getCommandContext().getSession(DbSqlSession.class);
    }
}
