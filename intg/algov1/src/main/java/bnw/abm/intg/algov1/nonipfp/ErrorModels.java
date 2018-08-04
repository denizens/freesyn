package bnw.abm.intg.algov1.nonipfp;

import bnw.abm.intg.algov2.framework.models.Agent;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.Group;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.Population;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author wniroshan 17 May 2018
 */
class ErrorModels {

    private final LinkedHashMap<ReferenceAgentType, Integer> expectedAgentTypeCounts;
    private final LinkedHashMap<GroupType, Integer> expectedGroupTypeCounts;

    ErrorModels(final LinkedHashMap<ReferenceAgentType, Integer> expectedAgentTypeCounts,
                       final LinkedHashMap<GroupType, Integer> expectedGroupTypeCounts) {
        this.expectedAgentTypeCounts = expectedAgentTypeCounts;
        this.expectedGroupTypeCounts = expectedGroupTypeCounts;
    }

    double calcProportionsErrorWithChange(Population population, Group proposedChange, double agentsProminence) {
        return calcGroupMeanSquaredProportionsError(population, proposedChange)
                + (agentsProminence * calcAgentsMeanSquaredProportionsError(population, proposedChange));
    }

    double calcProportionsError(Population population, double agentsProminence) {
        return calcGroupMeanSquaredProportionsError(population,null) + (agentsProminence * calcAgentsMeanSquaredProportionsError(population, null));
    }

