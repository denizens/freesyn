package bnw.abm.intg.algov1.ipfp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

import bnw.abm.intg.algov1.Algov1Criteria.DefaultGroupSizeAccept;
import bnw.abm.intg.algov1.Algov1Criteria.DefaultGroupSizeReject;
import bnw.abm.intg.algov2.framework.models.Agent;
import bnw.abm.intg.algov2.framework.models.AgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.Group;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRulesWrapper;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.MembersPool;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion.Accept;
import bnw.abm.intg.algov2.templates.DefaultAcceptanceCriterion;
import bnw.abm.intg.algov2.templates.DefaultRejectionCriteria;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.RejectionCriterion;
import bnw.abm.intg.algov2.templates.RejectionCriterion.Reject;
import bnw.abm.intg.util.Log;

public class GroupsBuilder {

    private static IPFPTable currentCounts, targetCounts, targetProportions;
    private static int targetPopSize = 0;
    private final GroupTypeLogic groupTypeLogic;
    private final AcceptanceCriterion groupSizeAcceptCriterion, globalDefaultAcceptance;
    private final RejectionCriterion groupSizeRejectionCriterion;

    private int maxIterationsWithoutResults = 100000;
    final private int maxAttempts;

    public GroupsBuilder(LinkRules linkRules, IPFPTable targetCounts, IPFPTable targetProportions, GroupTypeLogic groupTypeLogic, int maxGroupAttempts) {
        this.maxAttempts = maxGroupAttempts;
        LinkRulesWrapper.setLinkRules(linkRules);
        this.groupTypeLogic = groupTypeLogic;

        GroupsBuilder.targetCounts = targetCounts;
        GroupsBuilder.targetProportions = targetProportions;
        currentCounts = new IPFPTable(targetCounts.rowKeySet(), targetCounts.columnKeySet());

        targetPopSize = (int) targetCounts.values().stream().mapToDouble(v -> v).sum();

        groupSizeAcceptCriterion = new DefaultGroupSizeAccept();
        groupSizeAcceptCriterion.register();
        globalDefaultAcceptance = new DefaultAcceptanceCriterion();
        globalDefaultAcceptance.register();

        groupSizeRejectionCriterion = new DefaultGroupSizeReject();
        groupSizeRejectionCriterion.register();
        new DefaultRejectionCriteria().register();

    }

    public Population buildGroupWiseMonteCarlo(Random random) {
        Population population = new Population(targetProportions);
        int currentGroups = 0;

        // Find expected total number of groups in each group type
        Map<GroupType, Integer> expectedGroupsCounts = new LinkedHashMap<>();
        int totalExpectedGroups = 0;
        for (GroupType gType : targetCounts.rowKeySet()) {
            int maxMembers = (int) GroupRulesWrapper.get(gType, GroupRuleKeys.MAX_MEMBERS);
            double expectedCount = targetCounts.row(gType).values().stream().mapToDouble(d -> d).sum();
            expectedCount = Math.round(expectedCount / (double) maxMembers);
            expectedGroupsCounts.put(gType, (int) expectedCount);
            totalExpectedGroups += (int) expectedCount;
        }

        for (GroupType gType : expectedGroupsCounts.keySet()) {
            int countInThisGType = 0;
            int iterationsBeforeTermination = maxIterationsWithoutResults;
            while (countInThisGType < expectedGroupsCounts.get(gType)) {
                // Now we have to randomly select a group head
                List<ReferenceAgentType> allGroupHeads = (List<ReferenceAgentType>) GroupRulesWrapper.get(gType, GroupRuleKeys.REFERENCE_AGENT);
                double groupHeadAgentCumulativeProbability = targetProportions.row(gType).entrySet().stream()
                        .filter(entry -> allGroupHeads.contains(entry.getKey())).mapToDouble(m -> m.getValue()).sum();
                double randVal = random.nextDouble();
                double proportionalOffset = randVal * groupHeadAgentCumulativeProbability;
                ReferenceAgentType groupHead = null;
                double agentsTempOffsetSum = 0;
                for (ReferenceAgentType refAgentType : allGroupHeads) {

                    if (targetProportions.get(gType, refAgentType) == 0) {
                        continue; // If probability is 0, do not consider
                    }
                    agentsTempOffsetSum += targetProportions.get(gType, refAgentType);
                    if (proportionalOffset <= agentsTempOffsetSum) {
                        groupHead = refAgentType;
                        break;
                    }
                }
                GroupTemplate newTemplate = newGroupTemplate(gType, groupHead);
                boolean isSuccessful = constructGroupMonteCarloSelection(newTemplate, random);
                if (isSuccessful) {
                    population.addGroup(new Group(newTemplate));
                    currentGroups++;
                    countInThisGType++;
                    iterationsBeforeTermination = maxIterationsWithoutResults;
                } else {
                    iterationsBeforeTermination--;
                }
                if (iterationsBeforeTermination == 0) {
                    break;
                }
            }

            Log.info("Group Type: " + gType + " total groups: " + population.getGroups().size() + " total agents: " + population.size());
        }

        return population;
    }

