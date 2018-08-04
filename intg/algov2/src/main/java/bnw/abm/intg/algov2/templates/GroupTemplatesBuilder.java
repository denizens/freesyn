package bnw.abm.intg.algov2.templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.GroupSnippet;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.MembersPool;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.LinkType.NONELinkType;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion.Accept;
import bnw.abm.intg.algov2.templates.GroupTypeLogic.GroupTypeLogicCaller;
import bnw.abm.intg.algov2.templates.RejectionCriterion.Reject;
import bnw.abm.intg.filemanager.obj.Writer;
import bnw.abm.intg.util.Log;

public class GroupTemplatesBuilder {

    private final int maxGroupSize;
    private final CombinationsBuilder combinationsBuilder;
    private static Map<String, Integer> totalTemplates = new HashMap<>(2);
    private final Writer writer;

    /**
     * Constructor
     * 
     * @param groupTypeLogic
     *            Implemented concrete instance of GroupTypeLogic with functionality on deciding GroupType of a given group template
     * @param maxGroupSize
     *            Maximum number of members in a group template
     * @param templatesOutFile
     *            File to store accepted group templates
     * @throws IOException
     *             When doing i/o operations to templatesOutFile
     */
    public GroupTemplatesBuilder(GroupTypeLogic groupTypeLogic, int maxGroupSize, Path templatesOutFile) throws IOException {
        GroupTypeLogicCaller.setGroupTypeLogic(groupTypeLogic);

        Files.deleteIfExists(templatesOutFile);
        Files.createDirectories(templatesOutFile.getParent());
        

        writer = Writer.createAndWriteToFile("START", templatesOutFile);

        this.maxGroupSize = maxGroupSize;
        this.combinationsBuilder = new CombinationsBuilder(LinkRulesWrapper.getLinkRules().getRelativeLocationsTable());
        new DefaultGroupSizeRejectionCriterion(maxGroupSize).register();
        new DefaultRejectionCriteria().register();
        new DefaultAcceptanceCriterion().register();
    }

    /**
     * Create all group templates using link rules and saves them to templatesOutputFile using agent types that appear from fromAgentTypeId to
     * toAgentTypeId in Link Rules
     * 
     * @param fromAgentTypeId
     *            starts creating group templates from this agent type
     * @param toAgentTypeId
     *            last agent type to use for group templates creation
     * @return number acceptable group templates
     * @throws IOException
     *             When writing constructed template objects to templatesOutFile
     */
    public int build(int fromAgentTypeId, int toAgentTypeId) throws IOException {
        for (ReferenceAgentType refAgentType : LinkRulesWrapper.rowKeySet()) {
            if (refAgentType.getTypeID() < fromAgentTypeId || toAgentTypeId < refAgentType.getTypeID()) {
                continue;
            }
            GroupTemplate basicTemplate = new GroupTemplate(refAgentType);
            Member refMemberAgentType = basicTemplate.getCurrentReferenceMembers().get(0);

            // NONE is a special LinkType. Agents only form NONE link type with agents of the same type. So we do that processing here in
            // a separate block.
            Optional<LinkType> noneLinkTypeOptional = LinkRulesWrapper.row(refAgentType).keySet().stream()
                    .filter(k -> k instanceof NONELinkType).findFirst();
            if (noneLinkTypeOptional.isPresent()) {
                int templatesCount = 0;

                templatesCount = getNONELinkGroupTemplates(refMemberAgentType, (NONELinkType) noneLinkTypeOptional.get(), basicTemplate);

                Log.info("GroupTemplates of AgentType (NONE LinkType) " + refMemberAgentType + ": count: " + templatesCount);

                addToTotalTemplates(templatesCount);

            } // End of NONE Processing

            // Now do normal link types and store the acceptable group templates
            int templatesCount = 0;
            templatesCount = createGroupTemplates(basicTemplate, new ArrayList<Member>(Arrays.asList(refMemberAgentType)));

            Log.info("GroupTemplates of AgentType " + refAgentType + ": count: " + templatesCount);

            addToTotalTemplates(templatesCount);
        }

        writer.appendToFile("END");
        Log.info("Group templates construction complete!");
        Log.info("All template objects written to " + writer.getFilePath());
        return totalTemplates.get("Total");

    }

    private static void addToTotalTemplates(int newCount) {
        synchronized (totalTemplates) {
            if (totalTemplates.containsKey("Total")) {
                int currCount = totalTemplates.get("Total");
                totalTemplates.put("Total", currCount + newCount);
            } else {
                totalTemplates.put("Total", newCount);
            }

        }
    }

