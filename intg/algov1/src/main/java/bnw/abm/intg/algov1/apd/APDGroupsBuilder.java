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

public class APDGroupsBuilder extends NewGroupBuilder {

    private final Random random;
    private final int maxGroupFromAttempts;

    /*
     * public NonIPFPGroupsBuilder(LinkRules linkRules, Map<GroupType, Double> groupTypeProportions, Map<ReferenceAgentType, Double>
     * agentTypeProportions, Map<GroupType, Integer> groupTypeCounts, Map<ReferenceAgentType, Integer> agentTypeCounts, GroupTypeLogic
     * groupTypeLogic, Map<GroupType, Integer> groupSizeMap, IPFPTable seed) {
     */
    public APDGroupsBuilder(LinkRules linkRules,
                            Map<GroupType, Integer> groupTypeCounts,
                            Map<ReferenceAgentType, Integer> agentTypeCounts,
                            GroupTypeLogic groupTypeLogic,
                            IPFPTable seed,
                            int maxGroupFromAttempts,
                            Random random) {
        super(groupTypeCounts, agentTypeCounts, groupTypeLogic, seed);
        this.maxGroupFromAttempts = maxGroupFromAttempts;
        this.random = new Random(0);
        LinkRulesWrapper.setLinkRules(linkRules);
    }


