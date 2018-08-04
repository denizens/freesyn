package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkType.NONELinkType;
import bnw.abm.intg.algov2.templates.LinkCondition;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class GroupTemplate extends ForwardingTable<Member, LinkType, List<Member>> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -7017471665807350916L;
    private transient static Long globalTemplateIdCounter = 0L;

    // Table based on LinkedHashMap
    private Table<Member, LinkType, List<Member>> adjacencyList;
    private LinkedHashSet<Member> allMembers;
    private transient List<Member> currentRefs;

    private GroupType groupType = null;
    private final long templateId;

    /**
     * This is a map of member.type and index of last member of this type. index of last member of this type is always the largest. Map
     * Key:Member.type Value:Last member instance id
     */
    private transient Map<Integer, Integer> memberIDsMap;

    @Override
    protected Table<Member, LinkType, List<Member>> delegate() {
        return adjacencyList;
    }

    /**
     * Creates a new instance of a GroupTemplate
     * 
     * @param referenceMember
     *            Main reference Agent
     */
    public GroupTemplate(ReferenceAgentType referenceMember) {
        adjacencyList = HashBasedTable.create(6, 4);
        // adjacencyList = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
        allMembers = new LinkedHashSet<>(8);
        memberIDsMap = new ConcurrentHashMap<>(5);
        Member firstMember = MembersPool.getNextMemberForGroup(this, referenceMember);
        synchronized (globalTemplateIdCounter) {
            this.templateId = globalTemplateIdCounter;
            globalTemplateIdCounter++;
            allMembers.add(firstMember);
        }
        this.currentRefs = new ArrayList<>(Arrays.asList(firstMember));
        memberIDsMap.put(referenceMember.getTypeID(), firstMember.getInstanceID());
    }

    /**
     * Used for copy method
     * 
     * @param groupHeadType
     *            Main reference Agent
     * 
     * @param currentReferenceMembers
     *            current reference agent types
     */
    private GroupTemplate(GroupTemplate templateToCopy) {
        adjacencyList = HashBasedTable.create();
        allMembers = new LinkedHashSet<>(templateToCopy.allMembers);
        memberIDsMap = new ConcurrentHashMap<>(templateToCopy.memberIDsMap);

        synchronized (globalTemplateIdCounter) {
            this.templateId = globalTemplateIdCounter;
            globalTemplateIdCounter++;
        }

        for (Cell<Member, LinkType, List<Member>> entry : templateToCopy.adjacencyList.cellSet()) {
            // Must create new ArrayList
            this.adjacencyList.put(entry.getRowKey(), entry.getColumnKey(), new ArrayList<>(entry.getValue()));
        }

        this.currentRefs = new ArrayList<>(templateToCopy.getCurrentReferenceMembers());
    }

    public boolean equals(GroupTemplate template) {

        List<Member> templateMembers = new ArrayList<>(template.getAllMembers());
        List<Member> thisMembers = new ArrayList<>(this.allMembers);

        if (template instanceof GroupTemplate) {
            if (this.size() == template.size()) {
                while (!templateMembers.isEmpty()) {
                    Member templateMemb = templateMembers.remove(0);
                    for (int i = 0; i < thisMembers.size(); i++) {
                        Member thisMember = thisMembers.get(i);
                        boolean matched = false;
                        if (templateMemb.isSameType(thisMember)) {
                            Map<LinkType, List<Member>> thisMembRels = this.getAdjacentMembers(thisMember);
                            for (LinkType thisMembLink : thisMembRels.keySet()) {
                                List<Member> thisMembRelsOfLink = thisMembRels.get(thisMembLink);
                                List<Member> templateMembRelsOfLink = template.getAdjacentMembers(templateMemb, thisMembLink);
                                if (thisMembRelsOfLink.size() == templateMembRelsOfLink.size()) {
                                    List<Member> templateRelsCopy = new ArrayList<>(templateMembRelsOfLink);
                                    for (Member thisRel : thisMembRelsOfLink) {
                                        for (int j = 0; j < templateRelsCopy.size(); j++) {
                                            Member templateRel = templateRelsCopy.get(j);
                                            if (thisRel.isSameType(templateRel)) {
                                                templateRelsCopy.remove(templateRel);
                                                break;
                                            }
                                        }
                                    }
                                    if (templateRelsCopy.isEmpty()) {
                                        matched = true;
                                    } else {
                                        matched = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (matched) {
                            thisMembers.remove(thisMember);
                            i--;
                        }
                    }
                }
            }
        }
        if (thisMembers.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public int getFirstMatch(List<GroupTemplate> templates) {
        for (int i = 0; i < templates.size(); i++) {
            if (this.equals(templates.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Creates a duplicate instance of this GroupTemplate
     * 
     * @return duplicate instance of GroupTemplate
     */
    public GroupTemplate copy() {
        GroupTemplate newCopy = new GroupTemplate(this);
        return newCopy;
    }

    public void setNewReferenceMembers(List<Member> refs) {
        this.currentRefs = refs;
    }

    public List<Member> getCurrentReferenceMembers() {
        return this.currentRefs;
    }

    private Map<Integer, Integer> getLastUsedIDsMap() {
        return this.memberIDsMap;
    }

    /**
     * Unique ID of this group template
     * 
     * @return ID
     */
    public long getID() {
        return this.templateId;
    }

    /**
     * Adds a new member to this group template
     * 
     * @param reference
     *            immediate reference agent type of the new member type
     * @param linkType
     *            type of the link between immediate reference agent type and newly added member type
     * @param target
     *            new member type
     */
    public void put(Member reference, LinkType linkType, Member target) {
        putAll(reference, linkType, Arrays.asList(target));
    }

    public void putAll(Member reference, LinkType linkType, GroupSnippet targetSnippets) {
        synchronized (this.memberIDsMap) {
            List<Member> targets = MembersPool.getNextMembersForGroup(this, targetSnippets);
            this.putAll(reference, linkType, targets);
        }
    }

    /**
     * Adds a list of new members to this group template
     * 
     * @param reference
     *            immediate reference agent type of the new list of member types
     * @param linkType
     *            type of the link between immediate reference agent type and newly added member types list
     * @param targets
     *            new member types list
     */
    public void putAll(Member reference, LinkType linkType, List<Member> targets) {

        List<Member> actualNewTargets = null;

        if (adjacencyList.contains(reference, linkType)) {
            List<Member> currTargets = adjacencyList.get(reference, linkType);
            // We don't want to add items already in the adjacency list. See if there are any actually new items
            actualNewTargets = new ArrayList<>((int) (allMembers.size() * 0.5));
            for (Member t : targets) {
                if (!currTargets.contains(t)) {
                    actualNewTargets.add(t);
                }
                ((ArrayList<Member>) actualNewTargets).trimToSize();
            }
            if (!actualNewTargets.isEmpty()) { // Append the new agents to the group template
                currTargets.addAll(actualNewTargets);
            }
        } else {
            if (targets.isEmpty() && linkType.getMinLinks() != 0 && (linkType instanceof NONELinkType == false)) {
                throw new UnsupportedOperationException("LinkType " + linkType + " cannot have empty relationships");
            }
            try {
                adjacencyList.put(reference, linkType, new ArrayList<Member>(targets));
            } catch (Exception e) {
                Log.errorAndExit(this + " ref: " + reference + " link type: " + linkType + " members: " + targets, e, EXITCODE.PROGERROR);
            }
            actualNewTargets = targets;
        }

        // (!actualNewTargets.isEmpty()) == True: There are actually new entries. So we need to validate
        // targets.isEmpty() == True: This means new reference Member can have no relatives (e.g. [Snc].none.[]). We need to validate it
        if (!actualNewTargets.isEmpty() || targets.isEmpty()) {
            allMembers.add(reference);
            allMembers.addAll(actualNewTargets);
            LinkCondition.applyAll(this, reference, linkType, actualNewTargets);
        }
    }

    public void remove(Member member) {
        allMembers.remove(member);
        currentRefs.remove(member);
        int lastHighestId = -1;
        Table<Member, LinkType, List<Member>> revertedAdjacencyList = HashBasedTable.create(6, 4);
        for (Cell<Member, LinkType, List<Member>> entry : this.adjacencyList.cellSet()) {
            if (entry.getRowKey() == member) {
                continue;
            }
            if (entry.getRowKey().isSameType(member) && entry.getRowKey().getInstanceID() > lastHighestId) {
                lastHighestId = entry.getRowKey().getInstanceID();
            }
            List<Member> newValuesList = new ArrayList<>(entry.getValue());
            newValuesList.remove(member);
            revertedAdjacencyList.put(entry.getRowKey(), entry.getColumnKey(), newValuesList);
        }

        memberIDsMap.put(member.getTypeID(), lastHighestId);

        this.adjacencyList = revertedAdjacencyList;
    }

    public void delete() {
        allMembers.removeAll(allMembers);
        currentRefs = null;
        adjacencyList = null;
        memberIDsMap = null;
        groupType = null;
    }

    /**
     * Gets a view of the adjacent agents of referenceAgentType instance. Changes to Map structure will not reflect in the original table. But
     * changes to map elements will change original instances.
     * 
     * @param referenceAgentType
     *            Of whom we want to get adjacent agent types
     * @return Map of reference agent type's adjacent members by link type
     */
    public Map<LinkType, List<Member>> getAdjacentMembers(Member referenceAgentType) {
        return new LinkedHashMap<>(adjacencyList.row(referenceAgentType));
    }

    /**
     * Returns a new view adjacent agents of reference agent type under given link type. Changes to ArrayList will not reflect in the original
     * table. But changes to individual elements will change original instances.
     * 
     * @param reference
     *            The reference agent type
     * @param linkType
     *            The link type of the reference agent0
     * @return The reference agent's adjacent members
     */
    public List<Member> getAdjacentMembers(Member reference, LinkType linkType) {
        List<Member> out = adjacencyList.get(reference, linkType);
        if (out == null)
            return new ArrayList<>(0);
        else
            return new ArrayList<>(adjacencyList.get(reference, linkType));
    }

    /**
     * Type of this group, computed according to instructions provided by overriding computeGroupType() method in GroupTypeLogic class
     * 
     * @return GroupType of this group template
     */
    public GroupType getGroupType() {
        return this.groupType;
    }

    public void setGroupType(GroupType newGroupType) {
        this.groupType = newGroupType;
    }

    /**
     * Returns all the members in the group template. Any changes to this will alter group template
     * 
     * @return LinkedHashSet of members currently in the group template
     */
    public Collection<Member> getAllMembers() {
        return this.allMembers;
    }

    @Override
    public String toString() {
        return allMembers.toString();
    }

    public String toStringFullMode() {
        if (this.size() == 1)
            return "{" + allMembers.toArray()[0] + "}";
        else
            return adjacencyList.toString();
    }

    public String json() throws JsonProcessingException {
        Object outObject = null;
        if (this.size() == 1)
            outObject = allMembers;
        else
            outObject = adjacencyList.rowMap();
        return new ObjectMapper().writeValueAsString(outObject);
    }

    /**
     * Returns the size of allMembers LinkedHashSet
     * 
     * @return number of members
     */
    public int size() {
        return allMembers.size();
    }

    /**
     * Member instances are created when adding agent types to GroupTemplates. We cannot use original AgentType instance because if we use that
     * map will always point to same object.
     * 
     * @author Bhagya N. Wickramasinghe
     *
     */
    public static class Member extends AgentType {

        /**
         * 
         */
        private static final long serialVersionUID = 3098768812241078363L;
        private int agentId;

        private Member(AgentType agentTypeToBeDecorated, int agentId) {
            super(agentTypeToBeDecorated.getTypeID());
            this.agentId = agentId;
        }

        /**
         * Returns unique instance ID.
         * 
         * @return agent id
         */
        public int getInstanceID() {
            return this.agentId;
        }

        public String getIdentifier() {
            return super.getTypeID() + "." + this.agentId;
        }

        @Override
        public String toString() {
            if (Log.isTraceEnabled()) {
                return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
            } else {
                return getIdentifier();
            }
        }
    }

    public static class GroupSnippet extends ArrayList<AgentType> {
        /**
         * 
         */
        private static final long serialVersionUID = -1448337731852104646L;

        GroupSnippet(List<AgentType> members) {
            super(members);
        }

        public GroupSnippet() {
        }
    }

    /**
     * This class is for creating Member instances for GroupTemplates. We maintain a pool of member instances. Uniqueness of agent instances is
     * only important within a GroupTemplate. Reason for maintaining a pool is creating too many member instances requires large amount of
     * memory. At one point even 120GB of RAM was not enough. Hence this special approach for creating Member instances.
     * 
     * @author Bhagya N. Wickramasinghe
     *
     */
    public static class MembersPool {
        // Map<AgentType.Type,List_of_Member_Instances>
        private static Map<Integer, List<Member>> membersPool = new ConcurrentHashMap<>();

        public static Member getNextMemberForGroup(GroupTemplate groupTemplate, AgentType agentType) {
            // Map<AgentType.Type, BiggestAgentInstanceID>
            Map<Integer, Integer> lastUsedIDMap = groupTemplate.getLastUsedIDsMap();
            int nextInstanceID = lastUsedIDMap.get(agentType.getTypeID()) == null ? 0 : lastUsedIDMap.get(agentType.getTypeID()) + 1;

            if (membersPool.containsKey(agentType.getTypeID())) {
                List<Member> membersPoolOfSelectedType = membersPool.get(agentType.getTypeID());
                if (membersPoolOfSelectedType.size() > nextInstanceID) {
                    lastUsedIDMap.put(agentType.getTypeID(), nextInstanceID);
                    return membersPoolOfSelectedType.get(nextInstanceID);
                } else {
                    Member newMember = new Member(agentType, nextInstanceID);
                    membersPoolOfSelectedType.add(newMember);
                    lastUsedIDMap.put(newMember.getTypeID(), newMember.getInstanceID());
                    return newMember;
                }
            } else {
                Member member = new Member(agentType, nextInstanceID);
                membersPool.put(agentType.getTypeID(), new ArrayList<>(Arrays.asList(member)));
                lastUsedIDMap.put(member.getTypeID(), member.getInstanceID());
                return member;
            }
        }

        public static List<Member> getNextMembersForGroup(GroupTemplate groupTemplate, List<AgentType> agentTypes) {
            List<Member> newMembers = new ArrayList<>(agentTypes.size());
            for (AgentType agentType : agentTypes) {
                newMembers.add(getNextMemberForGroup(groupTemplate, agentType));
            }
            return newMembers;
        }
    }
}
