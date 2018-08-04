package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.GroupSnippet;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkType.NONELinkType;

public class CombinationsOnDemand {

    private int maxSelectionSize;
    private final Map<Triple<Member, LinkType, List<GroupSnippet>>, Integer> lastIterIndex;
    private final GroupTemplate baseConfig;
    private boolean hasNext = false, computeAgain = true;
    private GroupTemplate currentCombination = null;

    /**
     * This forms GroupTemplate combinations by taking target agent combinations. GroupTemplates are generated on request rather than producing
     * all at once. hasNext() returns boolean value indicating if there is a combination available. next() returns the next available
     * combination.
     * 
     * @param groupSnippetTuples
     *            Combinations of TargetAgentTypes grouped by reference agent type and it's link type
     * @param baseConfig
     *            starting GroupTemplate combination
     * @param maxSelectionSize
     *            max extra agents to be added to baseConfig
     */
    public CombinationsOnDemand(List<Triple<Member, LinkType, List<GroupSnippet>>> groupSnippetTuples, GroupTemplate baseConfig,
            int maxSelectionSize) {
        this.maxSelectionSize = maxSelectionSize;
        this.baseConfig = baseConfig.copy();

        // Remove NONELinkType tuple, because it is a special type that is handled separately
        groupSnippetTuples.removeIf(t -> t.getMiddle() instanceof NONELinkType);
        groupSnippetTuples.removeIf(t -> t.getRight().isEmpty());
        lastIterIndex = new LinkedHashMap<>(groupSnippetTuples.size());
        for (Triple<Member, LinkType, List<GroupSnippet>> snippetTuple : groupSnippetTuples) {
            this.lastIterIndex.put(snippetTuple, 0);
        }

        this.computeAgain = true;
        this.currentCombination = computeNextGroupTemplate();
        if (this.currentCombination != null && this.currentCombination.equals(baseConfig) && this.hasNext()) {
            this.next();
        }

    }

    /**
     * Tells whether another GroupTemplate combination is available
     * 
     * @return true if there is another combination, else false
     */
    public boolean hasNext() {
        return hasNext;
    }

    public GroupTemplate next() {
        GroupTemplate returnThis = null;
        if (hasNext) {
            returnThis = this.currentCombination;
        } else {
            throw new UnsupportedOperationException("No more combinations");
        }

        if (computeAgain) {
            this.currentCombination = computeNextGroupTemplate();
        } else {
            hasNext = false; // If we don't compute again we don't have a new next combination
        }
        return returnThis;
    }

    /**
     * Gets next available GroupTemplate combination. Throws UnsupportedOperationException if there is no combination available.
     * 
     * @return Next GroupTemplate
     */
    private GroupTemplate computeNextGroupTemplate() {
        if (this.lastIterIndex.keySet().isEmpty()) {
            computeAgain = false;
            hasNext = false;
            return null;
        }
        List<Triple<Member, LinkType, List<GroupSnippet>>> groupSnippetTuples = new ArrayList<>(this.lastIterIndex.keySet());

        CombinationParts combinationParts;
        int indexSum = 0;
        do {
            int index = 0;
            Member ref = groupSnippetTuples.get(index).getLeft();
            LinkType link = groupSnippetTuples.get(index).getMiddle();
            // Log.info(ref.toString()+":"+link.toString()+":"+groupSnippetTuples.get(index).getRight().toString());
            GroupSnippet snippet = groupSnippetTuples.get(index).getRight().get(lastIterIndex.get(groupSnippetTuples.get(index)));
            combinationParts = new CombinationParts(groupSnippetTuples.size());
            combinationParts.add(Triple.of(ref, link, snippet));

            index++;
            while (index < groupSnippetTuples.size()) {
                Triple<Member, LinkType, List<GroupSnippet>> nextList = groupSnippetTuples.get(index);
                ref = nextList.getLeft();
                link = nextList.getMiddle();
                GroupSnippet nextCombinationPart = nextList.getRight().get(lastIterIndex.get(nextList));
                Triple<Member, LinkType, GroupSnippet> toAdd = Triple.of(ref, link, nextCombinationPart);
                combinationParts.add(toAdd);
                if (combinationParts.size() > maxSelectionSize) {
                    // Can't form anymore. Moving index counter of this tuple and every triple that comes after this to end of the GroupSnippets
                    // list, so below for-loop will set them to 0.
                    // This will also cause below for-loop to increment index counter of previous tuple by 1. So we can start forming a new
                    // combination
                    lastIterIndex.put(nextList, nextList.getRight().size() - 1);
                }
                index++;
            }
            // After above while loop update iterator indices for next combination
            for (int i = groupSnippetTuples.size() - 1; i >= 0; i--) {
                Triple<Member, LinkType, List<GroupSnippet>> endTuple = groupSnippetTuples.get(i);
                int nextIterIndex = lastIterIndex.get(endTuple) + 1;
                if (nextIterIndex == endTuple.getRight().size()) {
                    nextIterIndex = 0;
                    lastIterIndex.put(endTuple, nextIterIndex);
                } else {
                    lastIterIndex.put(endTuple, nextIterIndex);
                    break;
                }
            }
            indexSum = lastIterIndex.values().stream().mapToInt(i -> i).sum();

        } while (indexSum > 0 && combinationParts.size() > maxSelectionSize);

        if (lastIterIndex.values().stream().mapToInt(i -> i).sum() == 0) {
            computeAgain = false;
            if (combinationParts.size() <= maxSelectionSize) {
                hasNext = true;
            } else {
                hasNext = false;
            }
        } else {
            computeAgain = true;
            if (combinationParts.size() <= maxSelectionSize) {
                hasNext = true;
            } else {
                // do not have a new combination: Impossible - this situation prevented by do-while loop
            }
        }

        GroupTemplate group = null;
        if (hasNext) {
            group = baseConfig.copy();
            for (Triple<Member, LinkType, GroupSnippet> part : combinationParts) {
                group.putAll(part.getLeft(), part.getMiddle(), part.getRight());
            }
        }
        return group;
    }

    final class CombinationParts extends ArrayList<Triple<Member, LinkType, GroupSnippet>> {

        /**
         * 
         */
        private static final long serialVersionUID = 1180196388110166252L;
        private int size = 0;

        CombinationParts(int size) {
            super(size);
        }

        private void computesize() {
            size = 0;
            for (Triple<Member, LinkType, GroupSnippet> tuple : this) {
                size += tuple.getRight().size();
            }
        }

        @Override
        public boolean add(Triple<Member, LinkType, GroupSnippet> e) {
            boolean addWorked = super.add(e);
            this.computesize();
            return addWorked;
        }

        @Override
        public int size() {
            return this.size;
        }

    }

    /**
     * Gets the base template used for this combinations builder
     * 
     * @return base template
     */
    public GroupTemplate getBaseConfig() {
        return this.baseConfig.copy();

    }
}