    public Population build() {
        Population population = new Population(expectedGroupTypeCounts, expectedAgentTypeCounts);
        int currentGroups = 0;

        for (GroupType gType : expectedGroupTypeCounts.keySet()) {
            List<ReferenceAgentType> allGroupHeads = (List<ReferenceAgentType>) GroupRules.GroupRulesWrapper.get(gType,
                                                                                                                 GroupRules.GroupRuleKeys
                                                                                                                         .REFERENCE_AGENT);

            int countInThisGType = 0;
            int iterationsBeforeTermination = maxGroupFromAttempts;
            while (countInThisGType < expectedGroupTypeCounts.get(gType)) {

                //                ReferenceAgentType groupHead = minimumErrorAgentType(allGroupHeads);
                //FIXME
                sortAgentTypesByAPD(allGroupHeads);
                for (ReferenceAgentType groupHead : allGroupHeads) {
                    GroupTemplate newTemplate = newGroupTemplate(gType, groupHead);
                    boolean isSuccessful = constructGroup(newTemplate);
                    if (isSuccessful) {
                        population.addGroup(new Group(newTemplate));
                        currentGroups++;
                        countInThisGType++;

                        iterationsBeforeTermination = maxGroupFromAttempts;
                        break;
                    } else {

                        iterationsBeforeTermination--;
                    }

                    if (iterationsBeforeTermination == 0) {
                        break;
                    }
                }

            }

            Log.info("Group Type: " + gType + " total groups: " + population.getGroups().size() + " total agents: " + population.size());
        }

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
                targetAgentTypes = getPossibleAgentTypesOnly(targetAgentTypes, baseTemplate.getGroupType());

                // removeDepleted(targetAgentTypes);
                //FIXME
                sortAgentTypesByAPD(targetAgentTypes);
                for (TargetAgentType target : targetAgentTypes) {
//                    //FIXME
                    //                    TargetAgentType target = minimumErrorAgentType(targetAgentTypes);
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
                    }
                }
            }
        }

        sortPairByAPD(potentialTargets);
                for (int i = 0; i < potentialTargets.size(); i++) {
        Pair<LinkType, TargetAgentType> pair = potentialTargets.get(0);

        Member newMember = addMember(baseTemplate, refAgent, pair.getLeft(), pair.getRight());

        if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES) {
            if (groupTypeLogic.computeGroupType(baseTemplate) == baseTemplate.getGroupType()) {
                return true;
            } else {
                // Not complete yet, but no problems
            }
        } else if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
            removeMember(baseTemplate, newMember);
            //            continue; FIXME
        } else {
            // This means Accpet.NO & Reject.NO
            // We have already formed all Compulsory links for this group. But this can be caused by an agent we added just now.
        }

        sortPairByAPD(potentialTargets);
                i = 0;
                }
        return false;
    }

    private <A extends AgentType> A minimumErrorAgentType(List<A> agentTypes) {
        double minimumError = Double.POSITIVE_INFINITY;
        A minimumErrorAgentType = null;
        for (A agentType : agentTypes) {
            double newError = calculateAPD(agentType);
            if (minimumError > newError) {
                minimumError = newError;
                minimumErrorAgentType = agentType;
            }
        }
        return minimumErrorAgentType;
    }

    private double calculateAPD(GroupType groupType) {
        double diffPercent = 0;
        if (expectedGroupTypeCounts.get(groupType) != 0) {
            for (GroupType gType : expectedGroupTypeCounts.keySet()) {
                if (expectedGroupTypeCounts.get(gType) > 0) {
                    if (gType != groupType) {
                        diffPercent += expectedGroupTypeCounts.get(gType) - currentGroupTypeCounts.get(gType) / expectedGroupTypeCounts.get(
                                gType) * 100;
                    } else {
                        diffPercent += (expectedGroupTypeCounts.get(gType) - currentGroupTypeCounts.get(gType) + 1) / expectedGroupTypeCounts
                                .get(gType) * 100;
                    }
                } else {
                    diffPercent += 100;
                }
            }
        } else {
            diffPercent = Double.POSITIVE_INFINITY;
        }
        return diffPercent;
    }

    private double calculateAPD(AgentType agentType) {
        ReferenceAgentType refAgentType = ReferenceAgentType.getExisting(agentType.getTypeID());
        double diffPercent = 0;
        if (expectedAgentTypeCounts.get(refAgentType) != 0) {
            for (ReferenceAgentType aType : expectedAgentTypeCounts.keySet()) {
                if (expectedAgentTypeCounts.get(aType) > 0) {
                    if (aType != refAgentType) {
                        diffPercent += expectedAgentTypeCounts.get(aType) - currentAgentTypeCounts.get(aType) / expectedAgentTypeCounts.get(
                                aType) * 100;
                    } else {
                        diffPercent += (expectedAgentTypeCounts.get(aType) - currentAgentTypeCounts.get(aType) + 1) / expectedAgentTypeCounts
                                .get(aType) * 100;
                    }
                } else {
                    diffPercent += 100;
                }
            }
        } else {
            diffPercent = Double.POSITIVE_INFINITY;
        }
        return diffPercent;
    }

    private void sortGroupTypesByError(List<GroupType> selection) {
        // removeDepleted(selection);
        Collections.shuffle(selection, random);
        selection.sort((o1, o2) -> {
            Double s_1 = calculateAPD(o1);
            Double s_2 = calculateAPD(o2);
            if (s_1 == Double.NaN || s_2 == Double.NaN) {
                Log.warn("Detected Double.NaN - caution!!");
            }
            return s_1.compareTo(s_2);
        });
    }

    private <A extends AgentType> void sortAgentTypesByAPD(List<A> selection) {
        Collections.shuffle(selection, random);
        selection.sort((o1, o2) -> {
            Double s_1 = calculateAPD(o1);
            Double s_2 = calculateAPD(o2);
            if (s_1 == Double.NaN || s_2 == Double.NaN) {
                Log.warn("Detected Double.NaN - caution!!");
            }
            return s_1.compareTo(s_2);
        });
    }

    private <A extends AgentType> void sortPairByAPD(List<Pair<LinkType, A>> selection) {
        Collections.shuffle(selection, random);
        selection.sort((o1, o2) -> {
            Double s_1 = calculateAPD(o1.getRight());
            Double s_2 = calculateAPD(o2.getRight());
            if (s_1 == Double.NaN || s_2 == Double.NaN) {
                Log.warn("Detected Double.NaN - caution!!");
            }
            return s_1.compareTo(s_2);
        });
    }

}
