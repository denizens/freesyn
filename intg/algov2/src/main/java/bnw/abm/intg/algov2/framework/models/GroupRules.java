package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;

public class GroupRules extends ForwardingTable<GroupType, GroupRuleKeys, Object> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8559204538470697563L;
    private final Table<GroupType, GroupRuleKeys, Object> delegate;

    public GroupRules() {
        delegate = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
    }

    @Override
    protected Table<GroupType, GroupRuleKeys, Object> delegate() {
        return delegate;
    }

    public static enum GroupRuleKeys {
        REFERENCE_AGENT,
        LINKS_OF_REFERENCE,
        MAX_MEMBERS;
    }

    final public static class GroupRulesWrapper {
        private static GroupRules groupRules;

        public static void setGroupRules(GroupRules groupRules) {
            GroupRulesWrapper.groupRules = groupRules;
        }

        public static Object get(GroupType rowKey, GroupRuleKeys columnKey) {
            return groupRules.get(rowKey, columnKey);
        }

        public static Map<GroupRuleKeys, Object> row(GroupType rowKey) {
            return groupRules.row(rowKey);
        }

        public static boolean contains(GroupType rowKey, GroupRuleKeys columnKey) {
            return groupRules.contains(rowKey, columnKey);
        }

        public static GroupRules getGroupRules() {
            return groupRules;
        }

        public static Set<GroupType> rowKeySet() {
            return groupRules.rowKeySet();
        }

    }
}