    public Population hillClimbingCounts(Population population, int maxIterations, Random random) {
        double agentErrorProminance = 1;

        double oldError = calcError(population, agentErrorProminance);

        int rounds = 0;
        while (rounds < maxIterations) {// FIXME:Change this to a target error and a number of rounds
            rounds++;
            int changesErr = 0;
            for (int i = 0; i < 800; i++) {
                Group removeGroup = randomSelectGroup(population, random);
                population.removeGroup(removeGroup);
                Group proposedChange = constructNewGroup(removeGroup.type(), random);

                double newError = calcErrorWithChange(population, proposedChange, agentErrorProminance);
                if (newError < oldError) {
                    population.addGroup(proposedChange);
                    oldError = newError;
                    changesErr++;
                } else {
                    population.addGroup(removeGroup);
                }
            }
            // population.addToErrorList(oldError); // recording error reduction
            Log.info("Round " + rounds + " Changes made on error: " + changesErr);
        }
        return population;
    }

    public Population simulatedAnnealingCounts(Population population, Random random, double temp, double coolingRate, double exitTemperature) {

        double bestEnergy = Double.POSITIVE_INFINITY;
        Population bestPop = null;
        int changesCount = 0;
        int improvingChanges = 0;

        double currentEnergy = calcError(population, 1);
        while (temp > exitTemperature) {

            // Prepare new solution
            Group removeGroup = randomSelectGroup(population, random);
            population.removeGroup(removeGroup);
            Group proposedChange = constructNewGroup(removeGroup.type(), random);

            double neighbourEnergy = calcErrorWithChange(population, proposedChange, 1);

            // Decide if we should accept the neighbour
            if (acceptanceProbability(currentEnergy, neighbourEnergy, temp) > random.nextDouble()) {
                population.addGroup(proposedChange);
                currentEnergy = neighbourEnergy;
                ++changesCount;
            } else {
                population.addGroup(removeGroup);
            }

            // Keep track of the best solution found
            if (currentEnergy < bestEnergy) {
                bestPop = population.copy();
                bestEnergy = currentEnergy;
                ++changesCount;
                ++improvingChanges;
            }

            // Cool system
            temp *= 1 - coolingRate;
            System.out.print("\rChanges: " + changesCount + " Improvements: " + improvingChanges + " Temp: " + temp + " Population size: "
                    + population.size() + " Error: " + bestEnergy + "       ");
        }

        Log.info("Final population error: " + bestEnergy);
        return bestPop;
    }

    /**
     * Calculate probability to accept a suggestion in simulated annealing based on metropolis criteria
     * 
     * @param energy
     *            Current error
     * @param newEnergy
     *            Error after the change
     * @param temperature
     *            Current temperature for annealing
     * @return selection probability
     */
    double acceptanceProbability(double energy, double newEnergy, double temperature) {
        // If the new solution is better, accept it
        if (newEnergy < energy) {
            return 1.0;
        }
        // If the new solution is worse, calculate an acceptance probability - metropolis criteria
        return Math.exp((energy - newEnergy) / temperature);
    }

    public Population hillClimbingProportions(Population population, int maxIterations, Random random) {
        double agentErrorProminance = 1;
        double initialAcceptanceProbability = 0.6;
        double temperatureMin = 0.0001;
        double alpha = 0.8;

        double oldError = calcErrorProportions(population, agentErrorProminance);
        List<Double> errorVec = new ArrayList<>();
        errorVec.add(oldError);
        /*
         * Good starting error depends on the application. I am using a proposal found in the literature. It calculates a temperature they may
         * lead to accepting about 80% of error increasing proposals initially.
         */

        int rounds = 0;
        while (rounds < maxIterations) {
            rounds++;
            int changesErr = 0;
            for (int i = 0; i < 800; i++) {
                Group removeGroup = randomSelectGroup(population, random);
                population.removeGroup(removeGroup);
                Group proposedChange = constructNewRandomGroup(random);

                double newError = calcErrorWithChangeProportions(population, proposedChange, agentErrorProminance);
                if (newError < oldError) {
                    population.addGroup(proposedChange);
                    oldError = newError;
                    changesErr++;
                } else {
                    population.addGroup(removeGroup); // Again adding the previously removed group
                }
            }
            Log.info("Round: " + rounds + " Changes made on error: " + changesErr);
        }
        Log.info(errorVec.toString());
        return population;
    }

    /**
     * Randomly select a group from current population
     * 
     * @param population
     *            current Population
     * @param random
     *            Random number generator
     * @return selected group
     */
    private Group randomSelectGroup(Population population, Random random) {
        int groupsCount = population.getGroups().size();
        int randomIndex = random.nextInt(groupsCount);
        return population.getGroups().get(randomIndex);
    }

