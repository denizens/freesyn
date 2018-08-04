package bnw.abm.intg.apps.wdll.A4yrRmp;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;

public class WDLL4yrAgeMaritalParentalRelRules extends LinkRulesBuilder {

    @Override
    protected void build() {
        int ageCats = 26;
        ArrayList<TargetAgentType> targets;
        for (int i = 0; i <= 415; i++) {
            int maxMarriedAgeGapCategories = 3;// Max married age difference is 31(inclusive) -16(inclusive) = 16 years
            int ageOfConsentCat = 4;
            int lastAgeCat = 25;
            int genderGap = 208;

            // Married males
            if ((108 <= i && i <= 129) || (134 <= i && i <= 155) || (160 <= i && i <= 181) || (186 <= i && i <= 207)) {

                int femaleOfSameAge = i + genderGap;

                int from, to;
                from = femaleOfSameAge - maxMarriedAgeGapCategories;
                to = femaleOfSameAge;

                if ((from % ageCats) < ageOfConsentCat) {
                    from = genderGap + ((i / ageCats) * ageCats) + ageOfConsentCat;
                }

                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }

            // Married females
            if ((316 <= i && i <= 337) || (342 <= i && i <= 363) || (368 <= i && i <= 389) || (394 <= i && i <= 415)) {
                int maleOfSameAge = i - genderGap;

                int from, to;
                from = maleOfSameAge;
                to = maleOfSameAge + maxMarriedAgeGapCategories;

                if ((to % ageCats) < (i % ageCats)) {// trespassed into next relationship category
                    to = ((maleOfSameAge / ageCats) * ageCats) + lastAgeCat;
                }

                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(from, to));
            }

            int a = i % genderGap;// This captures both males and females

            // Parents of U15 children, SU15c and RU15c parents
            if ((30 <= a & a <= 51) | (134 <= a & a <= 155)) {
                targets = new ArrayList<>();
                // Male
                int from = 0, to = 3;
                if (a % ageCats < to + ageOfConsentCat) {
                    int diff = (to + ageOfConsentCat) - (a % ageCats);
                    to -= diff;
                }
                targets.addAll(getTargets(from, to));

                // Female
                targets.addAll(getTargets(from + genderGap, to + genderGap));

                addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOfU15Child", 1, 8, true), targets);
            }

            // SO15c, SUO15c, RUO15c, RO15c parents
            if ((52 <= a & a <= 103) || (156 <= a & a <= 207)) {
                
                if ((82 <= a & a <= 103) || (186 <= a & a <= 207)) { // Adding U15c to SUO15c and RUO15c parents
                    targets = new ArrayList<>();
                    // Male
                    int from = 0, to = 3;
                    if (a % ageCats < to + ageOfConsentCat) {
                        int diff = (to + ageOfConsentCat) - (a % ageCats);
                        to -= diff;
                    }
                    targets.addAll(getTargets(from, to));

                    // Female
                    targets.addAll(getTargets(from + genderGap, to + genderGap));
                    addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOfU15Child", 1, 8, true), targets);
                }
                if ((8 <= (i % ageCats) & (i % ageCats) <= 25)) {
                    targets = new ArrayList<>();
                    // male
                    targets.addAll(getTargets(4, (i % ageCats) - 4));// SNc children
                    targets.addAll(getTargets(30, (i % ageCats) + (1 * ageCats) - 4));// SU15c children
                    targets.addAll(getTargets(56, (i % ageCats) + (2 * ageCats) - 4));// SO15c children
                    targets.addAll(getTargets(82, (i % ageCats) + (3 * ageCats) - 4));// SUO15c children
                    targets.addAll(getTargets(108, (i % ageCats) + (4 * ageCats) - 4));// RNc children
                    targets.addAll(getTargets(134, (i % ageCats) + (5 * ageCats) - 4));// RU15c children
                    targets.addAll(getTargets(160, (i % ageCats) + (6 * ageCats) - 4));// RO15c children
                    targets.addAll(getTargets(186, (i % ageCats) + (7 * ageCats) - 4));// RUO15c children

                    // female
                    targets.addAll(getTargets(4 + genderGap, (i % ageCats) - 4 + genderGap));// SNc children
                    targets.addAll(getTargets(30 + genderGap, (i % ageCats) + (1 * ageCats) - 4 + genderGap));// SU15c children
                    targets.addAll(getTargets(56 + genderGap, (i % ageCats) + (2 * ageCats) - 4 + genderGap));// SO15c children
                    targets.addAll(getTargets(82 + genderGap, (i % ageCats) + (3 * ageCats) - 4 + genderGap));// SUO15c children
                    targets.addAll(getTargets(108 + genderGap, (i % ageCats) + (4 * ageCats) - 4 + genderGap));// RNc children
                    targets.addAll(getTargets(134 + genderGap, (i % ageCats) + (5 * ageCats) - 4 + genderGap));// RU15c children
                    targets.addAll(getTargets(160 + genderGap, (i % ageCats) + (6 * ageCats) - 4 + genderGap));// RO15c children
                    targets.addAll(getTargets(186 + genderGap, (i % ageCats) + (7 * ageCats) - 4 + genderGap));// RUO15c children
                    addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOfO15Child", 1, 8, true), targets);
                }
                
            }

            // u15 Children, SNc
            if ((0 <= a & a <= 3)) {
                targets = new ArrayList<>();
                targets.addAll(getTargets(26 + a + 4, 51)); // SU15c fathers
                targets.addAll(getTargets(78 + a + 4, 103)); // SUO15c fathers
                targets.addAll(getTargets(130 + a + 4, 155)); // RU15c fathers
                targets.addAll(getTargets(182 + a + 4, 207));// RUO15c fathers
                addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 1, 1, false), targets);

