package bnw.abm.intg.algov1.nonipfp;

import bnw.abm.intg.algov1.NewGroupBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.*;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion.Accept;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.RejectionCriterion;
import bnw.abm.intg.algov2.templates.RejectionCriterion.Reject;
import bnw.abm.intg.util.Log;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author wniroshan 16 May 2018
 */
public class NonIPFMonteCarloGroupsBuilder extends NewGroupBuilder {

    private final Random random;
    private final int maxGroupFromAttempts;
    // private Map<GroupType, Double> groupTypeProportions;
    private Map<GroupType, Integer> currentGroupTypeCounts, targetGroupTypeCounts;
    // private Map<ReferenceAgentType, Double> agentTypeProportions;
    private Map<ReferenceAgentType, Integer> currentAgentTypeCounts, targetAgentTypeCounts;

    public NonIPFMonteCarloGroupsBuilder(LinkRules linkRules,
                                         Map<GroupType, Integer> groupTypeCounts,
                                         Map<ReferenceAgentType, Integer> agentTypeCounts,
                                         GroupTypeLogic groupTypeLogic,
                                         IPFPTable seed,
                                         int maxGroupFromAttempts,
                                         Random random) {
        super(groupTypeCounts, agentTypeCounts, groupTypeLogic, seed);
        this.maxGroupFromAttempts = maxGroupFromAttempts;
        this.random = random;
        LinkRulesWrapper.setLinkRules(linkRules);

        // this.agentTypeProportions = agentTypeProportions;
        // this.groupTypeProportions = groupTypeProportions;
        this.targetAgentTypeCounts = agentTypeCounts;
        this.targetGroupTypeCounts = groupTypeCounts;

        this.currentAgentTypeCounts = new LinkedHashMap<>(agentTypeCounts);
        this.currentAgentTypeCounts.forEach((k, v) -> this.currentAgentTypeCounts.put(k, 0));
        this.currentGroupTypeCounts = new LinkedHashMap<>(groupTypeCounts);
        this.currentGroupTypeCounts.forEach((k, v) -> this.currentGroupTypeCounts.put(k, 0));

    }

