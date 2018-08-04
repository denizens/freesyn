package bnw.abm.intg.algov1;

import bnw.abm.intg.algov1.Algov1Criteria.DefaultGroupSizeAccept;
import bnw.abm.intg.algov2.framework.models.*;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.templates.*;

import java.util.*;

/**
 * @author wniroshan 17 May 2018
 */
public class NewGroupBuilder {
    protected final AcceptanceCriterion groupSizeAcceptCriterion, globalDefaultAcceptance;
    protected final RejectionCriterion groupSizeRejectionCriterion;

    protected final Map<GroupType, Integer> currentGroupTypeCounts, expectedGroupTypeCounts;
    protected final Map<ReferenceAgentType, Integer> currentAgentTypeCounts, expectedAgentTypeCounts;
    protected final GroupTypeLogic groupTypeLogic;
    protected final IPFPTable seed;

    protected NewGroupBuilder(
            final Map<GroupType, Integer> groupTypeCounts,
            final Map<ReferenceAgentType, Integer> agentTypeCounts,
            final GroupTypeLogic groupTypeLogic,
            final IPFPTable seed) {
        this.groupTypeLogic = groupTypeLogic;
        this.seed = seed;

        this.expectedAgentTypeCounts = agentTypeCounts;
        this.expectedGroupTypeCounts = groupTypeCounts;

        this.currentAgentTypeCounts = new LinkedHashMap<>(agentTypeCounts);
        this.currentAgentTypeCounts.forEach((k, v) -> this.currentAgentTypeCounts.put(k, 0));
        this.currentGroupTypeCounts = new LinkedHashMap<>(groupTypeCounts);
        this.currentGroupTypeCounts.forEach((k, v) -> this.currentGroupTypeCounts.put(k, 0));

        groupSizeAcceptCriterion = new DefaultGroupSizeAccept();
        groupSizeAcceptCriterion.register();
        globalDefaultAcceptance = new DefaultAcceptanceCriterion();
        globalDefaultAcceptance.register();
        groupSizeRejectionCriterion = new Algov1Criteria.DefaultGroupSizeReject();
        groupSizeRejectionCriterion.register();
        new DefaultRejectionCriteria().register();
    }

    protected GroupTemplate.Member addMember(GroupTemplate groupTemplate,
                                             GroupTemplate.Member reference,
                                             LinkType linkType,
                                             TargetAgentType target) {
        GroupTemplate.Member newTarget = GroupTemplate.MembersPool.getNextMemberForGroup(groupTemplate, target);
        groupTemplate.put(reference, linkType, newTarget);
        ReferenceAgentType targetR = ReferenceAgentType.getExisting(target.getTypeID());
        int currentAgentCount = currentAgentTypeCounts.get(targetR);
        currentAgentTypeCounts.put(targetR, currentAgentCount + 1);
        return newTarget;
    }

    protected void discardGroup(GroupTemplate groupTemplate) {
        for (GroupTemplate.Member member : groupTemplate.getAllMembers()) {
            ReferenceAgentType memberR = ReferenceAgentType.getExisting(member.getTypeID());
            int existing = currentAgentTypeCounts.get(memberR);
            currentAgentTypeCounts.put(memberR, existing - 1);
        }
        groupTemplate.delete();
    }

    protected void removeMember(GroupTemplate groupTemplate, GroupTemplate.Member memberToRemove) {
        groupTemplate.remove(memberToRemove);
        ReferenceAgentType memberR = ReferenceAgentType.getExisting(memberToRemove.getTypeID());
        int existing = currentAgentTypeCounts.get(memberR);
        currentAgentTypeCounts.put(memberR, existing - 1);
    }

    protected <T> T get(Collection<T> collection, int index) {
        int i = 0;
        for (T item : collection) {
            if (i == index) {
                return item;
            }
            i++;
        }
        throw new IndexOutOfBoundsException("size: " + collection.size() + " index: " + index);
    }

    protected GroupTemplate newGroupTemplate(GroupType groupType, ReferenceAgentType agentType) {
        GroupTemplate newTemplate = new GroupTemplate(ReferenceAgentType.getExisting(agentType.getTypeID()));
        newTemplate.setGroupType(groupType);
        int currentAgentCount = currentAgentTypeCounts.get(agentType);
        currentAgentTypeCounts.put(agentType, currentAgentCount + 1);
        return newTemplate;
    }

    /**
     * Filters heads list based on the targetAgents distribution. If the target agents count is 0 the head is removed from the list.
     *
     * @param agentTypes The list of possible agent types
     * @param gType      The group type
     * @param <A>        ReferenceAgentType or TargetAgentType
     * @return The list of selected group head types
     */
    protected <A extends AgentType> List<A> getPossibleAgentTypesOnly(List<A> agentTypes, GroupType gType) {
        agentTypes.removeIf(a -> seed.get(gType, ReferenceAgentType.getExisting(a.getTypeID())) == 0);
        agentTypes.removeIf(a -> expectedAgentTypeCounts.get(ReferenceAgentType.getExisting(a.getTypeID())) == 0);
        return agentTypes;
    }
}
