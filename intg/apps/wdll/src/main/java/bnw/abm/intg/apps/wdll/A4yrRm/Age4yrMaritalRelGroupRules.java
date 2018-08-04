package bnw.abm.intg.apps.wdll.A4yrRm;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupRulesBuilder;

public class Age4yrMaritalRelGroupRules extends GroupRulesBuilder {

    @Override
    protected void build() {
        int genderGap = 52;
        for (int i = 1; i <= 12; i++) {

            GroupType groupType = GroupType.getInstance(i);
            addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(i));

            List<ReferenceAgentType> groupHeads;
            switch (i) {
            case 1:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(4, 25));// S-m
                groupHeads.addAll(getReferenceAgents(4 + genderGap, 25 + genderGap));// S-f
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, groupHeads);
                break;
            default:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(4, 25));// S-m
                groupHeads.addAll(getReferenceAgents(30, 51));// M-m
                groupHeads.addAll(getReferenceAgents(4 + genderGap, 25 + genderGap));// S-f
                groupHeads.addAll(getReferenceAgents(30 + genderGap, 51 + genderGap));// M-f
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, groupHeads);
                break;
            }

        }

    }

    /**
     * Creates a list of reference agents
     * 
     * @param fromIDInclusive
     *            starting reference agent id
     * @param toIDInclusive
     *            ending reference agent it
     * @return list of reference agent instances
     */
    public static List<ReferenceAgentType> getReferenceAgents(int fromIDInclusive, int toIDInclusive) {
        List<ReferenceAgentType> refs = new ArrayList<>((toIDInclusive < fromIDInclusive) ? 0 : toIDInclusive - fromIDInclusive + 1);
        for (int i = fromIDInclusive; i <= toIDInclusive; i++) {
            refs.add(ReferenceAgentType.getInstance(i));
        }
        return refs;
    }
}