    public Population build() {
        Population population = new Population(targetGroupTypeCounts, targetAgentTypeCounts);
        int currentGroups = 0;

        for (GroupType gType : targetGroupTypeCounts.keySet()) {
            List<ReferenceAgentType> allGroupHeads = (List<ReferenceAgentType>) GroupRulesWrapper.get(gType,
                                                                                                      GroupRuleKeys.REFERENCE_AGENT);

            double groupHeadAgentCumulativeProbability = getPossibleAgentTypesOnly(allGroupHeads, gType).parallelStream()
                                                                                                        .mapToDouble(head -> targetAgentTypeCounts
                                                                                                               .get(head))
                                                                                                        .sum();
            int countInThisGType = 0;
            int iterationsBeforeTermination = maxGroupFromAttempts;
            while (countInThisGType < targetGroupTypeCounts.get(gType)) {
                // Now we have to randomly select a group head

                double randVal = random.nextDouble();
                double proportionalOffset = randVal * groupHeadAgentCumulativeProbability;
                ReferenceAgentType groupHead = null;
                double agentsTempOffsetSum = 0;
                for (ReferenceAgentType refAgentType : allGroupHeads) {

                    agentsTempOffsetSum += targetAgentTypeCounts.get(refAgentType);
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
                    iterationsBeforeTermination = maxGroupFromAttempts;
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

    private boolean formCompulsoryLinksMonteCarloSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        GroupTemplate.Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
        boolean isSuccessful = true;
        for (LinkType linkType : LinkRulesWrapper.row(refAgent).keySet()) {
            int remainingLinks = linkType.getMinLinks() - baseTemplate.getAdjacentMembers(refAgent, linkType).size();

            for (int linkCount = 0; linkCount < remainingLinks && linkType.isActive(); linkCount++) {// Form minimum required links

                List<TargetAgentType> sortedTargets = LinkRulesWrapper.get(refAgent, linkType);
                sortedTargets = getPossibleAgentTypesOnly(sortedTargets, baseTemplate.getGroupType());

                // Select the group member that fits to the link based on random probability
                double randVal = random.nextDouble();
                double targetsCumulativeProbability = 0;
                for (TargetAgentType target : sortedTargets) {
                    targetsCumulativeProbability += targetAgentTypeCounts.get(ReferenceAgentType.getExisting(target.getTypeID()));
                }
                double proportionalOffset = randVal * targetsCumulativeProbability;
                GroupTemplate.Member newMember = null;
                double agentsTempOffsetSum = 0;
                for (TargetAgentType tarAgentType : sortedTargets) {

                    agentsTempOffsetSum += targetAgentTypeCounts.get(ReferenceAgentType.getExisting(tarAgentType.getTypeID()));
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
        if ((refMemberIndex == 0) && (globalDefaultAcceptance.validate(baseTemplate) == Accept.NO)) {
            discardGroup(baseTemplate);
            return false;
        } else {
            return true;
        }
    }

    private boolean formNonCompulsoryLinksMonteCarloSelection(GroupTemplate baseTemplate, int refMemberIndex, Random random) {
        while (baseTemplate.size() > refMemberIndex) {
            GroupTemplate.Member refAgent = get(baseTemplate.getAllMembers(), refMemberIndex);
            GroupType gType = baseTemplate.getGroupType();
            int expectedGroupSize = (int) GroupRules.GroupRulesWrapper.get(gType, GroupRules.GroupRuleKeys.MAX_MEMBERS);

            // Find current reference member's potential targets
            double cumulativeProbability = 0;
            List<Pair<LinkType, TargetAgentType>> potentialTargets = new ArrayList<>();
            for (LinkType linkType : LinkRules.LinkRulesWrapper.row(refAgent).keySet()) {
                if (linkType.isActive()) {
                    int existingLinks = baseTemplate.getAdjacentMembers(refAgent, linkType).size();
                    if (existingLinks < linkType.getMaxLinks()) {
                        for (TargetAgentType target : (List<TargetAgentType>) getPossibleAgentTypesOnly(LinkRules.LinkRulesWrapper.get(
                                refAgent,
                                linkType),
                                                                                                        gType)) {
                            potentialTargets.add(Pair.of(linkType, target));
                            cumulativeProbability += targetAgentTypeCounts.get(ReferenceAgentType.getExisting(target.getTypeID()));

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
            while (attempts < maxGroupFromAttempts && expectedGroupSize > baseTemplate.size()) {
                attempts++;
                double randVal = random.nextDouble();
                double proportionalOffset = randVal * cumulativeProbability;
                double agentsTempOffsetSum = 0;

                for (int i = 0; i < potentialTargets.size(); i++) {
                    Pair<LinkType, TargetAgentType> pair = potentialTargets.get(i);

                    agentsTempOffsetSum += targetAgentTypeCounts.get(ReferenceAgentType.getExisting(pair.getRight().getTypeID()));
                    GroupTemplate.Member newMember = null;
                    if (proportionalOffset <= agentsTempOffsetSum) {
                        newMember = addMember(baseTemplate, refAgent, pair.getLeft(), pair.getRight());

                        // Log.info("calling validation");
                        if (RejectionCriterion.validateAll(baseTemplate) == RejectionCriterion.Reject.YES) {
                            removeMember(baseTemplate, newMember);
                            potentialTargets.remove(pair);
                            cumulativeProbability -= targetAgentTypeCounts.get(ReferenceAgentType.getExisting(pair.getRight().getTypeID()));
                            break;
                        }

                        if (AcceptanceCriterion.validateAll(baseTemplate) == AcceptanceCriterion.Accept.YES
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

}
