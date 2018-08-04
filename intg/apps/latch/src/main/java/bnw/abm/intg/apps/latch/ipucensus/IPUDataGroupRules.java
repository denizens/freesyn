package bnw.abm.intg.apps.latch.ipucensus;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupRulesBuilder;


import java.util.ArrayList;
import java.util.List;

/**
 * @author wniroshan 12 May 2018
 */
public class IPUDataGroupRules extends GroupRulesBuilder {

    /**
     * Creates a list of reference agents
     *
     * @param fromIDInclusive starting reference agent id
     * @param toIDInclusive   ending reference agent it
     * @return list of reference agent instances
     */
    public static List<ReferenceAgentType> getReferenceAgents(int fromIDInclusive, int toIDInclusive) {
        List<ReferenceAgentType> refs = new ArrayList<>((toIDInclusive < fromIDInclusive) ? 0 : toIDInclusive - fromIDInclusive + 1);
        for (int i = fromIDInclusive; i <= toIDInclusive; i++) {
            refs.add(ReferenceAgentType.getInstance(i));
        }
        return refs;
    }

    @Override
    protected void build() {
        for (int i = 0; i <= 59; i++) {
            GroupType groupType = GroupType.getInstance(i);
            if (i < 8) {
                addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(0));
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, new ArrayList<>(0));
                continue;
            }

            int coupleWithNoChildren = 0, coupleWithChildren = 1, oneParentFamily = 2, otherFamily = 3, lonePersonHousehold = 8,
                    groupHousehold = 9;
            int familyTypes = 4; // There are only 4 family types Couples w/n children, couples w/ children, One parent family and
            // Other family
            int actualHouseholdTypes = 10; // Household type without considering size

            int maxMembers = (i / actualHouseholdTypes) + 1;
            addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(maxMembers));

            if (i == lonePersonHousehold) {
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, getReferenceAgents(AT.ST_M_1P.getCatId(), AT.LS_F_1P.getCatId()));

            }else if (i % actualHouseholdTypes == lonePersonHousehold) {
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

}
