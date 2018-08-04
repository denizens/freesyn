package bnw.abm.intg.apps.latch.algov1;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupRulesBuilder;
import bnw.abm.intg.apps.latch.algov2.AT;


class AlgoV1LatchGroupRules extends GroupRulesBuilder {

    @Override
    protected void build() {
        for (int i = 0; i <= 111; i++) {
            GroupType groupType = GroupType.getInstance(i);
            if (i < 12) {
                addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(0));
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, new ArrayList<>(0));
                continue;
            }

            int coupleWithNoChildren = 0, coupleWithChildren = 1, oneParentFamily = 2, otherFamily = 3, lonePersonHousehold = 12,
                    groupHousehold = 13;
            int familyTypes = 4; // There are only 4 family types Couples w/n children, couples w/ children, One parent family and Other family
            int actualHouseholdTypes = 14; // Household type without considering size

            int maxMembers = (i / actualHouseholdTypes) + 1;
            addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(maxMembers));

            if (i == lonePersonHousehold) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_1P.getCatId(), AT.LS_F_1P.getCatId()));

            } else if (i % actualHouseholdTypes == lonePersonHousehold) {
                // This is 2+ member LonePerson households. There are no such Hhs
                // This block catches all impossible LonePerson households and continue without doing anything. I don't have to worry about
                // LonePerson households anymore
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, new ArrayList());
            } else if (i % actualHouseholdTypes == groupHousehold) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_GRP.getCatId(), AT.LS_F_GRP.getCatId()));
            } else if ((i % actualHouseholdTypes) % 4 == coupleWithNoChildren) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_MAR.getCatId(), AT.LS_F_MAR.getCatId()));
            } else if ((i % actualHouseholdTypes) % 4 == coupleWithChildren) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_MAR.getCatId(), AT.LS_F_MAR.getCatId()));
            } else if ((i % actualHouseholdTypes) % 4 == oneParentFamily) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_LNPAR.getCatId(), AT.LS_F_LNPAR.getCatId()));
            } else if ((i % actualHouseholdTypes) % 4 == otherFamily) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_REL.getCatId(), AT.LS_REL.getCatId()));
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