                targets = new ArrayList<>();
                targets.addAll(getTargets(26 + a + 4 + genderGap, 51 + genderGap)); // SU15c mothers
                targets.addAll(getTargets(78 + a + 4 + genderGap, 103 + genderGap));// SUO15c mothers
                targets.addAll(getTargets(130 + a + 4 + genderGap, 155 + genderGap));// RU15c mothers
                targets.addAll(getTargets(182 + a + 4 + genderGap, 207 + genderGap));// RUO15c mothers
                addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 1, 1, false), targets);

            } else if ((4 <= (i % ageCats) && (i % ageCats) <= 21)) {// O15 cats
                targets = new ArrayList<>();
                targets.addAll(getTargets(52 + (i % ageCats) + 4, 77)); // SO15c fathers
                targets.addAll(getTargets(78 + (i % ageCats) + 4, 103)); // SUO15c fathers
                targets.addAll(getTargets(156 + (i % ageCats) + 4, 181)); // RU15c fathers
                targets.addAll(getTargets(182 + (i % ageCats) + 4, 207)); // RUO15c fathers
                addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 0, 1, false), targets);

                targets = new ArrayList<>();
                targets.addAll(getTargets(52 + (i % ageCats) + 4 + genderGap, 77 + genderGap)); // SO15c mothers
                targets.addAll(getTargets(78 + (i % ageCats) + 4 + genderGap, 103 + genderGap)); // SUO15c mothers
                targets.addAll(getTargets(156 + (i % ageCats) + 4 + genderGap, 181 + genderGap)); // RU15c mothers
                targets.addAll(getTargets(182 + (i % ageCats) + 4 + genderGap, 207 + genderGap)); // RUO15c mothers
                addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 0, 1, false), targets);
            }

            // SNc O15 cats
            if ((4 <= i & i <= 25) || (212 <= i & i <= 233)) {
                addRule(createReferenceAgentType(i), LinkType.NONE, new ArrayList<>(0));
            }

            // if ((4 <= i & i <= 25) | (30 <= i & i <= 51) | (56 <= i & i <= 77) | (82 <= i & i <= 103)) {
            // targets = new ArrayList<>();
            // targets.addAll(getTargets(0, (i % 26) - 3));
            // targets.addAll(getTargets(26, (i % 26) + 26 - 3));
            // targets.addAll(getTargets(52, (i % 26) + (2 * 26) - 3));
            // targets.addAll(getTargets(78, (i % 26) + (3 * 26) - 3));
            // targets.trimToSize();
            // addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOf", 1, 4, true), targets);
            // }
            // targets = new ArrayList<>();
            // targets.addAll(getTargets((i % 26) + 4, 26));
            // targets.addAll(getTargets((i % 26) + 4 + 26, 52));
            // targets.trimToSize();
            // addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 1, 1, false), targets);
            //
            // targets = new ArrayList<>();
            // targets.addAll(getTargets((i % 26) + 4 + 52, 78));
            // targets.addAll(getTargets((i % 26) + 4 + 52 + 26, 104));
            // targets.trimToSize();
            // addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 1, 1, false), targets);
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
