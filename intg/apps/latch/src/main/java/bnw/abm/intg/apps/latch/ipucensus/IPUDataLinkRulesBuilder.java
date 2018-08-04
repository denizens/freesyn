package bnw.abm.intg.apps.latch.ipucensus;

import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.LinkType.NONELinkType;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;

import java.util.ArrayList;
import java.util.List;

enum MyLink {
    MALE_MARRIED(LinkType.getInstance("Male-Married", 1, 1, true)),
    FEMALE_MARRIED(LinkType.getInstance("Female-Married", 1, 1, true)),
    M_MALE_PARENTOF_CHILD(LinkType.getInstance("Married-Male-ParentOf-Child", 0, 6, true)),
    M_FEMALE_PARENTOF_CHILD(LinkType.getInstance("Married-Female-ParentOf-Child", 0, 6, true)),
    LP_MALE_PARENTOF_CHILD(LinkType.getInstance("LoneParent-Male-ParentOf-Child", 1, 7, true)),
    LP_FEMALE_PARENTOF_CHILD(LinkType.getInstance("LoneParent-Female-ParentOf-Child", 1, 7, true)),
    M_MALE_PARENTOF_MLPCHILD(LinkType.getInstance("Married-Male-ParentOf-MLPChild", 0, 3, true)),
    M_FEMALE_PARENTOF_MLPCHILD(LinkType.getInstance("Married-Female-ParentOf-MLPChild", 0, 3, true)),
    LP_MALE_PARENTOF_MLPCHILD(LinkType.getInstance("LoneParent-Male-ParentOf-MLPChild", 0, 3, true)),
    LP_FEMALE_PARENTOF_MLPCHILD(LinkType.getInstance("LoneParent-Female-ParentOf-MLPChild", 0, 3, true)),
    MLP_CHILDOF(LinkType.getInstance("MLP-ChildOf", 0, 2, false)),
    CHILDOF(LinkType.getInstance("ChildOf", 1, 2, false)),
    GROUPHOUSEHOLD(NONELinkType.getInstance("GroupHousehold", 1, 7, true)),
    FAMILY_RELATIVE(LinkType.getInstance("Family-RelativeOf", 0, 3, true)),
    FAMILY_RELATIVE_INV(LinkType.getInstance("Family-RelativeOf-Inv", 0, 1, false)),
    RELATIVE_OTHER(LinkType.getInstance("Other-Family-Relative", 0, 7, true));

    private final LinkType linkType;

    MyLink(LinkType linkType) {
        this.linkType = linkType;
    }

    LinkType getLink() {
        return this.linkType;
    }
}

enum AT {
    ST_M_MAR(0),
    LS_M_MAR(5),
    ST_F_MAR(7),
    LS_F_MAR(12),
    ST_M_LNPAR(14),
    LS_M_LNPAR(19),
    ST_F_LNPAR(21),
    LS_F_LNPAR(26),
    ST_M_CHLD(28),
    LS_M_CHLD(34),
    ST_F_CHLD(35),
    LS_F_CHLD(41),
    ST_M_GRP(42),
    LS_M_GRP(47),
    ST_F_GRP(49),
    LS_F_GRP(54),
    ST_M_1P(56),
    LS_M_1P(61),
    ST_F_1P(63),
    LS_F_1P(68),
    ST_REL(70),
    LS_REL(83);

    int catId;

    AT(int catId) {
        this.catId = catId;
    }

    int getCatId() {
        return this.catId;
    }
}

public class IPUDataLinkRulesBuilder extends LinkRulesBuilder {

