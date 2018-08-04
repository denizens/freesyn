package bnw.abm.intg.apps.wdll;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;

public class WDLL_LatchAgeMaritalRelRules extends LinkRulesBuilder {

    @Override
    public void build() {
        ArrayList<TargetAgentType> targets;
        for (int i = 0; i < 32; i++) {
            if (9 <= i & i <= 15) {
                int from, to;
                if (i == 9) {
                    from = 24 + (i % 8) - 1 + 1;
                } else {
                    from = 24 + (i % 8) - 1;
                }
                to = 24 + (i % 8);
                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }
            if (25 <= i & i <= 31) {
                int from, to;
                from = 8 + (i % 8);
                if (i == 31) {
                    to = 8 + (i % 8);
                } else {
                    to = 8 + (i % 8) + 1;
                }
                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }
            if ((1 <= i & i <= 7) | (9 <= i & i <= 15) | (17 <= i & i <= 23) | (25 <= i & i <= 31)) {
                int fromOffset = (i % 8) < 4 ? 0 : (i % 8) - 4;
                targets = new ArrayList<>();
                targets.addAll(getTargets(0 + fromOffset, (i % 8) - 1));
                targets.addAll(getTargets(8 + fromOffset, (i % 8) + 8 - 1));
                targets.addAll(getTargets(16 + fromOffset, (i % 8) + (2 * 8) - 1));
                targets.addAll(getTargets(24 + fromOffset, (i % 8) + (3 * 8) - 1));
                targets.trimToSize();
                addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOf", 1, 4, true), targets);
            }

            int toOffset = (i % 8) + 4 > 7 ? 7 : (i % 8) + 4;
            targets = new ArrayList<>();
            targets.addAll(getTargets(0 + (i % 8) + 1, 0 + toOffset));
            targets.addAll(getTargets(8 + (i % 8) + 1, 8 + toOffset));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 1, 1, false), targets);

            targets = new ArrayList<>();
            targets.addAll(getTargets(16 + (i % 8) + 1, 16 + toOffset));
            targets.addAll(getTargets(24 + (i % 8) + 1, 24 + toOffset));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 1, 1, false), targets);
        }

    }

    List<TargetAgentType> getTargets(int fromIDInclusive, int toIDInclusive) {
        List<TargetAgentType> tars = new ArrayList<>((toIDInclusive < fromIDInclusive) ? 0 : toIDInclusive - fromIDInclusive + 1);
        for (int i = fromIDInclusive; i <= toIDInclusive; i++) {
            tars.add(createTargetAgentTypeInstance(i, 1));
        }
        return tars;
    }

}