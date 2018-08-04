package bnw.abm.intg.algov1.nonipfp;

import bnw.abm.intg.algov1.NewGroupBuilder;
import bnw.abm.intg.algov2.framework.models.*;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.RejectionCriterion;
import bnw.abm.intg.util.Log;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author wniroshan 17 May 2018
 */
public class GroupsOptimiser extends NewGroupBuilder {

    private final Random random;
    private final ErrorModels errorModels;

    public GroupsOptimiser(final IPFPTable seed,
                           final LinkedHashMap<ReferenceAgentType, Integer> targetAgentCounts,
                           final LinkedHashMap<GroupType, Integer> targetGroupCounts,
                           final GroupTypeLogic groupTypeLogic,
                           final Random random) {
        super(targetGroupCounts, targetAgentCounts, groupTypeLogic, seed);
        this.random = random;
        this.errorModels = new ErrorModels(targetAgentCounts, targetGroupCounts);
    }

    public Population hillClimbingCounts(Population population, int maxIterations) {
        double agentErrorProminance = 1;

        double lowestError = calcRMSECounts(population, agentErrorProminance);

        int rounds = 0;
        while (rounds < maxIterations) {// FIXME:Change this to a target error and a number of rounds
            rounds++;
            int changesErr = 0;
            for (int i = 0; i < 800; i++) {
                Group removeGroup = randomSelectGroup(population);
                population.removeGroup(removeGroup);
                Group proposedChange = constructNewGroupWithRandomSelection(removeGroup.type());

                double newError = calcRMSECountsWithChange(population, proposedChange, agentErrorProminance);
                if (newError < lowestError) {
                    population.addGroup(proposedChange);
                    lowestError = newError;
                    changesErr++;
                } else {
                    population.addGroup(removeGroup);
                }
            }

            // population.addToErrorList(lowestError); // recording error reduction
            System.out.print("\rRound " + rounds + " Changes made on error: " + changesErr+ " Current error: "+lowestError+"         ");
        }
        return population;
    }

    public Population simulatedAnnealingCounts(Population population,
                                               Random random,
                                               double temperature,
                                               double coolingRate,
                                               double exitTemperature) {

        double bestEnergy = Double.POSITIVE_INFINITY;
        Population bestPop = null;
        int changesCount = 0;
        int improvingChanges = 0;

        Map<ReferenceAgentType, Integer> addedAgentSummary = new LinkedHashMap<>(), removedAgentSummary = new LinkedHashMap<>();
        for (ReferenceAgentType agentType : population.getAgentsSummary().keySet()) {
            addedAgentSummary.put(agentType, 0);
            removedAgentSummary.put(agentType, 0);
        }


        double currentEnergy = calcRMSECounts(population, 1);
        //        double currentEnergy = calcAAPD(population, 1);

        while (temperature > exitTemperature) {

            // Prepare new solution
            Group removeGroup = randomSelectGroup(population);
            population.removeGroup(removeGroup);
            Group proposedChange = constructNewGroupWithRandomSelection(removeGroup.type());

            double neighbourEnergy = calcRMSECountsWithChange(population, proposedChange, 1);
            //            double neighbourEnergy = calcAAPDWithChange(population, proposedChange, 1);

            // Decide if we should accept the neighbour
            if (acceptanceProbability(currentEnergy, neighbourEnergy, temperature) > random.nextDouble()) {
                population.addGroup(proposedChange);
                currentEnergy = neighbourEnergy;
                ++changesCount;
                if (changesCount % 10000 == 0) {
                    population.addToErrorList(currentEnergy);
                }

            } else {
                population.addGroup(removeGroup);
            }

            // Keep track of the best solution found
            if (currentEnergy < bestEnergy) {
                bestPop = population.copy();
                bestEnergy = currentEnergy;
                ++improvingChanges;
            }

            // Cool system
            temperature *= 1 - coolingRate;
            System.out.print("\rChanges: " + changesCount + " Improvements:" + improvingChanges + " Temp:" + temperature + " Population" +
                                     " size:" + population.size() + " Error:" + bestEnergy + " Current Error: " + currentEnergy + "      " +
                                     "      ");
        }

        Log.info("Final population error: " + bestEnergy);
        return bestPop;
    }

