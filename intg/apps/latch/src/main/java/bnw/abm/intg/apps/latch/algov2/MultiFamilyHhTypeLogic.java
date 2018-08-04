package bnw.abm.intg.apps.latch.algov2;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum FamilyType {
    COUPLEFAMILYWITHCHILDREN("Couple family with children", 1),
    COUPLEONLY("Couple family with no children", 3),
    ONEPARENTFAMILY("One parent family", 2),
    OTHERFAMILY("Other family", 3),
    LONEPERSON("Lone person household", 4),
    GROUPHOUSEHOLD("Group household", 5);

    private final String familyType;
    private final int priority;

    FamilyType(String familyType, int priority) {
        this.familyType = familyType;
        this.priority = priority;
    }

    public String getFamilyTypeStr() {
        return this.familyType;
    }

    int priority() {
        return this.priority;
    }
}

enum FamilyCount {
    F1("1 Family"),
    F2("2 Family"),
    F3("3 Family");
    private final String familyCount;

    private FamilyCount(String familyCount) {
        this.familyCount = familyCount;
    }

    public String getFamilyCountStr() {
        return this.familyCount;
    }

}

enum HouseholdSize {
    P1("1", 1),
    P2("2", 2),
    P3("3", 3),
    P4("4", 4),
    P5("5", 5),
    P6("6", 6),
    P7("7", 7),
    P8("8", 8);
    private final String personsCountStr;
    private final int personsCount;

    private HouseholdSize(String personsCountStr, int oersonsCount) {
        this.personsCountStr = personsCountStr;
        this.personsCount = oersonsCount;
    }

    public String getHouseholdSizeStr() {
        return this.personsCountStr;
    }

    public int getHouseholdSize() {
        return this.personsCount;
    }
}

public class MultiFamilyHhTypeLogic extends GroupTypeLogic {
    static List<String> groupTypes = new ArrayList<>(14);

