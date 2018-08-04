package bnw.abm.intg.algov1.apd;

import bnw.abm.intg.algov1.NewGroupBuilder;
import bnw.abm.intg.algov2.framework.models.*;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion.Accept;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.RejectionCriterion;
import bnw.abm.intg.algov2.templates.RejectionCriterion.Reject;
import bnw.abm.intg.util.Log;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry;

public class RMSEGroupsBuilder extends NewGroupBuilder {

    private final Random random;
    private final int maxGroupFromAttempts;

    /*
     * public NonIPFPGroupsBuilder(LinkRules linkRules, Map<GroupType, Double> groupTypeProportions, Map<ReferenceAgentType, Double>
     * agentTypeProportions, Map<GroupType, Integer> groupTypeCounts, Map<ReferenceAgentType, Integer> agentTypeCounts, GroupTypeLogic
     * groupTypeLogic, Map<GroupType, Integer> groupSizeMap, IPFPTable seed) {
     */
    public RMSEGroupsBuilder(LinkRules linkRules,
                             Map<GroupType, Integer> groupTypeCounts,
                             Map<ReferenceAgentType, Integer> agentTypeCounts,
                             GroupTypeLogic groupTypeLogic,
                             IPFPTable seed,
                             int maxGroupFromAttempts) {
        super(groupTypeCounts, agentTypeCounts, groupTypeLogic, seed);
        this.maxGroupFromAttempts = maxGroupFromAttempts;
        this.random = new Random(0);
        LinkRulesWrapper.setLinkRules(linkRules);
    }


    public Population build() {

        Population population = new Population(expectedGroupTypeCounts, expectedAgentTypeCounts);
        //        List<ReferenceAgentType> agentTypeSelections = new ArrayList<>(expectedAgentTypeCounts.keySet());
        List<GroupType> groupTypeSelections = new ArrayList<>(expectedGroupTypeCounts.keySet());
        groupTypeSelections.removeIf(g -> expectedGroupTypeCounts.get(g) == 0);

        int groupsFound = 0;
        sortGroupTypesByError(groupTypeSelections);
        for (int g = 0; g < groupTypeSelections.size(); g++) {
            GroupType selectedGroupType = groupTypeSelections.get(g);
            int attempts = 0;
            while (attempts < maxGroupFromAttempts) {
                List<ReferenceAgentType> possibleHeads = seed.row(selectedGroupType)
                                                             .entrySet()
                                                             .parallelStream()
                                                             .filter(e -> e.getValue() > 0)
                                                             .map(Entry::getKey)
                                                             .collect(Collectors.toList());

                sortAgentTypesByError(possibleHeads);
                for (int i = 0; i < possibleHeads.size(); ) {
                    ReferenceAgentType groupHead = possibleHeads.get(i);
                    GroupTemplate newTemplate = newGroupTemplate(selectedGroupType, groupHead);

                    if (constructGroup(newTemplate)) {
                        population.addGroup(new Group(newTemplate));
                        int currentGroups = currentGroupTypeCounts.get(newTemplate.getGroupType());
                        currentGroupTypeCounts.put(newTemplate.getGroupType(), currentGroups + 1);

                        groupsFound++;
                        attempts = 0;

                        break;
                    } else {
                        i++;
                    }
                }

            }

        }

        Log.info("Total groups: " + groupsFound);
        return population;
    }

    private boolean constructGroup(GroupTemplate baseTemplate) {
        boolean isComplete = false;
        if (formCompulsoryLinks(baseTemplate, 0)) {
            if (this.groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                isComplete = true;
            } else {
                for (int i = 0; i < baseTemplate.getAllMembers().size(); i++) {
                    Member refAgent = get(baseTemplate.getAllMembers(), i);
                    isComplete = formNonCompulsoryLinks(baseTemplate, refAgent);
                    if (isComplete) {
                        break;
                    }
                }
            }
        }
        return isComplete;
    }