    private int getNONELinkGroupTemplates(Member refMemberAgentType, NONELinkType noneLinkType, GroupTemplate basicTemplate) throws IOException {

        List<GroupTemplate> tempAcceptedList = null;
        if (noneLinkType.isActive()) {// Only use active links, ignore passive links

            if (Log.isDebugEnabled())
                Log.debug("base: " + basicTemplate + " refs: " + basicTemplate.getCurrentReferenceMembers());

            tempAcceptedList = new ArrayList<>(noneLinkType.getMaxLinks());
            List<Triple<Member, LinkType, List<GroupSnippet>>> targetCombinations = getTargetAgentTypeCombinations(basicTemplate);
            targetCombinations = targetCombinations.stream().filter(comb -> comb.getMiddle() instanceof NONELinkType)
                    .collect(Collectors.toList());
            GroupTemplate newTemplate = null;
            if (!targetCombinations.isEmpty()) {
                for (GroupSnippet targetCombination : targetCombinations.get(0).getRight()) {
                    newTemplate = basicTemplate.copy();
                    List<Member> newTargetMembers = MembersPool.getNextMembersForGroup(newTemplate, targetCombination);
                    newTemplate.putAll(refMemberAgentType, noneLinkType, newTargetMembers);
                    if (RejectionCriterion.validateAll(newTemplate) == Reject.YES) {
                        continue;
                    }
                    if (AcceptanceCriterion.validateAll(newTemplate) == Accept.YES) {
                        newTemplate.setGroupType(GroupTypeLogicCaller.computeGroupType(newTemplate));
                        tempAcceptedList.add(newTemplate);
                        // System.out.println(newTemplate); //TODO: remove this
                    }
                }
            } else {
                Log.warn(refMemberAgentType + " did not have any taget combinations with NONE LinkType. Moving on..");
            }
            if (tempAcceptedList != null) {
                if (Log.isDebugEnabled()) {
                    tempAcceptedList.stream().forEach(grp -> Log.debug(grp.toStringFullMode()));
                }
                writer.appendToFile(tempAcceptedList);
            }
        }

        return tempAcceptedList.size();
    }

    /**
     * Considers all combinations each reference agent can form and creates group templates with them
     * 
     * @param baseConfig
     *            Starting group configuration
     * @param currentRefAgents
     *            the reference agents
     * @return number of List<GroupTemplate> objects written to templatesOutputFile
     * @throws IOException
     *             on failing to write templates to templatesOutputFile
     */
    private int createGroupTemplates(GroupTemplate baseConfig, List<Member> currentRefAgents) throws IOException {
        baseConfig.setNewReferenceMembers(currentRefAgents);
        List<GroupTemplate> acceptedTemplates = new ArrayList<>();
        List<CombinationsOnDemand> combinationModules = new ArrayList<>();
        // get all combinations of target agents of each reference agent
        List<Triple<Member, LinkType, List<GroupSnippet>>> groupSnippetTuples;
        groupSnippetTuples = getTargetAgentTypeCombinations(baseConfig);
        combinationModules.add(new CombinationsOnDemand(groupSnippetTuples, baseConfig, maxGroupSize - baseConfig.size()));

        int templatesCount = 0;
        while (!combinationModules.isEmpty()) {
            CombinationsOnDemand combinationsBuilder = combinationModules.remove(0);
            acceptedTemplates = new ArrayList<>();

            if (Log.isDebugEnabled())
                Log.debug("base: " + combinationsBuilder.getBaseConfig() + " refs: "
                        + combinationsBuilder.getBaseConfig().getCurrentReferenceMembers());

            while (combinationsBuilder.hasNext()) {
                GroupTemplate baseTemplate = combinationsBuilder.next();

                if (RejectionCriterion.validateAll(baseTemplate) == Reject.YES) {
                    continue;
                }

                if (AcceptanceCriterion.validateAll(baseTemplate) == Accept.YES) {
                    baseTemplate.setGroupType(GroupTypeLogicCaller.computeGroupType(baseTemplate));
                    acceptedTemplates.add(baseTemplate);
                }

                // Update each templates agents that can be use as reference agent for next extension of this template
                List<Member> nextRefAgents = getAllAdjacentMembers(baseTemplate, baseTemplate.getCurrentReferenceMembers());
                baseTemplate.setNewReferenceMembers(nextRefAgents);

                if (!InternalValidate.possibleToComplete(baseTemplate, maxGroupSize)) {
                    continue;
                }

                // get all combinations of target agents of each reference agent
                groupSnippetTuples = getTargetAgentTypeCombinations(baseTemplate);
                combinationModules.add(new CombinationsOnDemand(groupSnippetTuples, baseTemplate, maxGroupSize - baseTemplate.size()));
            }
            if (!acceptedTemplates.isEmpty()) {
                if (Log.isDebugEnabled()) {
                    acceptedTemplates.stream().forEach(grp -> Log.debug(grp.toStringFullMode()));
                }
                writer.appendToFile(acceptedTemplates);
                templatesCount += acceptedTemplates.size();
            }
        }
        return templatesCount;
    }