    public MultiFamilyHhTypeLogic() {
        List<String> familyTypes = new ArrayList<>(14);

        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.COUPLEONLY);
        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.COUPLEFAMILYWITHCHILDREN);
        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.ONEPARENTFAMILY);
        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.OTHERFAMILY);
        familyTypes.add(FamilyCount.F2.name() + "," + FamilyType.COUPLEONLY);
        familyTypes.add(FamilyCount.F2.name() + "," + FamilyType.COUPLEFAMILYWITHCHILDREN);
        familyTypes.add(FamilyCount.F2.name() + "," + FamilyType.ONEPARENTFAMILY);
        familyTypes.add(FamilyCount.F2.name() + "," + FamilyType.OTHERFAMILY);
        familyTypes.add(FamilyCount.F3.name() + "," + FamilyType.COUPLEONLY);
        familyTypes.add(FamilyCount.F3.name() + "," + FamilyType.COUPLEFAMILYWITHCHILDREN);
        familyTypes.add(FamilyCount.F3.name() + "," + FamilyType.ONEPARENTFAMILY);
        familyTypes.add(FamilyCount.F3.name() + "," + FamilyType.OTHERFAMILY);
        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.LONEPERSON);
        familyTypes.add(FamilyCount.F1.name() + "," + FamilyType.GROUPHOUSEHOLD);

        for (HouseholdSize hs : HouseholdSize.values()) {
            for (String familyType : familyTypes) {
                groupTypes.add(hs.name() + "," + familyType);
            }
        }
        groupTypes.add("Invalid");
    }

    public static List<String> getGroupTypes() {
        return groupTypes;
    }

    public static String getGroupTypeStr(int groupTypeId) {
        return groupTypes.get(groupTypeId);
    }

    static List<FamilyUnit> computeFamilyUnits(GroupTemplate groupTemplate) {
        if (groupTemplate.size() == 1) {
            Member member = groupTemplate.getCurrentReferenceMembers().get(0);
            if (AT.ST_M_1P.getCatId() <= member.getTypeID() && member.getTypeID() <= AT.LS_F_1P.getCatId()) {
                FamilyUnit fUnit = new FamilyUnit(member);
                fUnit.type = FamilyType.LONEPERSON;
                return Arrays.asList(fUnit);
            } else {
                return new ArrayList<>(0);
                // throw new IllegalStateException("Group template has only one member and that is not a Lone person" + groupTemplate);
            }
        }

        Member groupHhMember = groupTemplate.getCurrentReferenceMembers().get(0);
        if (AT.ST_M_GRP.getCatId() <= groupHhMember.getTypeID() && groupHhMember.getTypeID() <= AT.LS_F_GRP.getCatId()) {
            FamilyUnit fUnit = new FamilyUnit(groupHhMember);
            fUnit.type = FamilyType.GROUPHOUSEHOLD;
            fUnit.addOtherMembers(groupTemplate.getAdjacentMembers(groupHhMember, MyLink.GROUPHOUSEHOLD.getLink()));
            if (groupTemplate.size() != fUnit.size()) {
                throw new IllegalAccessError();
            }
            return Arrays.asList(fUnit);
        }

        List<FamilyUnit> familyUnits = new ArrayList<>(3);
        List<Member> allMembers = new ArrayList<>(groupTemplate.getAllMembers());
        int checkedGroupHeadsCount = 0;
        while (allMembers.size() > 0 && groupTemplate.size() != checkedGroupHeadsCount) {// Stop when we have tried all members

            Member head = allMembers.get(0);
            checkedGroupHeadsCount++;
            if (AT.ST_M_MAR.getCatId() <= head.getTypeID() && head.getTypeID() <= AT.LS_F_MAR.getCatId()) {
                List<Member> partners = null, couplesChildren = null;
                if (AT.ST_M_MAR.getCatId() <= head.getTypeID() && head.getTypeID() <= AT.LS_M_MAR.getCatId()) {
                    partners = groupTemplate.getAdjacentMembers(head, MyLink.MALE_MARRIED.getLink());
                    couplesChildren = groupTemplate.getAdjacentMembers(head, MyLink.M_MALE_PARENTOF_CHILD.getLink());
                } else {
                    partners = groupTemplate.getAdjacentMembers(head, MyLink.FEMALE_MARRIED.getLink());
                    couplesChildren = groupTemplate.getAdjacentMembers(head, MyLink.M_FEMALE_PARENTOF_CHILD.getLink());
                }

                if (partners.size() == 1) {
                    allMembers.remove(partners.get(0));
                    allMembers.remove(head);
                    // We don't take M-ParentOf_MLPChild because that child is a separate family unit

                    allMembers.removeAll(couplesChildren);

                    FamilyUnit unit = new FamilyUnit(head);
                    unit.addPartner(partners.get(0));
                    unit.addOtherMembers(couplesChildren);
                    unit.type = couplesChildren.isEmpty() ? FamilyType.COUPLEONLY : FamilyType.COUPLEFAMILYWITHCHILDREN;

                    // Now we add relatives of this family unit (only the ones that do not qualify for a separate family unit). We do
                    // this after
                    // deciding family type otherwise relatives may be considered as children.
                    List<Member> relativesOf1 = groupTemplate.getAdjacentMembers(head, MyLink.FAMILY_RELATIVE.getLink());
                    relativesOf1.removeIf(r -> !groupTemplate.getAdjacentMembers(r, MyLink.RELATIVE_OTHER.getLink()).isEmpty());
                    allMembers.removeAll(relativesOf1);
                    List<Member> relativesOf2 = groupTemplate.getAdjacentMembers(partners.get(0), MyLink.FAMILY_RELATIVE.getLink());
                    relativesOf2.removeIf(r -> !groupTemplate.getAdjacentMembers(r, MyLink.RELATIVE_OTHER.getLink()).isEmpty());
                    allMembers.removeAll(relativesOf2);

                    unit.addOtherMembers(relativesOf1);
                    unit.addOtherMembers(relativesOf2);
                    familyUnits.add(unit);
                    continue;
                } else {
                    return new ArrayList<>(0);
                    // throw new IllegalStateException(head + " must have one partner. We have detected " + partners.size() +
                    // " partners in group "
                    // + groupTemplate);
                }
            }

            if (AT.ST_M_LNPAR.getCatId() <= head.getTypeID() && head.getTypeID() <= AT.LS_F_LNPAR.getCatId()) {
                List<Member> dependentChildren = null;
                if (AT.ST_M_LNPAR.getCatId() <= head.getTypeID() && head.getTypeID() <= AT.LS_M_LNPAR.getCatId())
                    dependentChildren = groupTemplate.getAdjacentMembers(head, MyLink.LP_MALE_PARENTOF_CHILD.getLink());
                else
                    dependentChildren = groupTemplate.getAdjacentMembers(head, MyLink.LP_FEMALE_PARENTOF_CHILD.getLink());

                if (dependentChildren.size() > 0) {
                    allMembers.removeAll(dependentChildren);
                    allMembers.remove(head);
                    // Find any relatives in the family
                    List<Member> relativesOf1 = groupTemplate.getAdjacentMembers(head, MyLink.FAMILY_RELATIVE.getLink());
                    relativesOf1.removeIf(r -> !groupTemplate.getAdjacentMembers(r, MyLink.RELATIVE_OTHER.getLink()).isEmpty());
                    allMembers.removeAll(relativesOf1);

                    FamilyUnit fUnit = new FamilyUnit(head);
                    fUnit.addOtherMembers(dependentChildren);
                    fUnit.addOtherMembers(relativesOf1);
                    fUnit.type = FamilyType.ONEPARENTFAMILY;
                    familyUnits.add(fUnit);
                    continue;
                } else {
                    return new ArrayList<>(0);
                    // throw new IllegalStateException(head + " must have at least one dependent child. We have detected "
                    // + dependentChildren.size() + " children in group " + groupTemplate);
                }
            }

            if (AT.ST_REL.getCatId() <= head.getTypeID() && head.getTypeID() <= AT.LS_REL.getCatId()) {
                List<Member> otherFamilyRelative = groupTemplate.getAdjacentMembers(head, MyLink.RELATIVE_OTHER.getLink());
                if (otherFamilyRelative.size() > 0) {
                    FamilyUnit fUnit = new FamilyUnit(head);
                    fUnit.addOtherMembers(otherFamilyRelative);
                    fUnit.type = FamilyType.OTHERFAMILY;
                    allMembers.remove(head);
                    allMembers.removeAll(otherFamilyRelative);
                    familyUnits.add(fUnit);
                    continue;
                }
            }

            if (allMembers.contains(head)) {// Head could not form a family unit this time. Put it at the back of the list.
                allMembers.remove(head);
                allMembers.add(head);
            }
        }

        return familyUnits;
    }

    @Override
    public GroupType computeGroupType(GroupTemplate groupTemplate) {

        // Second and third families should not have relatives, unless it is an other family. If they have, the household must be rejected
        // 1st family has relatives - 2nd and 3rd also have relative - reject
        // 1st family has relatives - 2nd 3rd has no relatives - keep
        // 1st family has no relatives- 2nd 3rd have relatives - reject
        // 1st family has no relatives - 2nd 3rd have relative - reject
        // If there is a couple family with children, then that is the household type
        // Otherwise the largest becomes the primary family type.

        List<FamilyUnit> familyUnits = computeFamilyUnits(groupTemplate);

        for (int i = 1; i < familyUnits.size(); i++) {
            if (familyUnits.get(i).type != FamilyType.OTHERFAMILY) {
                if (familyUnits.get(i).numberOfRelativesWithRelativeOfLinkType() > 0) {
                    return GroupType.getInstance(groupTypes.indexOf("Invalid"));
                    // throw new IllegalStateException(
                    // "Non-primary family has relatives. Family units: " + familyUnits + " Group Template: " + groupTemplate);
                }
            }
            // Don't allow children more than the primary unit
            if (familyUnits.get(i).getChildrenSize() > familyUnits.get(0).getChildrenSize()) {
                return GroupType.getInstance(groupTypes.indexOf("Invalid"));
            }
        }

        HouseholdSize householdSize = null;
        if (groupTemplate.size() <= HouseholdSize.values().length)
            householdSize = HouseholdSize.values()[groupTemplate.size() - 1];
        else
            return GroupType.getInstance(groupTypes.indexOf("Invalid"));

        FamilyCount familyCount = null;
        switch (familyUnits.size()) {
            case 1:
                familyCount = FamilyCount.F1;
                break;
            case 2:
                familyCount = FamilyCount.F2;
                break;
            case 3:
                familyCount = FamilyCount.F3;
                break;
            default:
                return GroupType.getInstance(groupTypes.indexOf("Invalid"));
        }

        FamilyType familyType = familyUnits.get(0).type;
        familyUnits = null;// Trying to make it easy to garbage collector

        return GroupType.getInstance(groupTypes.indexOf(householdSize.name() + "," + familyCount.name() + "," + familyType.name()));
    }
}

