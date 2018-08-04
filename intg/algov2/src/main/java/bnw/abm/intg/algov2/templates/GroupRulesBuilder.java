package bnw.abm.intg.algov2.templates;

import bnw.abm.intg.algov2.framework.models.GroupRules;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRulesWrapper;
import bnw.abm.intg.algov2.framework.models.GroupType;

public abstract class GroupRulesBuilder {

    private final GroupRules groupRules;

    public GroupRulesBuilder() {
        groupRules = new GroupRules();
    }

    /**
     * Processes Group rules and registers rules in GroupRulesWrapper static class
     */
    public void register() {
        this.build();
        GroupRulesWrapper.setGroupRules(this.groupRules);
    }

    /**
     * Builds the Group Rules
     */
    protected abstract void build();

    /**
     * Adds rule to group rules
     * 
     * @param groupType
     *            Group type the rule is valid to
     * @param keyType
     *            Type of the rule
     * @param ruleValue
     *            The rule values
     */
    protected void addRule(GroupType groupType, GroupRuleKeys keyType, Object ruleValue) {
        if (groupRules != null) {
            groupRules.put(groupType, keyType, ruleValue);
        }

    }
}