    private double calcGroupMeanSquaredProportionsError(Population population, Group newGroup) {
        double expectedTotalGroups = expectedGroupTypeCounts.values().stream().mapToInt(v -> v).sum();
        double observedTotalGroups = population.getGroupsSummary().values().stream().mapToInt(v -> v).sum();

        double sumSquaredError = 0;
        for (GroupType groupType : expectedGroupTypeCounts.keySet()) {
            double currentProportion = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentProportion = (population.getGroupsSummary().get(groupType) + 1) / observedTotalGroups;
            } else {
                currentProportion = population.getGroupsSummary().get(groupType) / observedTotalGroups;
            }

            double expectedProportion = expectedGroupTypeCounts.get(groupType) / expectedTotalGroups;
            sumSquaredError += Math.pow((expectedProportion - currentProportion), 2);
        }
        return Math.sqrt(sumSquaredError / (double) expectedGroupTypeCounts.keySet().size());
    }


    private double calcAgentsMeanSquaredProportionsError(Population population, Group newGroup) {
        // Below block summarises agent counts in new group. So we can easily use them when calculating MSE.
        Map<ReferenceAgentType, Integer> groupAgentsSummary = null;
        if (newGroup != null) {
            groupAgentsSummary = new HashMap<>();
            for (Agent a : newGroup.getMembers()) {
                ReferenceAgentType refAgentType = ReferenceAgentType.getExisting(a.getType());
                if (groupAgentsSummary.containsKey(refAgentType)) {
                    groupAgentsSummary.put(refAgentType, groupAgentsSummary.get(refAgentType) + 1);
                } else {
                    groupAgentsSummary.put(refAgentType, 1);
                }
            }
        }

        double expectedTotalAgents = expectedAgentTypeCounts.values().stream().mapToInt(v -> v).sum();
        double observedTotalAgents = population.getAgentsSummary().values().stream().mapToInt(v -> v).sum();

        double sumSquaredError = 0;
        for (ReferenceAgentType refAgentType : expectedAgentTypeCounts.keySet()) {
            double currentAgentProportion = 0;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentProportion = (population.getAgentsSummary()
                                                    .get(refAgentType) + groupAgentsSummary.get(refAgentType)) / observedTotalAgents;
            } else {
                currentAgentProportion = population.getAgentsSummary().get(refAgentType) / observedTotalAgents;
            }
            double targetAgentProportion = expectedAgentTypeCounts.get(refAgentType) / expectedTotalAgents;
            sumSquaredError += Math.pow((targetAgentProportion - currentAgentProportion), 2);
        }
        return Math.sqrt(sumSquaredError / (double) expectedAgentTypeCounts.keySet().size());
    }

    /**
     * Calculates the error in the population after proposed change. Does not add the proposed changed to the population.
     *
     * @param population       current population
     * @param proposedChange   group to be added
     * @param agentsProminence How significant agent category's error with respect to groups' mean squared error.
     * @return Mean squared error if the proposed change is made
     */
    private double calcErrorWithChange(Population population, Group proposedChange, double agentsProminence) {
        return calcGroupMeanSquaredError(population, proposedChange)
                + (agentsProminence * calcAgentsMeanSquaredError(population, proposedChange));
    }

    /**
     * Calculates total error in the population, to be used in objective function in simulated annealing
     *
     * @param population       Current population
     * @param agentsProminance How significant agent category's error with respect to groups' mean squared error.
     * @return current mean squared error of the population
     */
    private double calcError(Population population, double agentsProminance) {
        return calcGroupMeanSquaredError(population, null) + (agentsProminance * calcAgentsMeanSquaredError(population, null));
    }

    private double calcGroupMeanSquaredError(Population population, Group newGroup) {

        double sumSquaredError = 0;
        for (GroupType groupType : expectedGroupTypeCounts.keySet()) {
            double currentGroupCount = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentGroupCount = (population.getGroupsSummary().get(groupType) + 1);
            } else {
                currentGroupCount = population.getGroupsSummary().get(groupType);
            }
            double targetGroupCount = expectedGroupTypeCounts.get(groupType);
            sumSquaredError += Math.pow((targetGroupCount - currentGroupCount), 2);
        }
        return Math.sqrt(sumSquaredError / (double) expectedGroupTypeCounts.keySet().size());
    }

    private double calcAgentsMeanSquaredError(Population population, Group newGroup) {

        // Below block summarises agent counts in new group. So we can easily use them when calculating MSE.
        Map<ReferenceAgentType, Integer> groupAgentsSummary = null;
        if (newGroup != null) {
            groupAgentsSummary = new HashMap<>();
            for (Agent a : newGroup.getMembers()) {
                ReferenceAgentType refAgentType = ReferenceAgentType.getExisting(a.getType());
                if (groupAgentsSummary.containsKey(refAgentType)) {
                    groupAgentsSummary.put(refAgentType, groupAgentsSummary.get(refAgentType) + 1);
                } else {
                    groupAgentsSummary.put(refAgentType, 1);
                }
            }
        }

        double sumSquaredError = 0;
        for (ReferenceAgentType refAgentType : expectedAgentTypeCounts.keySet()) {
            double currentAgentCount = 20;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentCount = (population.getAgentsSummary().get(refAgentType) + groupAgentsSummary.get(refAgentType));
            } else {
                currentAgentCount = population.getAgentsSummary().get(refAgentType);
            }
            double targetAgentCount = expectedAgentTypeCounts.get(refAgentType);
            sumSquaredError += Math.pow((targetAgentCount - currentAgentCount), 2);
        }
        return Math.sqrt(sumSquaredError / (double) expectedAgentTypeCounts.keySet().size());
    }

    /**
     * Calculates total error in the population, to be used in objective function in simulated annealing
     *
     * @param population       Current population
     * @param agentsProminance How significant agent category's error with respect to groups' mean squared error.
     * @return current mean squared error of the population
     */
    private double calcAAPD(Population population, double agentsProminance) {
        return calcGroupAAPD(population, null) + (agentsProminance * calcAgentsAAPD(population, null));
    }

    private double calcGroupAAPD(Population population, Group newGroup) {
        double diffPercent = 0;
        for (GroupType groupType : expectedGroupTypeCounts.keySet()) {

            double currentGroupCount = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentGroupCount = population.getGroupsSummary().get(groupType) + 1;
            } else {
                currentGroupCount = population.getGroupsSummary().get(groupType);
            }
            if (expectedGroupTypeCounts.get(groupType) != 0) {
                diffPercent += Math.abs(expectedGroupTypeCounts.get(groupType) - currentGroupCount) / expectedGroupTypeCounts.get
                        (groupType) * 100;
            } else {
                //If the group type is 0 and there are groups in it, each group is given a high penalty. So the percentage diff is x100.
                diffPercent += Math.abs(expectedGroupTypeCounts.get(groupType) - population.getGroupsSummary()
                                                                                           .get(groupType)) * 100;
            }
        }
        return diffPercent / (double) expectedGroupTypeCounts.keySet().size();
    }

    private double calcAgentsAAPD(Population population, Group newGroup) {


        // Below block summarises agent counts in new group. So we can easily use them when calculating MSE.
        Map<ReferenceAgentType, Integer> groupAgentsSummary = null;
        if (newGroup != null) {
            groupAgentsSummary = new HashMap<>();
            for (Agent a : newGroup.getMembers()) {
                ReferenceAgentType refAgentType = ReferenceAgentType.getExisting(a.getType());
                if (groupAgentsSummary.containsKey(refAgentType)) {
                    groupAgentsSummary.put(refAgentType, groupAgentsSummary.get(refAgentType) + 1);
                } else {
                    groupAgentsSummary.put(refAgentType, 1);
                }
            }
        }

        double diffPercent = 0;
        for (ReferenceAgentType refAgentType : expectedAgentTypeCounts.keySet()) {
            double currentAgentCount = 0;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentCount = (population.getAgentsSummary().get(refAgentType) + groupAgentsSummary.get(refAgentType));
            } else {
                currentAgentCount = population.getAgentsSummary().get(refAgentType);
            }

            double targetAgentCount = expectedAgentTypeCounts.get(refAgentType);
            if (expectedAgentTypeCounts.get(refAgentType) != 0) {
                diffPercent += Math.abs(targetAgentCount - currentAgentCount) / targetAgentCount * 100;
            } else {
                //If the agent type is 0 and there are agents in it, each agent is given a high penalty. So the percentage diff is x100.
                diffPercent += Math.abs(expectedAgentTypeCounts.get(refAgentType) - population.getAgentsSummary()
                                                                                              .get(refAgentType)) * 100;
            }
        }
        return diffPercent / (double) expectedAgentTypeCounts.keySet().size();
    }


}
