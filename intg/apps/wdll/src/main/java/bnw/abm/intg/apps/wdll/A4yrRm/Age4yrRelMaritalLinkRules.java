package bnw.abm.intg.apps.wdll.A4yrRm;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;

public class Age4yrRelMaritalLinkRules extends LinkRulesBuilder {

    @Override
    protected void build() {
        int ageCats = 26;
        ArrayList<TargetAgentType> targets;
        for (int i = 0; i <= 103; i++) {
            int maxMarriedAgeGapCategories = 3;// Max married age difference is 31(inclusive) -16(inclusive) = 16 years
            int ageOfConsentCat = 4;
            int lastAgeCat = 25;
            int genderGap = 52;

            int a = i % genderGap;// This captures both males and females

            // Married males
            if (30 <= i && i <= 51) {

                int femaleOfSameAge = i + genderGap;

                int from, to;
                from = femaleOfSameAge - maxMarriedAgeGapCategories;
                to = femaleOfSameAge + 1;

                if ((from % ageCats) < ageOfConsentCat) {
                    from = genderGap + ((i / ageCats) * ageCats) + ageOfConsentCat;
                }
                if (to > 103) {
                    to = 103;
                }

                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }

            // Married females
            if (82 <= i && i <= 103) {
                int maleOfSameAge = i - genderGap;

                int from, to;
                from = maleOfSameAge - 1;
                to = maleOfSameAge + maxMarriedAgeGapCategories;

                if ((to % ageCats) < (i % ageCats)) {// trespassed into next relationship category
                    to = ((maleOfSameAge / ageCats) * ageCats) + lastAgeCat;
                }
                if (from < 30) {
                    from = 30;
                }

                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }

            if ((4 <= i & i <= 25) | (30 <= i & i <= 51) | (56 <= i & i <= 77) | (82 <= i & i <= 103)) {
                targets = new ArrayList<>();
                int from = (0 + (i % 26) - 11);
                int to = (i % 26) - 4;
                targets.addAll(getTargets(from < 0 ? 0 : from, to));

                from = 26 + (i % 26) - 11;
                to = 26 + (i % 26) - 4;
                targets.addAll(getTargets(from < 26 ? 26 : from, to));

                from = 52 + (i % 26) - 11;
                to = 52 + (i % 26) - 4;
                targets.addAll(getTargets(from < 52 ? 52 : from, to));

                from = 78 + (i % 26) - 11;
                to = 78 + (i % 26) - 4;
                targets.addAll(getTargets(from < 78 ? 78 : from, to));
                targets.trimToSize();
                addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOf", 0, 8, true), targets);
            }
            targets = new ArrayList<>();
            int from = (i % 26) + 4;
            int to = (i % 26) + 11;
            targets.addAll(getTargets(from, to > 26 ? 26 : to));

            from = (i % 26) + 4 + 26;
            to = (i % 26) + 11 + 26;
            targets.addAll(getTargets(from, to > 52 ? 52 : to));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 0, 1, false), targets);

            targets = new ArrayList<>();
            from = (i % 26) + 4 + 52;
            to = (i % 26) + 11 + 52;
            targets.addAll(getTargets(from, to > 78 ? 78 : to));

            from = (i % 26) + 4 + 78;
            to = (i % 26) + 11 + 78;
            targets.addAll(getTargets(from, to > 104 ? 104 : to));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 0, 1, false), targets);

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