    private Group constructNewGroup(GroupType newGroupType, Random random) {

        GroupTemplate newGroupTemplate = null;
        while (true) {
            // Select a random group type
            // int groupTypesCount = targetProportions.rowKeySet().size();
            // GroupType newGroupType = (GroupType) targetProportions.rowKeySet().toArray()[random.nextInt(groupTypesCount)];

            // Select a random group head out of potential heads
            List<ReferenceAgentType> potentialGroupHeads = (List<ReferenceAgentType>) GroupRulesWrapper.get(newGroupType,
                    GroupRuleKeys.REFERENCE_AGENT);
            if (potentialGroupHeads != null) {

                try {
                    potentialGroupHeads = potentialGroupHeads.stream().filter(r -> targetProportions.get(newGroupType, r) > 0)
                                                             .collect(Collectors.toList());
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                if (potentialGroupHeads.isEmpty())
                    continue;

                ReferenceAgentType groupHead = potentialGroupHeads.get(random.nextInt(potentialGroupHeads.size()));
                GroupTemplate baseTemplate = newGroupTemplate(newGroupType, groupHead); // Construct base template

                // Construct links and populate base template
                if (formCompulsoryLinksRandomSelection(baseTemplate, 0, random)) {
                    if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                        newGroupTemplate = baseTemplate;
                        break; // We have successfully constructed a legal group template
                    } else if (formNonCompulsoryLinksRandomSelection(baseTemplate, 0, random)) {// Template not complete lets try to complete it
                                                                                                // by forming non compulsory links
                        // Group template is legal, but is this the right type
                        if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                            newGroupTemplate = baseTemplate;
                            break; // We have successfully constructed a legal group template
                        }
                    }
                }
                // Could not form a legal group template, let's try a different one
                discardGroup(baseTemplate);
                newGroupTemplate = null;
            }
        }