    public Population simulatedAnnealingProportions(Population population,
                                                    Random random,
                                                    double temperature,
                                                    double coolingRate,
                                                    double exitTemperature) {

        double bestEnergy = Double.POSITIVE_INFINITY;
        Population bestPop = null;
        int changesCount = 0;
        int improvingChanges = 0;

        double currentEnergy = errorModels.calcProportionsError(population, 1);

        while (temperature > exitTemperature) {

            // Prepare new solution
            Group removeGroup = randomSelectGroup(population);
            population.removeGroup(removeGroup);
            Group proposedChange = constructNewGroupWithRandomSelection(removeGroup.type());

            double neighbourEnergy = errorModels.calcProportionsErrorWithChange(population, proposedChange, 1);

            // Decide if we should accept the neighbour
            if (acceptanceProbability(currentEnergy, neighbourEnergy, temperature) > random.nextDouble()) {
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
            temperature *= 1 - coolingRate;
            System.out.print("\rChanges: " + changesCount + " Improvements: " + improvingChanges + " Temp: " + temperature + " Population" +
                                     " size: "
                                     + population.size() + " Best Error: " + bestEnergy + " Current error: " + currentEnergy + "        ");
        }

        Log.info("Final population error: " + bestEnergy);
        return bestPop;
    }

    /**
     * Calculate probability to accept a suggestion in simulated annealing based on metropolis criteria
     *
     * @param energy      Current error
     * @param newEnergy   Error after the change
     * @param temperature Current temperature for annealing
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

    /**
     * Randomly select a group from current population
     *
     * @param population current Population
     * @return selected group
     */
    private Group randomSelectGroup(Population population) {
        int groupsCount = population.getGroups().size();
        int randomIndex = random.nextInt(groupsCount);
        return population.getGroups().get(randomIndex);
    }

    /**
     * Calculates the error in the population after proposed change. Does not add the proposed changed to the population.
     *
     * @param population       current population
     * @param proposedChange   group to be added
     * @param agentsProminence How significant agent category's error with respect to groups' mean squared error.
     * @return Mean squared error if the proposed change is made
     */
    private double calcRMSECountsWithChange(Population population, Group proposedChange, double agentsProminence) {
        return calcGroupRMSE(population, proposedChange)
                + (agentsProminence * calcAgentsRMSE(population, proposedChange));
    }

    /**
     * Calculates total error in the population, to be used in objective function in simulated annealing
     *
     * @param population       Current population
     * @param agentsProminence How significant agent category's error with respect to groups' mean squared error.
     * @return current mean squared error of the population
     */
    private double calcRMSECounts(Population population, double agentsProminence) {
        return calcGroupRMSE(population, null) + (agentsProminence * calcAgentsRMSE(population, null));
    }

    private double calcGroupRMSE(Population population, Group newGroup) {

        double totalExpectedGroups = expectedGroupTypeCounts.values().stream().mapToDouble(v -> v).sum();
        double currentTotalGroups = population.getGroupsSummary().values().stream().mapToDouble(v -> v).sum();

        double sumSquaredError = 0;
        for (GroupType groupType : expectedGroupTypeCounts.keySet()) {
            double currentGroupCount = 0;
            if (newGroup != null && groupType == newGroup.type()) {
                currentGroupCount = (population.getGroupsSummary().get(groupType) + 1);
            } else {
                currentGroupCount = population.getGroupsSummary().get(groupType);
            }

            double inputGroupCount = expectedGroupTypeCounts.get(groupType);
//            double targetGroupCount = inputGroupCount / totalExpectedGroups * currentTotalGroups;
            sumSquaredError += Math.pow((inputGroupCount - currentGroupCount), 2);
        }
        return Math.sqrt(sumSquaredError / (double) expectedGroupTypeCounts.keySet().size());
    }

    private double calcAgentsRMSE(Population population, Group newGroup) {

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

        double totalExpectedAgents = expectedAgentTypeCounts.values().stream().mapToDouble(v -> v).sum();
        double currentTotalAgents = population.getAgentsSummary().values().stream().mapToDouble(v -> v).sum();
        double sumSquaredError = 0;

        for (ReferenceAgentType refAgentType : expectedAgentTypeCounts.keySet()) {
            double currentAgentCount = 0;
            if (newGroup != null && groupAgentsSummary.containsKey(refAgentType)) {
                currentAgentCount = (population.getAgentsSummary().get(refAgentType) + groupAgentsSummary.get(refAgentType));
            } else {
                currentAgentCount = population.getAgentsSummary().get(refAgentType);
            }

            //Calculate expected number of agents of this type in the final population
            double inputAgents = expectedAgentTypeCounts.get(refAgentType);
//            double expectedInAgentType = inputAgents / totalExpectedAgents * currentTotalAgents;

            sumSquaredError += Math.pow((inputAgents - currentAgentCount), 2);
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
    private double calcAAPDWithChange(Population population, Group proposedChange, double agentsProminence) {
        return
                (agentsProminence * calcAgentsAAPD(population, proposedChange));// + calcGroupAAPD(population, proposedChange);
    }

    /**
     * Calculates total error in the population, to be used in objective function in simulated annealing
     *
     * @param population       Current population
     * @param agentsProminence How significant agent category's error with respect to groups' mean squared error.
     * @return current mean squared error of the population
     */
    private double calcAAPD(Population population, double agentsProminence) {
        return (agentsProminence * calcAgentsAAPD(population, null)); // + calcGroupAAPD(population, null);
    }

    private double calcGroupAAPD(Population population, Group newGroup) {
        double diffPercent = 0;
        int erroneousCats = 0;
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
                if (diffPercent > 0) {
                    erroneousCats++;
                }
            } else {
                //If the group type is 0 and there are groups in it, each group is given a high penalty. So the percentage diff is x100.
                diffPercent += 0;
            }
        }
        return diffPercent == 0 ? diffPercent : diffPercent / (double) erroneousCats;
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

        int erroneousCats = 0;
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
                if (diffPercent > 0) {
                    erroneousCats++;
                }
            } else {
                //If the agent type is 0 and there are agents in it, each agent is given a high penalty. So the percentage diff is x100.
                diffPercent += 0;
            }
        }
        return diffPercent == 0 ? diffPercent : diffPercent / (double) erroneousCats;
    }


    private Group constructNewGroupWithRandomSelection(GroupType newGroupType) {

        GroupTemplate newGroupTemplate = null;
        while (true) {
            // Select a random group type
            // int groupTypesCount = targetProportions.rowKeySet().size();
            // GroupType newGroupType = (GroupType) targetProportions.rowKeySet().toArray()[random.nextInt(groupTypesCount)];

            // Select a random group head out of potential heads
            List<ReferenceAgentType> potentialGroupHeads = (List<ReferenceAgentType>) GroupRules.GroupRulesWrapper.get(newGroupType,
                                                                                                                       GroupRules
                                                                                                                               .GroupRuleKeys.REFERENCE_AGENT);
            if (potentialGroupHeads != null) {

                try {
                    potentialGroupHeads = getPossibleAgentTypesOnly(potentialGroupHeads, newGroupType);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if (potentialGroupHeads.isEmpty())
                    continue;

                ReferenceAgentType groupHead = potentialGroupHeads.get(random.nextInt(potentialGroupHeads.size()));
                GroupTemplate baseTemplate = newGroupTemplate(newGroupType, groupHead); // Construct base template

                // Construct links and populate base template
                if (formCompulsoryLinksRandomSelection(baseTemplate, 0)) {
                    if (groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                        newGroupTemplate = baseTemplate;
                        break; // We have successfully constructed a legal group template
                    } else if (formNonCompulsoryLinksRandomSelection(baseTemplate,
                                                                     0,
                                                                     random)) {// Template not complete lets try to complete it
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

    private boolean formCompulsoryLinksRandomSelection(GroupTemplate baseTemplate, int refMemberIndex) {
        GroupTemplate.Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRules.LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links
                List<TargetAgentType> sortedTargets = LinkRules.LinkRulesWrapper.get(refAgent, linkType);
                // Remove 0 probability agents
                sortedTargets = getPossibleAgentTypesOnly(sortedTargets, baseTemplate.getGroupType());

                if (!sortedTargets.isEmpty()) {
                    TargetAgentType targetAgentType = sortedTargets.get(random.nextInt(sortedTargets.size()));
                    GroupTemplate.Member newMember = addMember(baseTemplate, refAgent, linkType, targetAgentType);

                    if (groupSizeRejectionCriterion.validate(baseTemplate) == RejectionCriterion.Reject.YES) {
                        removeMember(baseTemplate, newMember); // This newMember doesn't work, lets try a different one
                        isSuccessful = false;
                    } else {
                        // This member looks OK for now, lets try forming it's links
                        isSuccessful = formCompulsoryLinksRandomSelection(baseTemplate, ++refMemberIndex);
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
        if (refMemberIndex == 0 && globalDefaultAcceptance.validate(baseTemplate) == AcceptanceCriterion.Accept.NO) {
            discardGroup(baseTemplate);
            return false;
        } else {
            return true;
        }
    }


    private boolean formNonCompulsoryLinksRandomSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        while (baseTemplate.size() > refMemberIndex) {
            GroupTemplate.Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
            GroupType gType = baseTemplate.getGroupType();
            int expectedGroupSize = (int) GroupRules.GroupRulesWrapper.get(gType, GroupRules.GroupRuleKeys.MAX_MEMBERS);

            // Find current reference member's potential targets

            List<Pair<LinkType, TargetAgentType>> potentialTargets = new ArrayList<>();
            for (LinkType linkType : LinkRules.LinkRulesWrapper.row(refAgent).keySet()) {
                if (linkType.isActive()) {
                    int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                    if (existingLinks < linkType.getMaxLinks()) {
                        for (TargetAgentType target : LinkRules.LinkRulesWrapper.get(refAgent, linkType)) {
                            if (expectedAgentTypeCounts.get(ReferenceAgentType.getExisting(target.getTypeID())) > 0) {
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

                    Pair<LinkType, TargetAgentType> selectedPair = potentialTargets.get(random.nextInt(potentialTargets.size()));
                    if (expectedAgentTypeCounts.get(ReferenceAgentType.getExisting(selectedPair.getRight().getTypeID())) == 0) {
                        continue; // If probability is 0, do not consider
                    }

                    GroupTemplate.Member newMember = addMember(baseTemplate, refAgent, selectedPair.getLeft(), selectedPair.getRight());

                    // Log.info("calling validation");
                    if (RejectionCriterion.validateAll(baseTemplate) == RejectionCriterion.Reject.YES) {
                        removeMember(baseTemplate, newMember);
                        potentialTargets.remove(selectedPair);
                        break;
                    }

                    if (AcceptanceCriterion.validateAll(baseTemplate) == AcceptanceCriterion.Accept.YES
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

}
