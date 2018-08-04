package bnw.abm.intg.algov2.framework.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

public class Population extends ForwardingTable<GroupType, AgentType, MatrixCell> {
    /**
     * Data matrix Table<GroupType,AgentType,MatixCell>
     */
    private Table<GroupType, AgentType, MatrixCell> matrix = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);

    @Override
    protected Table<GroupType, AgentType, MatrixCell> delegate() {
        return matrix;
    }

    private List<Group> groups = new ArrayList<>();
    private int size = 0;
    private Map<GroupType, Integer> groupSummary = new LinkedHashMap<>();
    private Map<ReferenceAgentType, Integer> agentSummary = new LinkedHashMap<>();
    private Map<GroupType, Integer> groupTypeCounts;
    private Map<ReferenceAgentType, Integer> agentTypeCounts;
    private List<Double> errorList = new ArrayList<>(20);

    public List<Double> getErrorList() {
        return errorList;
    }

    public void addToErrorList(Double error) {
        this.errorList.add(error);
    }

    public Population(IPFPTable targetDistribution) {
        setTargetProportions(targetDistribution);
        setGroupTypesInGroupSummaryMap(targetDistribution.rowKeySet());
        for (GroupType gType : targetDistribution.rowKeySet()) {
            groupSummary.put(gType, 0);
        }
        for (ReferenceAgentType aType : targetDistribution.columnKeySet()) {
            agentSummary.put(aType, 0);
        }
    }

    public Population(Map<GroupType, Integer> targetGroupTypeCounts, Map<ReferenceAgentType, Integer> targetAgentTypesCount) {

        this.groupTypeCounts = new LinkedHashMap<>(targetGroupTypeCounts);
        this.agentTypeCounts = new LinkedHashMap<>(targetAgentTypesCount);

        this.agentTypeCounts.forEach((k, v) -> this.agentSummary.put(k, 0));
        this.groupTypeCounts.forEach((k, v) -> this.groupSummary.put(k, 0));

        for (GroupType gType : targetGroupTypeCounts.keySet()) {
            for (ReferenceAgentType aType : targetAgentTypesCount.keySet()) {
                this.matrix.put(gType, aType, new MatrixCell(0));
            }
        }
    }

    private Population() {
    }

    public Population copy() {
        Population copy = new Population();
        for (GroupType gt : delegate().rowKeySet()) {
            for (AgentType at : delegate().columnKeySet()) {
                MatrixCell c = new MatrixCell(this.delegate().get(gt, at).targetProportion);
                c.agentCount = this.delegate().get(gt, at).agentCount;
                c.currentProportion = this.delegate().get(gt, at).currentProportion;
            }
        }
        copy.groups.addAll(this.groups);
        copy.groupSummary.putAll(this.groupSummary);
        copy.agentSummary.putAll(this.agentSummary);
        copy.groupTypeCounts = (this.groupTypeCounts == null) ? null : new LinkedHashMap<>(this.groupTypeCounts);
        copy.agentTypeCounts = (this.agentTypeCounts == null) ? null : new LinkedHashMap<>(this.agentTypeCounts);
        copy.errorList.addAll(this.errorList);

        return copy;
    }

    public Table<GroupType, AgentType, MatrixCell> matrix() {
        return this.matrix;
    }

    public Set<GroupType> groupTypes() {
        return this.matrix.rowKeySet();
    }

    public Set<AgentType> agentTypes() {
        return this.matrix.columnKeySet();
    }

    public MatrixCell get(GroupType gtype, AgentType atype) {
        return this.matrix.get(gtype, atype);
    }

    public int cellCount() {
        return this.matrix.values().size();
    }

    /**
     * Adds a group to the population
     * 
     * @param group
     *            Group instance to add
     */
    public void addGroup(Group group) {
        this.groups.add(group);
        this.size += group.size();
        GroupType selectedGroupType = group.type();
        for (Agent mem : group.getMembers()) {
            MatrixCell curr = matrix.get(selectedGroupType, ReferenceAgentType.getExisting(mem.getType()));
            curr.agentCount += 1;
            matrix.put(selectedGroupType, ReferenceAgentType.getExisting(mem.getType()), curr);
            int agentsCount = agentSummary.get(ReferenceAgentType.getExisting(mem.getType()));
            agentSummary.put(ReferenceAgentType.getExisting(mem.getType()), agentsCount + 1);
        }
        groupSummary.put(selectedGroupType, groupSummary.get(selectedGroupType) + 1);
    }

    /**
     * Removes a group from current population
     * 
     * @param group
     *            The group to be removed in current groups list
     */
    public void removeGroup(Group group) {
        if (this.groups.remove(group)) {
            this.size -= group.size();
            GroupType removedGroupType = group.type();
            for (Agent mem : group.getMembers()) {
                MatrixCell curr = matrix.get(removedGroupType, ReferenceAgentType.getExisting(mem.getType()));
                curr.agentCount -= 1;
                matrix.put(removedGroupType, ReferenceAgentType.getExisting(mem.getType()), curr);
                int agentsCount = agentSummary.get(ReferenceAgentType.getExisting(mem.getType()));
                agentSummary.put(ReferenceAgentType.getExisting(mem.getType()), agentsCount - 1);
            }
            groupSummary.put(removedGroupType, groupSummary.get(removedGroupType) - 1);
        } else {
            Log.errorAndExit("Group you are trying to remove not in the population", EXITCODE.PROGERROR);
        }
    }

    /**
     * Returns a current groups list
     * 
     * @return Groups list
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * Summary of group types and number of such groups in population
     * 
     * @return Map of GroupTypes and number of groups in each type
     */
    public Map<GroupType, Integer> getGroupsSummary() {
        return this.groupSummary;
    }

    /**
     * Summary of agent types in population
     * 
     * @return Map of agent types and number of groups in each type
     */
    public Map<ReferenceAgentType, Integer> getAgentsSummary() {
        return this.agentSummary;
    }

    /**
     * Size of the current population
     * 
     * @return number of agents
     */
    public int size() {
        return this.size;
    }

    private void setTargetProportions(IPFPTable populationDistribution) {
        for (GroupType row : populationDistribution.rowKeySet()) {
            for (AgentType col : populationDistribution.columnKeySet()) {
                MatrixCell c = new MatrixCell(populationDistribution.get(row, col));
                matrix.put(row, col, c);
            }
        }
    }

    private void setGroupTypesInGroupSummaryMap(Collection<GroupType> groupTypes) {
        for (GroupType gt : groupTypes) {
            this.groupSummary.put(gt, 0);
        }
    }

    public void printPopulationMat_IPFP() {
        for (GroupType row : matrix.rowKeySet()) {
            for (AgentType col : matrix.columnKeySet()) {
                System.out.print(matrix.get(row, col).targetProportion + " ");
            }
            System.out.println();
        }
    }

    public void printPopulationMat_AgentCount() {
        for (GroupType row : matrix.rowKeySet()) {
            for (AgentType col : matrix.columnKeySet()) {
                System.out.print(matrix.get(row, col).agentCount + " ");
            }
            System.out.println();
        }
    }

    public void printPopulationMat_CurrentProportion() {
        for (GroupType row : matrix.rowKeySet()) {
            for (AgentType col : matrix.columnKeySet()) {
                System.out.print(matrix.get(row, col).currentProportion + " ");
            }
            System.out.println();
        }
    }

    /**
     * Sets the the value that should be returned when MatrixCell.toString() method is called
     * 
     * @param printmode
     *            Enum bnw.abm.intg.init.framework.models.Population.MatrixCell.ValueMode;
     */
    public void setValueMode(ValueMode printmode) {
        MatrixCell.printmode = printmode;
    }

    public static class MatrixCell {
        private static ValueMode printmode = ValueMode.AGENTCOUNT;
        private final double targetProportion;
        private double currentProportion = 0;
        private double agentCount = 0;

        MatrixCell(double proportion) {
            this.targetProportion = proportion;
        }

        public double getTargetProportion() {
            return this.targetProportion;
        }

        public double getAgentCount() {
            return this.agentCount;
        }

        public double getCurrentProportion() {
            return this.currentProportion;
        }

        public String toString() {
            switch (printmode) {
            case AGENTCOUNT:
                return String.valueOf(this.agentCount);
            case TARGETPROPORTIONS:
                return String.valueOf(this.targetProportion);
            case CURRENTPROPORTIONS:
                return String.valueOf(this.currentProportion);
            default:
                return null;
            }
        }

        public enum ValueMode {
            AGENTCOUNT,
            CURRENTPROPORTIONS,
            TARGETPROPORTIONS;
        }
    }

}