        Log.debug("New random group : " + newGroupTemplate);
        return new Group(newGroupTemplate);
    }

    /**
     * Constructs a new group pure randomly
     * 
     * @param random
     *            Random number generator
     * @return Constructed group
     */
    private Group constructNewRandomGroup(Random random) {

        GroupTemplate newGroupTemplate = null;
        while (true) {
            // Select a random group type
            int groupTypesCount = targetProportions.rowKeySet().size();
            GroupType newGroupType = (GroupType) targetProportions.rowKeySet().toArray()[random.nextInt(groupTypesCount)];

            // Select a random group head out of potential heads
            List<ReferenceAgentType> potentialGroupHeads = (List<ReferenceAgentType>) GroupRulesWrapper.get(newGroupType,
                    GroupRuleKeys.REFERENCE_AGENT);
            if (potentialGroupHeads != null) {

                potentialGroupHeads = potentialGroupHeads.stream().filter(r -> targetProportions.get(newGroupType, r) > 0)
                        .collect(Collectors.toList());

                if (potentialGroupHeads.isEmpty())
                    continue;

                ReferenceAgentType groupHead = potentialGroupHeads.get(random.nextInt(potentialGroupHeads.size()));
                GroupTemplate baseTemplate = newGroupTemplate(newGroupType, groupHead); // Construct base template

                // Construct links and populate base template
                if (formCompulsoryLinksRandomSelection(baseTemplate, 0, random)) {
                    if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                        newGroupTemplate = baseTemplate;
                        break; // We have successfully constructed a legal group template
                    } else if (formNonCompulsoryLinksRandomSelection(baseTemplate, 0, random)) {// Template not complete lets try to complete it
                                                                                                // by
                                                                                                // forming non compulsory links
                        // Group template is legal, but is this the right type
                        if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                            newGroupTemplate = baseTemplate;
                            break; // We have successfully constructed a legal group template
                        }
                    }
                }
                // Could not form a legal group template, let's try a different one
                discardGroup(baseTemplate);
                newGroupTemplate = null;
            }
        }

        Log.debug("New random group : " + newGroupTemplate);
        return new Group(newGroupTemplate);
    }

    /**
     * Calculates initial temperature. We take several random updates and take the ones that increases the error. Then based on their averaged
     * error (delta) we take a temperature (t) that would accept at least 80% (p = 0.8) of suggestions t = delta\ln(p)
     *
     * @param population
     *            the population
     * @param random
     *            Random number generator
     * @param numberOfTries
     *            How may error values should we take
     * @param initialAcceptanceProbability
     *            What is the probability that an error increasing suggestion should be accepted initially. Usually 0.8, corresponding to 80%
     *            acceptance probability
     * @param agentErrorProminance
     *            How significant agent category's error with respect to groups' mean squared error.
     * @return initial temperature
     */
    private double calcInitialTemperatureProportions(Population population, Random random, int numberOfTries,
            double initialAcceptanceProbability, double agentErrorProminance) {

        double currentError = calcErrorProportions(population, 1);
        double errorSum = 0;

        int increasingErrorCount = 0;
        while (increasingErrorCount < numberOfTries) {
            Group removeGroup = randomSelectGroup(population, random);
            population.removeGroup(removeGroup);
            Group proposedChange = constructNewRandomGroup(random);
            double newError = calcErrorWithChangeProportions(population, proposedChange, agentErrorProminance);
            if (newError > currentError) {
                errorSum += newError;
            }
            population.addGroup(removeGroup);
            increasingErrorCount++;
        }

        double avgError = errorSum / numberOfTries;
        return (-1) * avgError / Math.log(initialAcceptanceProbability);
    }

    /**
     * Calculates the error in the population after proposed change. Does not add the proposed changed to the population.
     * 
     * @param population
     *            current population
     * @param proposedChange
     *            group to be added
     * @param agentsProminance
     *            How significant agent category's error with respect to groups' mean squared error.
     * @return Mean squared error if the proposed change is made
     */
    double calcErrorWithChange(Population population, Group proposedChange, double agentsProminance) {
        return calcGroupMeanSquaredError(population, proposedChange)
                + (agentsProminance * calcAgentsMeanSquaredError(population, proposedChange));
    }

    double calcErrorWithChangeProportions(Population population, Group proposedChange, double agentsProminance) {
        return calcGroupMeanSquaredErrorProportions(population, proposedChange)
                * (agentsProminance * calcAgentsMeanSquaredErrorProportions(population, proposedChange));
    }

    /**
     * Calculates total error in the population, to be used in objective function in simulated annealing
     * 
     * @param population
     *            Current population
     * @param agentsProminance
     *            How significant agent category's error with respect to groups' mean squared error.
     * @return current mean squared error of the population
     */
    private double calcError(Population population, double agentsProminance) {
        return calcGroupMeanSquaredError(population, null) + (agentsProminance * calcAgentsMeanSquaredError(population, null));
    }

    private double calcErrorProportions(Population population, double agentsProminance) {
        return calcGroupMeanSquaredErrorProportions(population, null)
                * (agentsProminance * calcAgentsMeanSquaredErrorProportions(population, null));
    }

    private double calcGroupMeanSquaredError(Population population, Group newGroup) {
        double currentPopGroupsSum = population.getGroupsSummary().values().stream().mapToDouble(v -> v).sum();
        if (newGroup != null) {
            currentPopGroupsSum += 1;
        }
        double sumSquaredError = 0;
        for (GroupType groupType : targetCounts.rowKeySet()) {
            double currentGroupCount = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentGroupCount = (population.getGroupsSummary().get(groupType) + 1);
            } else {
                currentGroupCount = population.getGroupsSummary().get(groupType);
            }
            double targetGroupCount = targetCounts.row(groupType).values().stream().mapToDouble(v -> v).sum();
            sumSquaredError += Math.pow((targetGroupCount - currentGroupCount), 2);
        }
        return Math.sqrt(sumSquaredError / (double) targetCounts.rowKeySet().size());
    }

    private double calcGroupMeanSquaredErrorProportions(Population population, Group newGroup) {
        double currentPopGroupsSum = population.getGroupsSummary().values().stream().mapToDouble(v -> v).sum();
        if (newGroup != null) {
            currentPopGroupsSum += 1;
        }
        double sumSquaredError = 0;
        for (GroupType groupType : targetProportions.rowKeySet()) {
            double currentGroupProportion = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentGroupProportion = (population.getGroupsSummary().get(groupType) + 1) / currentPopGroupsSum;
            } else {
                currentGroupProportion = population.getGroupsSummary().get(groupType) / currentPopGroupsSum;
            }
            double targetGroupProportion = targetProportions.row(groupType).values().stream().mapToDouble(v -> v).sum();
            sumSquaredError += Math.pow((targetGroupProportion - currentGroupProportion), 2);
        }
        return Math.sqrt(sumSquaredError / (double) targetProportions.rowKeySet().size());
    }

    private double calcAgentsMeanSquaredError(Population population, Group newGroup) {
        double currentPopAgentsSum = population.getAgentsSummary().values().stream().mapToDouble(v -> v).sum();

        // Below block summarises agent counts in new group. So we can easily use them when calculating MSE.
        Map<ReferenceAgentType, Integer> groupAgentsSummary = null;
        if (newGroup != null) {
            groupAgentsSummary = new HashMap<>();
            currentPopAgentsSum += newGroup.size();// Total number of agents in population need to be updated
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
        for (ReferenceAgentType refAgentType : targetCounts.columnKeySet()) {
            double currentAgentCount = 0;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentCount = (population.getAgentsSummary().get(refAgentType) + groupAgentsSummary.get(refAgentType));
            } else {
                currentAgentCount = population.getAgentsSummary().get(refAgentType);
            }
            double targetAgentCount = targetCounts.column(refAgentType).values().stream().mapToDouble(v -> v).sum();
            sumSquaredError += Math.pow((targetAgentCount - currentAgentCount), 2);
        }
        return Math.sqrt(sumSquaredError / (double) targetCounts.columnKeySet().size());
    }

    private double calcAgentsMeanSquaredErrorProportions(Population population, Group newGroup) {
        double currentPopAgentsSum = population.getAgentsSummary().values().stream().mapToDouble(v -> v).sum();

        // Below block summarises agent counts in new group. So we can easily use them when calculating MSE.
        Map<ReferenceAgentType, Integer> groupAgentsSummary = null;
        if (newGroup != null) {
            groupAgentsSummary = new HashMap<>();
            currentPopAgentsSum += newGroup.size();// Total number of agents in population need to be updated
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
        for (ReferenceAgentType refAgentType : targetProportions.columnKeySet()) {
            double currentAgentProportion = 0;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentProportion = (population.getAgentsSummary().get(refAgentType) + groupAgentsSummary.get(refAgentType))
                        / currentPopAgentsSum;
            } else {
                currentAgentProportion = population.getAgentsSummary().get(refAgentType) / currentPopAgentsSum;
            }
            double targetAgentProportion = targetProportions.column(refAgentType).values().stream().mapToDouble(v -> v).sum();
            sumSquaredError += Math.pow((targetAgentProportion - currentAgentProportion), 2);
        }
        return Math.sqrt(sumSquaredError / (double) targetProportions.columnKeySet().size());
    }

    public Population buildProbabilistically(Random random) {
        Population population = new Population(targetProportions);
        int currentGroups = 0;

        // Find expected total number of groups in each group type
        Map<GroupType, Integer> expectedGroupsCounts = new LinkedHashMap<>();
        int totalExpectedGroups = 0;
        for (GroupType gType : targetCounts.rowKeySet()) {
            double maxMembers = (double) GroupRulesWrapper.get(gType, GroupRuleKeys.MAX_MEMBERS);
            double expectedCount = targetCounts.row(gType).values().stream().mapToDouble(d -> d).sum();
            expectedCount = Math.round(expectedCount / maxMembers);
            expectedGroupsCounts.put(gType, (int) expectedCount);
            totalExpectedGroups += (int) expectedCount;
        }

        while (currentGroups < totalExpectedGroups) {

            // Randomly select a group type
            GroupType randomGroupType = null;
            double groupRandomOffSet = random.nextInt(totalExpectedGroups);
            int groupTypeIndex = 0;
            int groupTempOffsetSum = 0;
            for (GroupType gType : expectedGroupsCounts.keySet()) {
                groupTempOffsetSum += expectedGroupsCounts.get(gType);
                if (groupRandomOffSet < groupTempOffsetSum) {
                    randomGroupType = gType;
                    break;
                }
            }

            // If this group type is empty do not form any groups
            if (targetCounts.row(randomGroupType).values().stream().mapToDouble(t -> t).sum() == 0) {
                continue;
            }

            // Now we have to randomly select a group head
            List<ReferenceAgentType> allGroupHeads = (List<ReferenceAgentType>) GroupRulesWrapper.get(randomGroupType,
                    GroupRuleKeys.REFERENCE_AGENT);
            double groupHeadAgentCumulativeProbability = targetProportions.row(randomGroupType).entrySet().stream()
                    .filter(entry -> allGroupHeads.contains(entry.getKey())).mapToDouble(m -> m.getValue()).sum();
            double randVal = random.nextDouble();
            double proportionalOffset = randVal * groupHeadAgentCumulativeProbability;
            ReferenceAgentType groupHead = null;
            double agentsTempOffsetSum = 0;
            for (ReferenceAgentType refAgentType : allGroupHeads) {
                agentsTempOffsetSum += targetProportions.get(randomGroupType, refAgentType);
                if (proportionalOffset <= agentsTempOffsetSum) {
                    groupHead = refAgentType;
                    break;
                }
            }

            GroupTemplate newTemplate = newGroupTemplate(randomGroupType, groupHead);
            boolean isSuccesfull = constructGroupRandomSelection(newTemplate, random);
            if (isSuccesfull) {
                population.addGroup(new Group(newTemplate));
                currentGroups++;
            } else {
                discardGroup(newTemplate);
            }

            Log.info("Group Type: " + randomGroupType + " total groups: " + population.getGroups().size() + " total agents: "
                    + population.size());
        }
        return population;
    }

    public Population build() {
        Population population = new Population(targetProportions);

        for (GroupType gType : targetCounts.rowKeySet()) {
            int currentGroups = -1;
            if (targetCounts.row(gType).values().stream().mapToDouble(t -> t).sum() == 0) {
                continue;
            }
            Log.info("Group Type: " + gType);
            int iteration = 0;
            while (currentGroups < population.getGroups().size()) {
                Log.info("Iteration " + iteration);
                currentGroups = population.getGroups().size();
                List<ReferenceAgentType> agentTypeSelections = new ArrayList<>(targetCounts.columnKeySet());
                removeDepleted(gType, agentTypeSelections);
                sortByRMSE(gType, agentTypeSelections);
                for (int i = 0; i < agentTypeSelections.size();) {
                    ReferenceAgentType groupHead = agentTypeSelections.get(i);
                    GroupTemplate newTemplate = newGroupTemplate(gType, groupHead);
                    // System.out.println(newTemplate);
                    constructGroup(newTemplate);
                    if (newTemplate.size() != 0) {
                        population.addGroup(new Group(newTemplate));
                        removeDepleted(gType, agentTypeSelections);
                        sortByRMSE(gType, agentTypeSelections);
                        i = 0;
                    } else {
                        i++;
                    }
                }
                Log.info("complete");
                iteration++;
            }

            Log.info("Group Type: " + gType + " total groups: " + population.getGroups().size() + " total agents: " + population.size());
        }
        return population;
    }

    private void removeDepleted(GroupType gType, List<? extends AgentType> agentsList) {
        for (int i = 0; i < agentsList.size(); i++) {
            AgentType agent = agentsList.get(i);
            if (targetCounts.get(gType, agent) <= currentCounts.get(gType, agent)) {
                agentsList.remove(agent);
                i--;
            }
        }
    }

    private void removeDepletedPairs(GroupType gType, List<Pair<LinkType, AgentType>> pairsList) {
        for (int i = 0; i < pairsList.size(); i++) {
            AgentType agent = pairsList.get(i).getRight();
            if (targetCounts.get(gType, agent) <= currentCounts.get(gType, agent)) {
                pairsList.remove(agent);
                i--;
            }
        }
    }

    private static GroupTemplate newGroupTemplate(GroupType groupType, ReferenceAgentType agentType) {
        GroupTemplate newTemplate = new GroupTemplate(ReferenceAgentType.getExisting(agentType.getTypeID()));
        newTemplate.setGroupType(groupType);
        double currentAgentCount = currentCounts.get(groupType, agentType);
        currentCounts.put(groupType, agentType, currentAgentCount + 1);
        return newTemplate;
    }

    private static Member addMember(GroupTemplate groupTemplate, Member reference, LinkType linkType, AgentType target) {
        Member newTarget = MembersPool.getNextMemberForGroup(groupTemplate, target);
        groupTemplate.put(reference, linkType, newTarget);
        double currentAgentCount = currentCounts.get(groupTemplate.getGroupType(), target);
        currentCounts.put(groupTemplate.getGroupType(), target, currentAgentCount + 1);
        return newTarget;
    }

    private static void discardGroup(GroupTemplate groupTemplate) {
        for (Member member : groupTemplate.getAllMembers()) {
            double existing = currentCounts.get(groupTemplate.getGroupType(), member);
            currentCounts.put(groupTemplate.getGroupType(), member, existing - 1);
        }
        groupTemplate.delete();
    }

    private void removeMember(GroupTemplate groupTemplate, Member memberToRemove) {
        groupTemplate.remove(memberToRemove);
        double existing = currentCounts.get(groupTemplate.getGroupType(), memberToRemove);
        currentCounts.put(groupTemplate.getGroupType(), memberToRemove, existing - 1);
    }

    private <A extends AgentType> void sortByRMSE(GroupType gType, List<A> selection) {
        Collections.sort(selection, new Comparator<A>() {
            public int compare(A o1, A o2) {
                Double s_1 = calculateRMSE(gType, o1);
                Double s_2 = calculateRMSE(gType, o2);
                if (s_1 == Double.NaN || s_2 == Double.NaN) {
                    Log.warn("Detected Double.NaN - caution!!");
                }
                return s_1.compareTo(s_2);
            }
        });
    }

    private <A extends AgentType> void sortPairByRMSE(GroupType gType, List<Pair<LinkType, A>> selection) {
        Collections.sort(selection, new Comparator<Pair<LinkType, A>>() {
            public int compare(Pair<LinkType, A> o1, Pair<LinkType, A> o2) {
                Double s_1 = calculateRMSE(gType, o1.getRight());
                Double s_2 = calculateRMSE(gType, o2.getRight());
                if (s_1 == Double.NaN || s_2 == Double.NaN) {
                    Log.warn("Detected Double.NaN - caution!!");
                }
                return s_1.compareTo(s_2);
            }
        });
    }

    private double calculateRMSE(GroupType gType, AgentType selectedAgentType) {
        double squaredSum = 0;
        if (targetCounts.get(gType, selectedAgentType) != 0.0) {
            for (AgentType aType : targetProportions.columnKeySet()) {
                if (aType != selectedAgentType) {
                    squaredSum += Math.pow(targetProportions.get(gType, aType) - ((currentCounts.get(gType, aType)) / targetPopSize), 2);
                } else {
                    squaredSum += Math.pow(targetProportions.get(gType, aType) - ((currentCounts.get(gType, aType) + 1) / targetPopSize), 2);
                }
            }
        } else {
            squaredSum = Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(squaredSum / (double) targetCounts.size());
    }

    private void constructGroup(GroupTemplate baseTemplate) {
        if (formCompulsoryLinks(baseTemplate, 0)) {
            if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                return;
            } else {
                formNonCompulsoryLinks(baseTemplate, 0);
            }
        }
    }

    private boolean constructGroupMonteCarloSelection(GroupTemplate baseTemplate, Random random) {
        if (formCompulsoryLinksMonteCarloSelection(baseTemplate, 0, random)) {
            if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                return true;
            } else {
                return formNonCompulsoryLinksMonteCarloSelection(baseTemplate, 0, random);
            }
        } else {
            discardGroup(baseTemplate);
            return false;
        }
    }

    private boolean constructGroupRandomSelection(GroupTemplate baseTemplate, Random random) {
        if (formCompulsoryLinksRandomSelection(baseTemplate, 0, random)) {
            if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                return true;
            } else {
                return formNonCompulsoryLinksRandomSelection(baseTemplate, 0, random);
            }
        } else {
            return false;
        }
    }

    private boolean formCompulsoryLinks(GroupTemplate baseTemplate, int refMemberIndex) {
        Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links
                List<TargetAgentType> sortedTargets = LinkRulesWrapper.get(refAgent, linkType);
                removeDepleted(baseTemplate.getGroupType(), sortedTargets);
                // sortByRMSE(baseTemplate.getGroupType(), sortedTargets);
                for (TargetAgentType target : sortedTargets) {
                    Member newMember = addMember(baseTemplate, refAgent, linkType, target);
                    if (groupSizeRejectionCriterion.validate(baseTemplate) == Reject.YES) {
                        removeMember(baseTemplate, newMember); // This newMember doesn't work, lets try a different one
                    } else {
                        // This member looks OK for now, lets try forming it's links
                        isSuccessful = formCompulsoryLinks(baseTemplate, ++refMemberIndex);
                        if (!isSuccessful) {// This member cannot complete its links in this group. We have to try a differnt member
                            removeMember(baseTemplate, newMember);
                        } else {
                            break;// This new member works, now try the next link
                        }
                    }
                }
                if (!isSuccessful)// This link could not be formed. No point in continuing with this agent
                    return !isSuccessful;
            }

        }

        // At this point, group template construction is successful so far.
        if (refMemberIndex == 0 && globalDefaultAcceptance.validate(baseTemplate) == Accept.NO) {
            discardGroup(baseTemplate);
            return false;
        } else {
            return true;
        }
    }

    private boolean formCompulsoryLinksRandomSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links
                List<TargetAgentType> sortedTargets = LinkRulesWrapper.get(refAgent, linkType);
                // Remove 0 probability agents
                sortedTargets = sortedTargets.stream().filter(t -> targetProportions.get(baseTemplate.getGroupType(), t) > 0)
                        .collect(Collectors.toList());
                if (!sortedTargets.isEmpty()) {
                    TargetAgentType targetAgentType = sortedTargets.get(random.nextInt(sortedTargets.size()));
                    Member newMember = addMember(baseTemplate, refAgent, linkType, targetAgentType);

                    if (groupSizeRejectionCriterion.validate(baseTemplate) == Reject.YES) {
                        removeMember(baseTemplate, newMember); // This newMember doesn't work, lets try a different one
                        isSuccessful = false;
                    } else {
                        // This member looks OK for now, lets try forming it's links
                        isSuccessful = formCompulsoryLinksRandomSelection(baseTemplate, ++refMemberIndex, random);
                        if (!isSuccessful) {// This member cannot complete its links in this group. We have to try a different member
                            removeMember(baseTemplate, newMember);
                        } else {
                            break;// This new member works, now try the next link
                        }
                    }
                } else {
                    isSuccessful = false;
                }

                if (!isSuccessful)// This link could not be formed. No point in continuing with this agent
                    return false;
            }

        }

        // At this point, group template construction is successful so far.
        if (refMemberIndex == 0 && globalDefaultAcceptance.validate(baseTemplate) == Accept.NO) {
            discardGroup(baseTemplate);
            return false;
        } else {
            return true;
        }
    }

    private boolean formCompulsoryLinksMonteCarloSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links
                List<TargetAgentType> sortedTargets = LinkRulesWrapper.get(refAgent, linkType);

                // Select the group member that fits to the link based on random probability
                double randVal = random.nextDouble();
                double targetsCumulativeProbability = 0;
                for (TargetAgentType target : sortedTargets) {
                    targetsCumulativeProbability += targetProportions.get(baseTemplate.getGroupType(), target);
                }
                double proportionalOffset = randVal * targetsCumulativeProbability;
                Member newMember = null;
                double agentsTempOffsetSum = 0;
                for (TargetAgentType tarAgentType : sortedTargets) {
                    if (targetProportions.get(baseTemplate.getGroupType(), tarAgentType) == 0) {
                        continue; // If probability is 0, do not consider
                    }
                    agentsTempOffsetSum += targetProportions.get(baseTemplate.getGroupType(), tarAgentType);
                    if (proportionalOffset <= agentsTempOffsetSum) {
                        newMember = addMember(baseTemplate, refAgent, linkType, tarAgentType);
                        break;
                    }
                }

                if (newMember != null) {
                    if (groupSizeRejectionCriterion.validate(baseTemplate) == Reject.YES) {
                        removeMember(baseTemplate, newMember); // This newMember doesn't work, lets try a different one
                        isSuccessful = false;
                    } else {
                        // This member looks OK for now, lets try forming it's links
                        isSuccessful = formCompulsoryLinksMonteCarloSelection(baseTemplate, ++refMemberIndex, random);
                        if (!isSuccessful) {// This member cannot complete its links in this group. We have to try a different member
                            removeMember(baseTemplate, newMember);
                        } else {
                            break;// This new member works, now try the next link
                        }
                    }
                } else {
                    isSuccessful = false;
                }
                if (!isSuccessful)// This link could not be formed. No point in continuing with this agent
                    return false;
            }
        }

        // At this point, group template construction is successful so far.
        if (refMemberIndex == 0 && globalDefaultAcceptance.validate(baseTemplate) == Accept.NO) {
            discardGroup(baseTemplate);
            return false;
        } else {
            return true;
        }
    }

    private void formNonCompulsoryLinks(GroupTemplate baseTemplate, int refMemberIndex) {
        Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        GroupType gType = baseTemplate.getGroupType();
        List<Pair<LinkType, AgentType>> potentialTargets = new ArrayList<>();
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            if (linkType.isActive()) {
                int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                if (existingLinks < linkType.getMaxLinks()) {
                    for (TargetAgentType target : LinkRulesWrapper.get(refAgent, linkType)) {
                        if (targetCounts.get(gType, target) > currentCounts.get(gType, target)) {
                            potentialTargets.add(Pair.of(linkType, target));
                        }
                    }
                }
            }
        }
        // sortPairByRMSE(gType, potentialTargets);

        for (int i = 0; i < potentialTargets.size(); i++) {
            Pair<LinkType, AgentType> pair = potentialTargets.get(i);
            Member newMember = addMember(baseTemplate, refAgent, pair.getLeft(), pair.getRight());

            // Log.info("calling validation");
            if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
                removeMember(baseTemplate, newMember);
                continue;
            }

            // Removing depleted pairs
            if (targetCounts.get(gType, pair.getRight()) <= currentCounts.get(gType, pair.getRight())) {
                for (int j = 0; j < potentialTargets.size(); j++) {
                    if (potentialTargets.get(j).getRight().isSameType(pair.getRight())) {
                        potentialTargets.remove(potentialTargets.get(j));
                        if (j <= i) {
                            i--;
                        }
                    }
                }
            }
            // sortPairByRMSE(gType, potentialTargets);

            if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES
                    && groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                return;
            }
        }
        if (baseTemplate.size() > refMemberIndex + 1)
            formNonCompulsoryLinks(baseTemplate, refMemberIndex + 1);

        if (refMemberIndex == 0 && !(AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES
                && groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType())) {
            discardGroup(baseTemplate);
        }
    }

    private boolean formNonCompulsoryLinksRandomSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        while (baseTemplate.size() > refMemberIndex) {
            Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
            GroupType gType = baseTemplate.getGroupType();
            int expectedGroupSize = (int) GroupRulesWrapper.get(gType, GroupRuleKeys.MAX_MEMBERS);

            // Find current reference member's potential targets

            List<Pair<LinkType, AgentType>> potentialTargets = new ArrayList<>();
            for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
                if (linkType.isActive()) {
                    int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                    if (existingLinks < linkType.getMaxLinks()) {
                        for (TargetAgentType target : LinkRulesWrapper.get(refAgent, linkType)) {
                            if (targetProportions.get(gType, target) > 0) {
                                potentialTargets.add(Pair.of(linkType, target));
                            }
                        }
                    }
                }
            }
            // If current reference member has no remaining potential targets, try the next in line. After that exit.
            if (potentialTargets.isEmpty()) {
                refMemberIndex++;
                continue;
            }

            for (int count = 0; count < expectedGroupSize - baseTemplate.size(); count++) {
                int attempt = 0;
                while (attempt < potentialTargets.size()) {
                    attempt++;

                    Pair<LinkType, AgentType> selectedPair = potentialTargets.get(random.nextInt(potentialTargets.size()));
                    if (targetProportions.get(baseTemplate.getGroupType(), selectedPair.getRight()) == 0) {
                        continue; // If probability is 0, do not consider
                    }

                    Member newMember = addMember(baseTemplate, refAgent, selectedPair.getLeft(), selectedPair.getRight());

                    // Log.info("calling validation");
                    if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
                        removeMember(baseTemplate, newMember);
                        potentialTargets.remove(selectedPair);
                        break;
                    }

                    if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES
                            && groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                        return true;
                    }

                    if (potentialTargets.isEmpty()) {
                        break;
                    }
                }
                if (potentialTargets.isEmpty()) {
                    break;
                }

            }

            refMemberIndex++;

        }

        discardGroup(baseTemplate);
        return false;

    }

    private boolean formNonCompulsoryLinksMonteCarloSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        while (baseTemplate.size() > refMemberIndex) {
            Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
            GroupType gType = baseTemplate.getGroupType();
            int expectedGroupSize = (int) GroupRulesWrapper.get(gType, GroupRuleKeys.MAX_MEMBERS);

            // Find current reference member's potential targets
            double cumulativeProbability = 0;
            List<Pair<LinkType, AgentType>> potentialTargets = new ArrayList<>();
            for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
                if (linkType.isActive()) {
                    int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                    if (existingLinks < linkType.getMaxLinks()) {
                        for (TargetAgentType target : LinkRulesWrapper.get(refAgent, linkType)) {
                            potentialTargets.add(Pair.of(linkType, target));
                            cumulativeProbability += targetProportions.get(gType, target);

                        }
                    }
                }
            }
            // If current reference member has no remaining potential targets, try the next in line. After that exit.
            if (potentialTargets.isEmpty()) {
                refMemberIndex++;
                continue;
            }
            int attempts = 0;
            while (attempts < maxAttempts && expectedGroupSize > baseTemplate.size()) {
                attempts++;
                double randVal = random.nextDouble();
                double proportionalOffset = randVal * cumulativeProbability;
                double agentsTempOffsetSum = 0;

                for (int i = 0; i < potentialTargets.size(); i++) {
                    Pair<LinkType, AgentType> pair = potentialTargets.get(i);
                    if (targetProportions.get(baseTemplate.getGroupType(), pair.getRight()) == 0) {
                        continue; // If probability is 0, do not consider
                    }
                    agentsTempOffsetSum += targetProportions.get(baseTemplate.getGroupType(), pair.getRight());
                    Member newMember = null;
                    if (proportionalOffset <= agentsTempOffsetSum) {
                        newMember = addMember(baseTemplate, refAgent, pair.getLeft(), pair.getRight());

                        // Log.info("calling validation");
                        if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
                            removeMember(baseTemplate, newMember);
                            potentialTargets.remove(pair);
                            cumulativeProbability -= targetProportions.get(gType, pair.getRight());
                            break;
                        }

                        if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES
                                && groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                            return true;
                        }
                        break;
                    }
                }
                if (potentialTargets.isEmpty()) {
                    break;
                }
            }

            refMemberIndex++;

        }

        discardGroup(baseTemplate);
        return false;

    }

    private <T> T get(Collection<T> collection, int index) {
        int i = 0;
        for (T item : collection) {
            if (i == index) {
                return item;
            }
            i++;
        }
        throw new IndexOutOfBoundsException("size: " + collection.size() + " index: " + index);
    }

}