    @Override
    public void build() {
        ArrayList<TargetAgentType> targets;
        int agentTypesCount = 84;
        int ageCatsCount = 7;
        int a15_24cat = 5;
        for (int i = 0; i < agentTypesCount; i++) {
            if (i <= AT.LS_M_MAR.getCatId()) {// Married relationships for male
                // Partner is same age or one age category younger
                int from = i + ageCatsCount;
                int to = 0;
                if (i == a15_24cat)//if 15-24 years
                    to = i + ageCatsCount;
                else
                    to = i + ageCatsCount + 1;
                addRule(createReferenceAgentType(i), MyLink.MALE_MARRIED.getLink(), getTargets(from, to));

                // Can have children, but not compulsory. Child is 1 - 4 age categories younger
                // U15Child 32 - 47, Student
            }
            if (AT.ST_F_MAR.getCatId() <= i & i <= AT.LS_F_MAR.getCatId()) { // Married relationship for female
                // Partner is same age or one age category older
                int from, to;
                if (i == ageCatsCount) {
                    from = i - ageCatsCount;
                } else {
                    from = i - ageCatsCount - 1;
                }
                to = i - ageCatsCount;
                addRule(createReferenceAgentType(i), MyLink.FEMALE_MARRIED.getLink(), getTargets(from, to));
            }

            if ((AT.ST_M_MAR.getCatId() <= i & i <= AT.LS_M_MAR.getCatId()) || (AT.ST_F_MAR.getCatId() <= i & i <= AT.LS_F_MAR.getCatId()
            ) || (AT.ST_M_LNPAR
                    .getCatId() <= i & i <= AT.LS_M_LNPAR.getCatId()) || (AT.ST_F_LNPAR.getCatId() <= i & i <= AT.LS_F_LNPAR.getCatId())) {
                // Parental relationships for - (Married
                // and Lone Parents)
                targets = new ArrayList<>();
                int fromOffset = (i % ageCatsCount) + 1, toOffset = (i % ageCatsCount) + 4, from, to;

                // Children male
                from = AT.ST_M_CHLD.getCatId() + fromOffset;
                to = AT.ST_M_CHLD.getCatId() + toOffset;
                if (to > AT.LS_M_CHLD.getCatId())
                    to = AT.LS_M_CHLD.getCatId();
                targets.addAll(getTargets(from, to));

                // Children female
                from = AT.ST_F_CHLD.getCatId() + fromOffset;
                to = AT.ST_F_CHLD.getCatId() + toOffset;
                if (to > AT.LS_F_CHLD.getCatId())
                    to = AT.LS_F_CHLD.getCatId();
                targets.addAll(getTargets(from, to));

                if (AT.ST_M_MAR.getCatId() <= i & i <= AT.LS_M_MAR.getCatId()) // Married male - link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.M_MALE_PARENTOF_CHILD.getLink(), targets);
                else if (AT.ST_F_MAR.getCatId() <= i & i <= AT.LS_F_MAR.getCatId()) // Married female - link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.M_FEMALE_PARENTOF_CHILD.getLink(), targets);
                else if (AT.ST_M_LNPAR.getCatId() <= i & i <= AT.LS_M_LNPAR.getCatId()) // Lone parents male - link is active and compulsory
                    addRule(createReferenceAgentType(i), MyLink.LP_MALE_PARENTOF_CHILD.getLink(), targets);
                else if (AT.ST_F_LNPAR.getCatId() <= i & i <= AT.LS_F_LNPAR.getCatId()) // Lone parents female - link is active and
                    // compulsory
                    addRule(createReferenceAgentType(i), MyLink.LP_FEMALE_PARENTOF_CHILD.getLink(), targets);

                targets = new ArrayList<>();
                // Allowing married people as Children to allow multi-family households
                from = AT.ST_M_MAR.getCatId() + fromOffset;
                to = AT.ST_M_MAR.getCatId() + toOffset;
                if (to > AT.LS_M_MAR.getCatId())
                    to = AT.LS_M_MAR.getCatId();
                targets.addAll(getTargets(from, to)); // Married male children

                from = AT.ST_F_MAR.getCatId() + fromOffset;
                to = AT.ST_F_MAR.getCatId() + toOffset;
                if (to > AT.LS_F_MAR.getCatId())
                    to = AT.LS_F_MAR.getCatId();
                targets.addAll(getTargets(from, to));// Married female children

                // Allowing lone parents as children to allow multi-family households
                from = AT.ST_M_LNPAR.getCatId() + fromOffset;
                to = AT.ST_M_LNPAR.getCatId() + toOffset;
                if (to > AT.LS_M_LNPAR.getCatId())
                    to = AT.LS_M_LNPAR.getCatId();
                targets.addAll(getTargets(from, to)); // Lone parent male children

                from = AT.ST_F_LNPAR.getCatId() + fromOffset;
                to = AT.ST_F_LNPAR.getCatId() + toOffset;
                if (to > AT.LS_F_LNPAR.getCatId())
                    to = AT.LS_F_LNPAR.getCatId();
                targets.addAll(getTargets(from, to));// Lone parent female children

                if (AT.ST_M_MAR.getCatId() <= i & i <= AT.LS_M_MAR.getCatId()) // Married, parent of married or lone parent children -
                    // link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.M_MALE_PARENTOF_MLPCHILD.getLink(), targets);
                else if (AT.ST_F_MAR.getCatId() <= i & i <= AT.LS_F_MAR.getCatId()) // Married, parent of married or lone parent children
                    // - link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.M_FEMALE_PARENTOF_MLPCHILD.getLink(), targets);
                else if (AT.ST_M_LNPAR.getCatId() <= i & i <= AT.LS_M_LNPAR.getCatId()) // Lone parent, parent of married or lone parent
                    // children - link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.LP_MALE_PARENTOF_MLPCHILD.getLink(), targets);
                else if (AT.ST_F_LNPAR.getCatId() <= i & i <= AT.LS_M_LNPAR.getCatId()) // Lone parent, parent of married or lone parent
                    // children - link is active but not compulsory
                    addRule(createReferenceAgentType(i), MyLink.LP_FEMALE_PARENTOF_MLPCHILD.getLink(), targets);

                // Relatives - active but not compulsory
                addRule(createReferenceAgentType(i),
                        MyLink.FAMILY_RELATIVE.getLink(),
                        getTargets(AT.ST_REL.getCatId(), AT.LS_REL.getCatId()));
            }

            // ChildOf relationships
            if ((AT.ST_M_CHLD.getCatId() <= i & i <= AT.LS_F_CHLD.getCatId()) // Children male and female
                    || (AT.ST_M_MAR.getCatId() <= i & i <= AT.LS_M_MAR.getCatId()) || (AT.ST_F_MAR.getCatId() <= i & i <= AT.LS_F_MAR
                    .getCatId()) // Married male and female
                    || (AT.ST_M_LNPAR.getCatId() <= i & i <= AT.LS_M_LNPAR.getCatId()) || (AT.ST_F_LNPAR.getCatId() <= i & i <= AT
                    .LS_F_LNPAR
                    .getCatId())) {// LoneParents male and female
                targets = new ArrayList<>();
                // A parent must not be older than 4 age categories than child's age category. And parent must be at least one age
                // category older than the child.
                int fromOffset = (i % ageCatsCount) - 4, toOffset = (i % ageCatsCount) - 1, from, to;

                int startMarriedDad = AT.ST_M_MAR.getCatId(), startMarriedMom = AT.ST_F_MAR.getCatId();
                // Married dad
                from = (fromOffset < 0) ? startMarriedDad : startMarriedDad + fromOffset;
                to = startMarriedDad + toOffset;
                targets.addAll(getTargets(from, to));
                // Married mom
                from = (fromOffset < 0) ? startMarriedMom : startMarriedMom + fromOffset;
                to = startMarriedMom + toOffset;
                targets.addAll(getTargets(from, to));

                int startLnPDad = AT.ST_M_LNPAR.getCatId(), startLnPMom = AT.ST_F_LNPAR.getCatId();
                // Married dad
                from = (fromOffset < 0) ? startLnPDad : startLnPDad + fromOffset;
                to = startLnPDad + toOffset;
                targets.addAll(getTargets(from, to));
                // Married mom
                from = (fromOffset < 0) ? startLnPMom : startLnPMom + fromOffset;
                to = startLnPMom + toOffset;
                targets.addAll(getTargets(from, to));
                if (AT.ST_M_CHLD.getCatId() <= i && i <= AT.LS_F_CHLD.getCatId()) { // For children categories ChildOf relationship is
                    // compulsory
                    addRule(createReferenceAgentType(i), MyLink.CHILDOF.getLink(), targets);
                } else {
                    // For Married and LoneParents being a child of someone is not compulsory
                    addRule(createReferenceAgentType(i), MyLink.MLP_CHILDOF.getLink(), targets);
                }
            }

            if ((AT.ST_M_GRP.getCatId() <= i & i <= AT.LS_M_GRP.getCatId()) || (AT.ST_F_GRP.getCatId() <= i & i <= AT.LS_F_GRP.getCatId()
            )) { // GroupHholds
                targets = new ArrayList<>();
                targets.addAll(getTargets(AT.ST_M_GRP.getCatId(), AT.LS_M_GRP.getCatId()));
                targets.addAll(getTargets(AT.ST_F_GRP.getCatId(), AT.LS_F_GRP.getCatId()));

                addRule(createReferenceAgentType(i), MyLink.GROUPHOUSEHOLD.getLink(), targets);
            }

            if ((AT.ST_M_1P.getCatId() <= i && i <= AT.LS_M_1P.getCatId()) || (AT.ST_F_1P.getCatId() <= i && i <= AT.LS_F_1P.getCatId()))
            {//
                // Lone Person
                addRule(createReferenceAgentType(i), LinkType.NONE, new ArrayList<>(0));
            }

            if (AT.ST_REL.getCatId() <= i & i <= AT.LS_REL.getCatId()) { // Relative
                addRule(createReferenceAgentType(i),
                        MyLink.RELATIVE_OTHER.getLink(),
                        getTargets(AT.ST_REL.getCatId(), AT.LS_REL.getCatId()));

                targets = new ArrayList<>();
                targets.addAll(getTargets(AT.ST_M_MAR.getCatId(), AT.LS_M_MAR.getCatId()));
                targets.addAll(getTargets(AT.ST_F_MAR.getCatId(), AT.LS_F_MAR.getCatId()));
                targets.addAll(getTargets(AT.ST_M_LNPAR.getCatId(), AT.LS_M_LNPAR.getCatId()));
                targets.addAll(getTargets(AT.ST_F_LNPAR.getCatId(), AT.LS_F_LNPAR.getCatId()));
                addRule(createReferenceAgentType(i), MyLink.FAMILY_RELATIVE_INV.getLink(), targets);
            }
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
