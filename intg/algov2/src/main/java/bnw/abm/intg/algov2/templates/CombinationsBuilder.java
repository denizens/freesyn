package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.CombinatoricsUtils;

import bnw.abm.intg.algov2.framework.models.AgentType;
import bnw.abm.intg.algov2.framework.models.LinkRulesRelative;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.GroupSnippet;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class CombinationsBuilder {

    private final LinkRulesRelative linkRulesRelative;
    private static Table<String, Integer, List<Snippet>> combinationsMap;

    public CombinationsBuilder(LinkRulesRelative linkRulesRelative) {
        this.linkRulesRelative = linkRulesRelative;
        combinationsMap = HashBasedTable.create();

    }

    /**
     * Computes all combinations by selecting 1 to k elements from TargetAgentTypes with repetition. Maximum k is given by maxSelectionSize.
     * 
     * @param refType
     *            reference agent type
     * @param linkType
     *            link of which possible target agent combinations to find
     * @param maxSelectionSize
     *            Maximum size of combinations (k).
     * @return List of different combinations ordered by Snippet size. (Snippet is simply a list of Member AgentTypes)
     */
    public List<GroupSnippet> combinationsWithRepetition(Member refType, LinkType linkType, int maxSelectionSize) {

        List<Integer> targets = linkRulesRelative.get(refType, linkType);

        // Total combinations is given by C(n+k,k) - including empty combination
        List<Snippet> allCombinations = new ArrayList<>(
                (maxSelectionSize <= 0) ? 0 : (int) CombinatoricsUtils.binomialCoefficient(targets.size() + maxSelectionSize, maxSelectionSize));

        int lastCompletedSelectionSize = 0;
        String combinationMapKey = convertToCombinationMapKey(targets);

        if (combinationsMap.containsRow(combinationMapKey)) {
            for (int i = 1; i <= maxSelectionSize; i++) {
                if (combinationsMap.contains(combinationMapKey, i)) {
                    allCombinations.addAll(combinationsMap.get(combinationMapKey, i));
                    lastCompletedSelectionSize = i;
                } else {
                    break;
                }
            }
        }

        if (lastCompletedSelectionSize < maxSelectionSize) {
            // Log.info(allCombinations + "");
            int nextSelectionSize = lastCompletedSelectionSize + 1;
            allCombinations.add(0, new Snippet());// Adding empty combination as first combination - required for below logic

            List<Integer> offset = new ArrayList<>(targets.size());
            if (lastCompletedSelectionSize == 0) {
                for (int remainingTypes = targets.size(); remainingTypes > 0; remainingTypes--) {
                    offset.add(0);
                }
            } else {
                int n, q, p;
                for (int remainingTypes = targets.size(); remainingTypes > 0; remainingTypes--) {
                    n = remainingTypes + lastCompletedSelectionSize - 1;
                    if (n > lastCompletedSelectionSize) {
                        // Log.info(refType + " " + linkType + " " + targets);
                        // Log.info(maxSelectionSize + " " + n + " " + lastCompletedSelectionSize + "");
                        q = (int) CombinatoricsUtils.binomialCoefficient(n, lastCompletedSelectionSize);

                        p = (int) CombinatoricsUtils.binomialCoefficient(n - 1, lastCompletedSelectionSize);
                        // Log.info(p + "");
                        offset.add(q - p);
                    } else {
                        offset.add(1);
                    }
                }
            }

            int start;
            if (nextSelectionSize == 1) {
                start = 0;
            } else {
                start = (int) CombinatoricsUtils.binomialCoefficient(targets.size() + lastCompletedSelectionSize - 1,
                        lastCompletedSelectionSize - 1);
            }

            int a = 0;
            int listSize = start;
            List<Snippet> tempCombinations;
            for (int selecSize = nextSelectionSize; selecSize <= maxSelectionSize; selecSize++) {
                start = listSize;
                listSize = allCombinations.size();
                // Create a list large enough to hold the combinations formed in this iteration
                if (targets.size() + selecSize - 1 >= selecSize) {
                    tempCombinations = new ArrayList<>((int) CombinatoricsUtils.binomialCoefficient(targets.size() + selecSize - 1, selecSize));
                } else {
                    Log.warn("Reference: " + refType + ", LinkType: " + linkType + ", Targets: " + targets
                            + " - Cannot select " + selecSize + " agent type(s) from Targets. Treating as an empty selection.");
                    tempCombinations = new ArrayList<>(0);
                }
                for (int i = 0; i < targets.size(); i++) {
                    a = 0;
                    for (int combI = start; combI < listSize; combI++, a++) {
                        Snippet newComb = new Snippet(allCombinations.get(combI));
                        newComb.add(targets.get(i));
                        tempCombinations.add(newComb);
                    }
                    start += offset.get(i);
                    offset.set(i, a);
                }
                combinationsMap.put(combinationMapKey, selecSize, tempCombinations);
                allCombinations.addAll(tempCombinations);
            }
            allCombinations.remove(0); // Remove the previously added empty combination
        }

        ((ArrayList<Snippet>) allCombinations).trimToSize();

        List<GroupSnippet> allGroupSnippets = new ArrayList<>(allCombinations.size());
        for (Snippet snippet : allCombinations) {
            GroupSnippet groupSnippet = new GroupSnippet();
            for (int relativeLoc : snippet) {
                ReferenceAgentType correspondingRef = ReferenceAgentType.getInstance(refType.getTypeID());
                groupSnippet.add(getTargetAgentType(correspondingRef, linkType, relativeLoc + refType.getTypeID()));
            }
            allGroupSnippets.add(groupSnippet);
        }
        return allGroupSnippets;
    }

    /**
     * Get combinations of a size
     * 
     * @param refType
     *            reference agent
     * @param linkType
     *            link reference agent will form
     * @param selectionSize
     *            combination size
     * @return List of combination group snippets
     */
    public List<GroupSnippet> combinationsOfSizeWithRepetition(Member refType, LinkType linkType, int selectionSize) {
        List<Integer> targets = linkRulesRelative.get(refType, linkType);
        //
        // Total combinations is given by C(n+k,k) - including empty combination
        List<Snippet> selectedCombinations;
        String combinationMapKey = convertToCombinationMapKey(targets);

        if (combinationsMap.contains(combinationMapKey, selectionSize)) {
            selectedCombinations = combinationsMap.get(combinationMapKey, selectionSize);
        } else {
            this.combinationsWithRepetition(refType, linkType, selectionSize);
            selectedCombinations = combinationsMap.get(combinationMapKey, selectionSize);
        }
        List<GroupSnippet> allGroupSnippets = new ArrayList<>(selectedCombinations.size());
        for (Snippet snippet : selectedCombinations) {
            GroupSnippet groupSnippet = new GroupSnippet();
            for (int relativeLoc : snippet) {
                ReferenceAgentType correspondingRef = ReferenceAgentType.getInstance(refType.getTypeID());
                groupSnippet.add(getTargetAgentType(correspondingRef, linkType, relativeLoc + refType.getTypeID()));
            }
            allGroupSnippets.add(groupSnippet);
        }
        return allGroupSnippets;
    }

    AgentType getTargetAgentType(ReferenceAgentType agentType, LinkType linkType, int targetAgentType) {

        List<TargetAgentType> targets = LinkRulesWrapper.get(agentType, linkType);
        for (TargetAgentType target : targets) {
            if (target.getTypeID() == targetAgentType) {
                return target;
            }
        }
        Log.errorAndExit("Could not locate TargetAgentType instance - ReferenceAgentType:" + agentType.getTypeID() + " LinkType:" + linkType
                + " TargetAgentType:" + targetAgentType, new UnsupportedOperationException(), EXITCODE.PROGERROR);
        return null;
    }

    private String convertToCombinationMapKey(List<Integer> targets) {
        StringBuilder sb = new StringBuilder();
        targets.stream().forEach(t -> sb.append(t + ","));
        return sb.toString();
    }

    class Snippet extends ArrayList<Integer> {

        /**
         * 
         */
        private static final long serialVersionUID = -7838855368656273831L;

        public Snippet() {
        }

        Snippet(List<Integer> toCopy) {
            super(toCopy);
        }
    }
}
