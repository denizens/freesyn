package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Table.Cell;

import bnw.abm.intg.algov2.framework.models.LinkRules;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

/**
 * Abstract LinkRulesBuilder class. This class provides the support for link rules syntax.
 * [ReferenceAgentType].link_type.[TargetAgentType1,weight1][TargetAgentType2,weight2]
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
public abstract class LinkRulesBuilder {
    private LinkRules linkRules, linkRulesOriginal;

    public LinkRulesBuilder() {
        linkRules = new LinkRules();
    }

    public LinkRulesBuilder(int size) {
        linkRules = new LinkRules(size);
    }

    public void register() {
        this.build();
        LinkRulesWrapper.setLinkRules(linkRules);
    }

    public void filterEmptyAgentTypes(Map<ReferenceAgentType, Integer> targetAgentCounts) {
        synchronized (linkRules) {
            linkRulesOriginal = linkRules;
            linkRules = new LinkRules(linkRulesOriginal.size());
            for (Cell<ReferenceAgentType, LinkType, List<TargetAgentType>> entry : linkRulesOriginal.cellSet()) {
                if (targetAgentCounts.get(entry.getRowKey()) > 0) {
                    ArrayList<TargetAgentType> values = new ArrayList<>(entry.getValue());
                    values.removeIf(v -> targetAgentCounts.get(ReferenceAgentType.getExisting(v.getTypeID())) == 0);
                    linkRules.put(entry.getRowKey(), entry.getColumnKey(), values);
                }
            }
        }
        LinkRulesWrapper.setLinkRules(linkRules);
    }

    public void reset() {
        linkRules = linkRulesOriginal;
        LinkRulesWrapper.setLinkRules(linkRules);
    }

    protected boolean addRule(ReferenceAgentType referenceAgentType, LinkType linkType, List<TargetAgentType> targetAgentTypes) {

        if (linkType.getMaxLinks() < 0) {
            Log.errorAndExit("Maximum number of links not defined for LinkType: " + linkType + " for reference agent: " + referenceAgentType,
                    new IllegalStateException(), EXITCODE.PROGERROR);
        }
        if (linkType.getMinLinks() < 0) {
            Log.errorAndExit("Minimum number of links not defined for LinkType: " + linkType + " for reference agent: " + referenceAgentType,
                    new IllegalStateException(), EXITCODE.PROGERROR);
        }

        List<TargetAgentType> out = linkRules.put(referenceAgentType, linkType, targetAgentTypes);
        if (out == null) {
            return true;
        } else {
            Log.warn("Overwriting existing Link Rule. Please note specifying same link type as both active and passive is not supported: "
                    + referenceAgentType + linkType + out);
            return false;
        }
    }

    protected abstract void build();

    /**
     * Finds AgentType instance by typeId and decorates it as a TargetAgentType. If there is no existing AgentType instance then a new one is
     * created
     * 
     * @param typeId
     *            AgentType ID of target agent
     * @param weight
     *            Weight of the link between reference agent and target agent
     * @return A decorated TargetAgentType instance of AgentType represented by typeId
     */
    protected TargetAgentType createTargetAgentTypeInstance(int typeId, double weight) {
        TargetAgentType instance = null;
        instance = new TargetAgentType(typeId, weight);
        return instance;
    }

    /**
     * Finds AgentType instance by typeId and decorates it as a ReferenceAgentType. If there is no existing AgentType instance then a new one is
     * created
     * 
     * @param typeId
     *            Csv input string
     * @return list of AgentTypes
     */
    protected ReferenceAgentType createReferenceAgentType(int typeId) {
        return ReferenceAgentType.getInstance(typeId);
    }
}