    private boolean formCompulsoryLinks(GroupTemplate baseTemplate, int refMemberIndex) {
        Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links
                List<TargetAgentType> targetAgentTypes = LinkRulesWrapper.get(refAgent, linkType);
                targetAgentTypes = targetAgentTypes.parallelStream()
                                                   .filter(at -> seed.get(baseTemplate.getGroupType(),
                                                                          ReferenceAgentType.getExisting(at.getTypeID())) > 0)
                                                   .collect(

                                                           Collectors.toList());
                // removeDepleted(targetAgentTypes);
                sortAgentTypesByError(targetAgentTypes);
                for (TargetAgentType target : targetAgentTypes) {
                    Member newMember = addMember(baseTemplate, refAgent, linkType, target);
                    if (groupSizeRejectionCriterion.validate(baseTemplate) == Reject.YES) {
                        removeMember(baseTemplate, newMember); // This newMember doesn't work, lets try a different one
                    } else {
                        // This member looks OK for now, lets try forming it's links
                        isSuccessful = formCompulsoryLinks(baseTemplate, ++refMemberIndex);
                        if (!isSuccessful) {// This member cannot complete its links in this group. We have to try a different member
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

    private boolean formNonCompulsoryLinks(GroupTemplate baseTemplate, Member refAgent) {
        List<Pair<LinkType, TargetAgentType>> potentialTargets = new ArrayList<>();
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            if (linkType.isActive()) {
                int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                if (existingLinks < linkType.getMaxLinks()) {
                    for (TargetAgentType target : LinkRulesWrapper.get(refAgent, linkType)) {
                        potentialTargets.add(Pair.of(linkType, target));
                        // if (expectedAgentTypeCounts.get(targetR) > currentAgentTypeCounts.get(targetR)) {
                        //
                        // }
                    }
                }
            }
        }
        sortPairByError(potentialTargets);
        for (int i = 0; i < potentialTargets.size(); i++) {
            Pair<LinkType, TargetAgentType> pair = potentialTargets.get(i);
            Member newMember = addMember(baseTemplate, refAgent, pair.getLeft(), pair.getRight());

            if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES) {
                if (groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                    return true;
                } else {
                    // Not complete yet, but no problems
                }
            } else if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
                removeMember(baseTemplate, newMember);
                continue;
            } else {
                // This means Accpet.NO & Reject.NO
                // We have already formed all Compulsory links for this group. But this can be caused by an agent we added just now.
            }

            // Removing depleted pairs - there can be multiple pairs of same agent type (with different link types). We have to remove
            // all of
            // them.
            // ReferenceAgentType convertedtoRef = ReferenceAgentType.getExisting(pair.getRight().getTypeID());
            // if (expectedAgentTypeCounts.get(convertedtoRef) <= currentAgentTypeCounts.get(convertedtoRef)) {
            // for (int j = 0; j < potentialTargets.size(); j++) {
            // if (potentialTargets.get(j).getRight().isSameType(pair.getRight())) {
            // potentialTargets.remove(potentialTargets.get(j));
            // }
            // }
            // }
            sortPairByError(potentialTargets);
            i = 0;
        }
        return false;
    }

    private AgentType minimumErrorAgentType() {
        double minimumError = Double.POSITIVE_INFINITY;
        AgentType minimumErrorAgentType = null;
        for (ReferenceAgentType agentType : expectedAgentTypeCounts.keySet()) {
            double newError = calculateError(agentType);
            if (minimumError > newError) {
                minimumError = newError;
                minimumErrorAgentType = agentType;
            }
        }
        return minimumErrorAgentType;
    }

    private double calculateError(GroupType groupType) {
        double squaredSum = 0;
        if (expectedGroupTypeCounts.get(groupType) != 0) {
            for (GroupType gType : expectedGroupTypeCounts.keySet()) {
                // double targetProportion = expectedGroupTypeCounts.get(gType) / (double) targetGroupsCount;
                if (gType != groupType) {
                    squaredSum += Math.pow(expectedGroupTypeCounts.get(gType) - currentGroupTypeCounts.get(gType), 2);
                } else {
                    squaredSum += Math.pow(expectedGroupTypeCounts.get(gType) - (currentGroupTypeCounts.get(gType) + 1), 2);
                }
            }
        } else {
            squaredSum = Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(squaredSum / (double) expectedGroupTypeCounts.size());
    }

    private double calculateError(AgentType agentType) {
        double squaredSum = 0;
        ReferenceAgentType agentTypeRef = ReferenceAgentType.getExisting(agentType.getTypeID());
        if (expectedAgentTypeCounts.get(agentTypeRef) != 0) {
            for (ReferenceAgentType aType : expectedAgentTypeCounts.keySet()) {
                if (aType != agentTypeRef) {
                    squaredSum += Math.pow(expectedAgentTypeCounts.get(aType) - currentAgentTypeCounts.get(aType), 2);
                } else {
                    squaredSum += Math.pow(expectedAgentTypeCounts.get(aType) - (currentAgentTypeCounts.get(aType) + 1), 2);
                }
            }
        } else {
            squaredSum = Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(squaredSum / (double) expectedAgentTypeCounts.size());
    }


    // private void removeDepleted(List<? extends AgentType> agentsList) {
    // for (int i = 0; i < agentsList.size(); i++) {
    // ReferenceAgentType agent = ReferenceAgentType.getExisting(agentsList.get(i).getTypeID());
    // if (expectedAgentTypeCounts.get(agent) <= currentAgentTypeCounts.get(agent)) {
    // agentsList.remove(agentsList.get(i));
    // i--;
    // }
    // }
    // }
    //
    // private void removeDepletedPairs(GroupType gType, List<Pair<LinkType, ? extends AgentType>> pairsList) {
    // for (int i = 0; i < pairsList.size(); i++) {
    // ReferenceAgentType agent = ReferenceAgentType.getExisting(pairsList.get(i).getRight().getTypeID());
    // if (expectedAgentTypeCounts.get(agent) <= currentAgentTypeCounts.get(agent)) {
    // pairsList.remove(agent);
    // i--;
    // }
    // }
    // }

    private void sortGroupTypesByError(List<GroupType> selection) {
        // removeDepleted(selection);
        Collections.shuffle(selection, random);
        Collections.sort(selection, new Comparator<GroupType>() {
            public int compare(GroupType o1, GroupType o2) {
                Double s_1 = calculateError(o1);
                Double s_2 = calculateError(o2);
                if (s_1 == Double.NaN || s_2 == Double.NaN) {
                    Log.warn("Detected Double.NaN - caution!!");
                }
                return s_1.compareTo(s_2);
            }
        });
    }

    private <A extends AgentType> void sortAgentTypesByError(List<A> selection) {
        Collections.shuffle(selection, random);
        Collections.sort(selection, new Comparator<A>() {
            public int compare(A o1, A o2) {
                Double s_1 = calculateError(o1);
                Double s_2 = calculateError(o2);
                if (s_1 == Double.NaN || s_2 == Double.NaN) {
                    Log.warn("Detected Double.NaN - caution!!");
                }
                return s_1.compareTo(s_2);
            }
        });
    }

    private <A extends AgentType> void sortPairByError(List<Pair<LinkType, A>> selection) {
        Collections.shuffle(selection, random);
        Collections.sort(selection, new Comparator<Pair<LinkType, A>>() {
            public int compare(Pair<LinkType, A> o1, Pair<LinkType, A> o2) {
                Double s_1 = calculateError(o1.getRight());
                Double s_2 = calculateError(o2.getRight());
                if (s_1 == Double.NaN || s_2 == Double.NaN) {
                    Log.warn("Detected Double.NaN - caution!!");
                }
                return s_1.compareTo(s_2);
            }
        });
    }

}
