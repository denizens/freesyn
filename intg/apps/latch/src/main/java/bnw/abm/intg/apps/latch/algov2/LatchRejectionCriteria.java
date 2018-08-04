package bnw.abm.intg.apps.latch.algov2;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.templates.RejectionCriterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LatchRejectionCriteria {

    public static class FamilyTreeDegreeBasedRejection extends RejectionCriterion {
        public Reject validate(GroupTemplate groupTemplate) {
            int familyUnits = 0;
            List<Member> allMembers = new ArrayList<>(groupTemplate.getAllMembers());
            Member first = allMembers.get(0);

            if (!(AT.ST_M_GRP.getCatId() <= first.getTypeID() && first.getTypeID() <= AT.LS_F_1P.getCatId())) { // If not GroupHholds or
                // lone persons
                for (int i = 0; i < allMembers.size(); ) {
                    Member member = allMembers.get(i);

                    if (AT.ST_M_MAR.getCatId() <= member.getTypeID() && member.getTypeID() <= AT.LS_M_MAR.getCatId()) {
                        //Count marital family unit of a married-male
                        List<Member> partners = groupTemplate.getAdjacentMembers(member, MyLink.MALE_MARRIED.getLink());
                        allMembers.remove(member);
                        if (!partners.isEmpty())
                            allMembers.remove(partners.get(0));
                        familyUnits++;

                    } else if (AT.ST_F_MAR.getCatId() <= member.getTypeID() && member.getTypeID() <= AT.LS_F_MAR.getCatId()) {
                        //Count marital family unit of a married-female
                        List<Member> partners = groupTemplate.getAdjacentMembers(member, MyLink.FEMALE_MARRIED.getLink());
                        allMembers.remove(member);
                        if (!partners.isEmpty())
                            allMembers.remove(partners.get(0));
                        familyUnits++;

                    } else if (AT.ST_M_LNPAR.getCatId() <= member.getTypeID() && member.getTypeID() <= AT.LS_F_LNPAR.getCatId()) {
                        //Count family unit of a lone parent
                        allMembers.remove(member);
                        familyUnits++;

                    } else if (AT.ST_REL.getCatId() <= member.getTypeID() && member.getTypeID() <= AT.LS_REL.getCatId()) {// Relative
                        // other families
                        List<Member> relatives = groupTemplate.getAdjacentMembers(member, MyLink.RELATIVE_OTHER.getLink());
                        if (!relatives.isEmpty()) {
                            allMembers.remove(member);
                            allMembers.removeAll(relatives);
                            familyUnits++;
                        } else {
                            allMembers.remove(member);
                        }
                    } else {
                        i++;
                    }
                }
                // If number of family units is more than max allowed, then reject.
                int householdTypeOffset = groupTemplate.getGroupType().getID() % 14;
                if ((0 <= householdTypeOffset && householdTypeOffset <= 3) && familyUnits > 1) {
                    return Reject.YES;
                } else if ((4 <= householdTypeOffset && householdTypeOffset <= 7) && familyUnits > 2) {
                    return Reject.YES;
                } else if ((8 <= householdTypeOffset && householdTypeOffset <= 11) && familyUnits > 3) {
                    return Reject.YES;
                }
            }
            return Reject.NO;
        }
    }

    /**
     * Make sure relatives are only in 1 family unit
     *
     * @author Bhagya N. Wickramasinghe
     */
    public static class RelativesBasedRejection extends RejectionCriterion {

        @Override
        public Reject validate(GroupTemplate groupTemplate) {
            /*
             * Get relatives who do not have RELATIVE_OTHER relationships. These are the ones that are in a family unit as relatives. If
             * RELATIVE_OTHER relationship is there then they belong to a OTHER_FAMILY unit for sure.
             */
            List<Member> finalisedRelatives = groupTemplate.getAllMembers().stream()//
                                                           .filter(m -> (AT.ST_REL.getCatId() <= m.getTypeID() && m.getTypeID() <= AT.LS_REL
                                                                   .getCatId()) && //
                                                                   // !groupTemplate.getCurrentReferenceMembers().contains(m) && // This
                                                                   // for algov2. No significance in algov1
                                                                   groupTemplate.getAdjacentMembers(m, MyLink.RELATIVE_OTHER.getLink())
                                                                                .isEmpty())
                                                           .collect(Collectors.toList());

            /*
             * These are not in an OTHER_FAMILY, check if these relatives belong to one family. If they are in multiple family units we can
             * reject.
             */
            if (!finalisedRelatives.isEmpty() && groupTemplate.size() == (groupTemplate.getGroupType().getID() / 10) + 1) {
                Member rel = finalisedRelatives.get(0);
                Member mem = null;
                if (groupTemplate.getAdjacentMembers(rel, MyLink.FAMILY_RELATIVE_INV.getLink()).size() > 0) {
                    mem = groupTemplate.getAdjacentMembers(rel, MyLink.FAMILY_RELATIVE_INV.getLink()).get(0);
                } else {
                    mem = rel;
                }

                if (AT.ST_M_MAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_M_MAR.getCatId()) {// If the family unit is
                    // couple with/without children - related to male
                    List<Member> relatives = groupTemplate.getAdjacentMembers(mem, MyLink.FAMILY_RELATIVE.getLink());
                    finalisedRelatives.removeAll(relatives);
                    List<Member> memPartnerList = groupTemplate.getAdjacentMembers(mem, MyLink.MALE_MARRIED.getLink());
                    if (!memPartnerList.isEmpty()) {// Because group may not be complete yet
                        Member memPartner = memPartnerList.get(0);
                        List<Member> partnerRelatives = groupTemplate.getAdjacentMembers(memPartner, MyLink.FAMILY_RELATIVE.getLink());
                        finalisedRelatives.removeAll(partnerRelatives);
                    }

                } else if (AT.ST_F_MAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_F_MAR.getCatId()) {// If the family unit
                    // is couple with/without children - related to
                    // female
                    List<Member> relatives = groupTemplate.getAdjacentMembers(mem, MyLink.FAMILY_RELATIVE.getLink());
                    finalisedRelatives.removeAll(relatives);
                    List<Member> memPartnerList = groupTemplate.getAdjacentMembers(mem, MyLink.FEMALE_MARRIED.getLink());
                    if (!memPartnerList.isEmpty()) {// Because group may not be complete yet
                        Member memPartner = memPartnerList.get(0);
                        List<Member> partnerRelatives = groupTemplate.getAdjacentMembers(memPartner, MyLink.FAMILY_RELATIVE.getLink());
                        finalisedRelatives.removeAll(partnerRelatives);
                    }
                } else if (AT.ST_M_LNPAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_F_LNPAR.getCatId()) {// If the family
                    // unit is lone parent
                    List<Member> relatives = groupTemplate.getAdjacentMembers(mem, MyLink.FAMILY_RELATIVE.getLink());
                    finalisedRelatives.removeAll(relatives);
                } else if (AT.ST_REL.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_REL.getCatId()) { // A relative of
                    // OTHER_FAMILY unit
                    List<Member> relatives = groupTemplate.getAdjacentMembers(mem, MyLink.FAMILY_RELATIVE.getLink());
                    for (Member familyMem : groupTemplate.getAdjacentMembers(mem, MyLink.RELATIVE_OTHER.getLink())) {
                        relatives.addAll(groupTemplate.getAdjacentMembers(familyMem, MyLink.FAMILY_RELATIVE.getLink()));
                    }
                    finalisedRelatives.removeAll(relatives);

                }
                if (!finalisedRelatives.isEmpty())
                    return Reject.YES;
            }

            return Reject.NO;
        }
    }

    /**
     * Reject if the current configuration is obviously not going to form the desired household type
     *
     * @author Bhagya N. Wickramasinghe
     */
    public static class HouseholdTypeBasedRejection extends RejectionCriterion {

        @Override
        public Reject validate(GroupTemplate groupTemplate) {

            // Lone parent family Hhs must not have couple with children units
            if (Arrays.asList(2, 6, 10, 16, 20, 24, 30, 34, 38, 44, 48, 52, 58, 62, 66, 72, 76, 80, 86, 90, 94, 100, 104, 108)
                      .contains(groupTemplate.getGroupType().getID())) {
                for (Member mem : groupTemplate.getAllMembers()) {
                    if ((AT.ST_M_MAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_M_MAR.getCatId()) || (AT.ST_F_MAR.getCatId
                            () <= mem
                            .getTypeID() && mem.getTypeID() <= AT.LS_F_MAR.getCatId())) {
                        List<Member> children = groupTemplate.getAdjacentMembers(mem, MyLink.LP_MALE_PARENTOF_CHILD.getLink());
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_FEMALE_PARENTOF_CHILD.getLink()));
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_MALE_PARENTOF_MLPCHILD.getLink()));
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_FEMALE_PARENTOF_MLPCHILD.getLink()));
                        if (!children.isEmpty()) {
                            return Reject.YES;
                        }
                    }
                }

            }
            // Couple Only Hhs must not have couple with children or lone parent units
            if (Arrays.asList(0, 4, 8).contains(groupTemplate.getGroupType().getID() % 14)) {
                for (Member mem : groupTemplate.getAllMembers()) {
                    if ((AT.ST_M_MAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_M_MAR.getCatId()) || (AT.ST_F_MAR.getCatId
                            () <= mem
                            .getTypeID() && mem.getTypeID() <= AT.LS_F_MAR.getCatId())
                            || (AT.ST_M_LNPAR.getCatId() <= mem.getTypeID() && mem.getTypeID() <= AT.LS_F_LNPAR.getCatId())) {
                        List<Member> children = groupTemplate.getAdjacentMembers(mem, MyLink.LP_MALE_PARENTOF_CHILD.getLink());
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_FEMALE_PARENTOF_CHILD.getLink()));
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_MALE_PARENTOF_MLPCHILD.getLink()));
                        children.addAll(groupTemplate.getAdjacentMembers(mem, MyLink.LP_FEMALE_PARENTOF_MLPCHILD.getLink()));
                        if (!children.isEmpty()) {
                            return Reject.YES;
                        }
                    }
                }
            }

            return Reject.NO;
        }

    }
}