    /**
     * Gets the list of Member that were added to the group template after a given set of agents
     * 
     * @param groupTemplate
     *            The group template
     * @param currentRefAgents
     *            Set of agents whom to be considered as reference agents
     * @return List of agents added to the group just after given list of reference agents
     */
    private List<Member> getAllAdjacentMembers(GroupTemplate groupTemplate, List<Member> currentRefAgents) {
        List<Member> adjacentList = new ArrayList<>();

        // Get the list of lists containing members (child nodes) of each referenceAgent and then flat the 'list of list' to a one large list
        for (Member m : currentRefAgents) {
            adjacentList.addAll(groupTemplate.getAdjacentMembers(m).values().stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
        }

        // Above list contains previous reference agents also. We need to remove them otherwise program goes into a never ending loop and fails
        // giving a Stack Overflow Error. GroupTemplate.getAllMembers() returns a LinkedHashSet which had preserved the order that agents were
        // added to the group. Any Member instance that has a higher index than instances in currentRefAgents is guaranteed not to be one of the
        // previously considered reference agents.
        List<Integer> indices = new ArrayList<>(3);
        int index = 0;
        for (Member m : currentRefAgents) {
            index = 0;
            for (Member currMem : groupTemplate.getAllMembers()) {
                if (m == currMem) {
                    indices.add(index);
                    break;
                }
                index++;
            }
        }
        int maxRef = Collections.max(indices); // Index of the last reference agent

        // Now get all the agents that were added to group after last reference agents.
        Set<Member> newReferenceMembers = new HashSet<>(groupTemplate.getAllMembers().size() - currentRefAgents.size());// 3
        for (Member m : adjacentList) {
            index = 0;
            for (Member curMem : groupTemplate.getAllMembers()) {
                if (m == curMem && index > maxRef) {
                    newReferenceMembers.add(m);
                    break;
                }
                index++;
            }
        }

        return new ArrayList<>(newReferenceMembers);
    }

    /**
     * This function finds combinations that agent types in a given list can form. This considers that there can be multiple agents of same type
     * in one group. We specify referenceAgentType as the parameter. Internally function selects the list of target agent types the reference can
     * form links with and computes different combinations.
     * 
     * @param refAgentTypes
     *            reference agent type which we want target agent combinations
     * @return List of Triples with reference agent type, links types it can form and different combinations of target agent types under each
     *         link type. Combinations are ordered ascending by combination size.
     */
    private List<Triple<Member, LinkType, List<GroupSnippet>>> getTargetAgentTypeCombinations(GroupTemplate baseConfig) {
        List<Member> refAgentTypes = baseConfig.getCurrentReferenceMembers();
        int emptySlotsInGroup = maxGroupSize - baseConfig.size();

        ArrayList<Triple<Member, LinkType, List<GroupSnippet>>> groupSnippets = new ArrayList<>();
        for (Member refType : refAgentTypes) {
            for (LinkType linkType : LinkRulesWrapper.row(ReferenceAgentType.getExisting(refType.getTypeID())).keySet()) {
                if (!linkType.isActive()) { // Form links only with active links
                    continue;
                }
                int existingMembers = baseConfig.getAdjacentMembers(refType, linkType).size();
                int nofLinksRefCanForm = linkType.getMaxLinks() - existingMembers;
                int maxCombinationSize = emptySlotsInGroup > nofLinksRefCanForm ? nofLinksRefCanForm : emptySlotsInGroup;

                List<GroupSnippet> targetCombinationsOfReference = null;
                // Now we can get combinations

                targetCombinationsOfReference = this.combinationsBuilder.combinationsWithRepetition(refType, linkType, maxCombinationSize);
                if (linkType.getMinLinks() == 0) {
                    // If link allows 0 connections then add an empty Snippet
                    targetCombinationsOfReference.add(0, new GroupSnippet());
                } else if (existingMembers >= linkType.getMinLinks()) {
                    targetCombinationsOfReference.add(0, new GroupSnippet());
                } else {
                    // If there are combinations smaller than minimum link count, remove them
                    targetCombinationsOfReference.removeIf(c -> c.size() < linkType.getMinLinks());
                }
                groupSnippets.add(Triple.of(refType, linkType, targetCombinationsOfReference));
            }
        }
        groupSnippets.trimToSize();
        return groupSnippets;
    }

    static class InternalValidate {
        static boolean possibleToComplete(GroupTemplate groupTemplate, int maxGroupSize) {
            if (groupTemplate.getCurrentReferenceMembers().isEmpty()) {
                return false;
            }
            for (Member member : groupTemplate.getCurrentReferenceMembers()) {
                for (LinkType linkType : LinkRulesWrapper.row(ReferenceAgentType.getExisting(member.getTypeID())).keySet()) {
                    int existingLinks = groupTemplate.getAdjacentMembers(member, linkType).size();
                    int requiredButUnformedLinks = (linkType.getMinLinks() > existingLinks) ? (linkType.getMinLinks() - existingLinks) : 0;
                    if ((maxGroupSize - groupTemplate.size()) < requiredButUnformedLinks) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
