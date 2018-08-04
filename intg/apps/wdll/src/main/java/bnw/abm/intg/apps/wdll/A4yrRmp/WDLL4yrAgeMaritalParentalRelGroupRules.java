package bnw.abm.intg.apps.wdll.A4yrRmp;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.templates.GroupRulesBuilder;

public class WDLL4yrAgeMaritalParentalRelGroupRules extends GroupRulesBuilder {

    @Override
    protected void build() {
        int genderGap = 208;
        for (int i = 1; i <= 12; i++) {

            GroupType groupType = GroupType.getInstance(i);
            addRule(groupType, GroupRuleKeys.MAX_MEMBERS, new Integer(i));

            List<ReferenceAgentType> groupHeads;
            switch (i) {
            case 1:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(4, 25));// SNC -m
                groupHeads.addAll(getReferenceAgents(212,233));// SNc-f
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, groupHeads);
                break;
            case 2:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(30, 51));// SU15c-m
                groupHeads.addAll(getReferenceAgents(56, 77));// SO15c-m
                groupHeads.addAll(getReferenceAgents(108, 129));// RNc-m
                groupHeads.addAll(getReferenceAgents(30 + genderGap, 51 + genderGap));// SU15c-f
                groupHeads.addAll(getReferenceAgents(56 + genderGap, 77 + genderGap));// SO15c-f
                groupHeads.addAll(getReferenceAgents(108 + genderGap, 129 + genderGap));// RNc-f
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, groupHeads);
                break;
            case 3:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(30, 51));// SU15c-m
                groupHeads.addAll(getReferenceAgents(56, 77));// SO15c-m
                groupHeads.addAll(getReferenceAgents(82, 103));// SUO15c-m
                groupHeads.addAll(getReferenceAgents(134, 155));// RU15c-m
                groupHeads.addAll(getReferenceAgents(160, 181));// RO15c-m
                groupHeads.addAll(getReferenceAgents(30 + genderGap, 51 + genderGap));// SU15c-f
                groupHeads.addAll(getReferenceAgents(56 + genderGap, 77 + genderGap));// SUO15c-f
                groupHeads.addAll(getReferenceAgents(82 + genderGap, 103 + genderGap));// SO15c-f
                groupHeads.addAll(getReferenceAgents(134 + genderGap, 155 + genderGap));// RU15c-f
                groupHeads.addAll(getReferenceAgents(160 + genderGap, 181 + genderGap));// RO15c-f
                addRule(groupType, GroupRuleKeys.REFERENCE_AGENT, groupHeads);
                break;
            default:
                groupHeads = new ArrayList<>();
                groupHeads.addAll(getReferenceAgents(30, 51));// SU15c-m
                groupHeads.addAll(getReferenceAgents(56, 77));// SO15c-m
                groupHeads.addAll(getReferenceAgents(82, 103));// SUO15c-m
                groupHeads.addAll(getReferenceAgents(134, 155));// RU15c-m
                groupHeads.addAll(getReferenceAgents(160, 181));// RO15c-m
                groupHeads.addAll(getReferenceAgents(186, 207));// RUO15c-m
                groupHeads.addAll(getReferenceAgents(30 + genderGap, 51 + genderGap));// SU15c-f
                groupHeads.addAll(getReferenceAgents(56 + genderGap, 77 + genderGap));// SO15c-f
                groupHeads.addAll(getReferenceAgents(82 + genderGap, 103 + genderGap));// SO15c-f
                groupHeads.addAll(getReferenceAgents(134 + genderGap, 155 + genderGap));// RU15c-f
                groupHeads.addAll(getReferenceAgents(160 + genderGap, 181 + genderGap));// RO15c-f
                groupHeads.addAll(getReferenceAgents(186 + genderGap, 207 + genderGap));// RUO15c-f
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