class FamilyUnit {
    FamilyType type = null;
    private int headType = -1, partnerType = -1;
    private int children = 0, relatives = 0, groupies = 0;

    FamilyUnit(Member head) {
        this.headType = head.getTypeID();
    }

    void addPartner(Member partner) {
        if (this.partnerType == -1)
            this.partnerType = partner.getTypeID();
        else
            throw new UnsupportedOperationException("Partner alreay exists");
    }

    void addOtherMembers(List<Member> others) {
        for (Member member : others) {
            int memType = member.getTypeID();
            if (AT.ST_M_CHLD.getCatId() <= memType & memType <= AT.LS_F_CHLD.getCatId()) {
                children++;
            } else if (AT.ST_REL.getCatId() <= memType && memType <= AT.LS_REL.getCatId()) {
                relatives++;
            } else if (AT.ST_M_GRP.getCatId() <= memType && memType <= AT.LS_F_GRP.getCatId()) {
                groupies++;
            } else {
                throw new IllegalStateException("Unexpected memebr of type " + memType);
            }
        }
    }

    int numberOfRelativesWithRelativeOfLinkType() {
        // Not taking head for relatives. Idea of this function is to get number of relatives who are related to family unit head
        if (this.type == FamilyType.OTHERFAMILY)
            return 0;
        else
            return this.relatives;
    }

    int size() {
        int size = 0;
        if (headType != -1)
            size++;
        if (partnerType != -1)
            size++;
        return size + children + relatives + groupies;
    }

    int getChildrenSize() {
        return children;
    }

    int getTypePriority() {
        return this.type.priority();
    }

    public String toString() {
        return this.type.toString();
    }
}
