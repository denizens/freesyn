package bnw.abm.intg.apps.latch.ipucensus;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.LinkCondition;
import com.google.common.collect.HashBasedTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IPUDataLinkConditions {
    public static class InverseLinks extends LinkCondition {

        Map<LinkType, LinkType> inversions;

        public InverseLinks() {
            this.inversions = new HashMap<>();
            inversions.put(LinkType.NONE, LinkType.NONE);
            inversions.put(MyLink.MALE_MARRIED.getLink(), MyLink.FEMALE_MARRIED.getLink());
            inversions.put(MyLink.FEMALE_MARRIED.getLink(), MyLink.MALE_MARRIED.getLink());
            inversions.put(MyLink.M_MALE_PARENTOF_CHILD.getLink(), MyLink.CHILDOF.getLink());
            inversions.put(MyLink.M_FEMALE_PARENTOF_CHILD.getLink(), MyLink.CHILDOF.getLink());
            // inversions.put(LinkType.getInstance("ChildOf"), LinkType.getInstance("MParentOf"));
            inversions.put(MyLink.LP_MALE_PARENTOF_CHILD.getLink(), MyLink.CHILDOF.getLink());
            inversions.put(MyLink.LP_FEMALE_PARENTOF_CHILD.getLink(), MyLink.CHILDOF.getLink());
            inversions.put(MyLink.M_MALE_PARENTOF_MLPCHILD.getLink(), MyLink.MLP_CHILDOF.getLink());
            inversions.put(MyLink.M_FEMALE_PARENTOF_MLPCHILD.getLink(), MyLink.MLP_CHILDOF.getLink());
            inversions.put(MyLink.LP_MALE_PARENTOF_MLPCHILD.getLink(), MyLink.MLP_CHILDOF.getLink());
            inversions.put(MyLink.LP_FEMALE_PARENTOF_MLPCHILD.getLink(), MyLink.MLP_CHILDOF.getLink());
            inversions.put(MyLink.FAMILY_RELATIVE.getLink(), MyLink.FAMILY_RELATIVE_INV.getLink());
            inversions.put(MyLink.FAMILY_RELATIVE_INV.getLink(), MyLink.FAMILY_RELATIVE.getLink());
            // inversions.put(LinkType.getInstance("RelativeOf"), LinkType.getInstance("F-RelativeOf"));
            inversions.put(MyLink.RELATIVE_OTHER.getLink(), MyLink.RELATIVE_OTHER.getLink());
            inversions.put(MyLink.GROUPHOUSEHOLD.getLink(), MyLink.GROUPHOUSEHOLD.getLink());
        }

        @Override
        protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {
            LinkType inverseLinkType = null;
            if (linkType.isSameType(MyLink.CHILDOF.getLink())) {
                if (AT.ST_M_MAR.getCatId() <= targets.get(0).getTypeID() & targets.get(0).getTypeID() <= AT.LS_M_MAR.getCatId()) {
                    inverseLinkType = MyLink.M_MALE_PARENTOF_CHILD.getLink();
                } else if (AT.ST_F_MAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0).getTypeID() <= AT.LS_F_MAR.getCatId()) {
                    inverseLinkType = MyLink.M_FEMALE_PARENTOF_CHILD.getLink();
                } else if (AT.ST_M_LNPAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0)
                                                                                            .getTypeID() <= AT.LS_M_LNPAR.getCatId()) {
                    inverseLinkType = MyLink.LP_MALE_PARENTOF_CHILD.getLink();
                } else if (AT.ST_F_LNPAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0)
                                                                                            .getTypeID() <= AT.LS_F_LNPAR.getCatId()) {
                    inverseLinkType = MyLink.LP_FEMALE_PARENTOF_CHILD.getLink();
                } else {
                    throw new IllegalStateException("Cannot resolve inverse link for [" + reference + "].[" + linkType + "]." + targets);
                }
            } else if (linkType.isSameType(MyLink.MLP_CHILDOF.getLink())) {
                if (AT.ST_M_MAR.getCatId() <= targets.get(0).getTypeID() & targets.get(0).getTypeID() <= AT.LS_M_MAR.getCatId()) {
                    inverseLinkType = MyLink.M_MALE_PARENTOF_MLPCHILD.getLink();
                } else if (AT.ST_F_MAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0).getTypeID() <= AT.LS_F_MAR.getCatId()) {
                    inverseLinkType = MyLink.M_FEMALE_PARENTOF_MLPCHILD.getLink();
                } else if (AT.ST_M_LNPAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0)
                                                                                            .getTypeID() <= AT.LS_M_LNPAR.getCatId()) {
                    inverseLinkType = MyLink.LP_MALE_PARENTOF_MLPCHILD.getLink();
                } else if (AT.ST_F_LNPAR.getCatId() <= targets.get(0).getTypeID() && targets.get(0)
                                                                                            .getTypeID() <= AT.LS_F_LNPAR.getCatId()) {
                    inverseLinkType = MyLink.LP_FEMALE_PARENTOF_MLPCHILD.getLink();
                } else {
                    throw new IllegalStateException("Cannot resolve inverse link for [" + reference + "].[" + linkType + "]." + targets);
                }
            } else {
                inverseLinkType = inversions.get(linkType);
            }
            for (Member targetMember : targets)
                groupTemplate.put(targetMember, inverseLinkType, reference);
        }
    }

    public static class DependentLinks extends LinkCondition {

        HashBasedTable<LinkType, LinkType, LinkType> dependentLinkTypes;

        public DependentLinks() {
            // |.......newlink..................|.......existing............|.......dependent...........|
            // |--------------------------------|---------------------------|---------------------------|
            // |[Ref].M-Married.[Tar]...........|.[Ref].ParentOf.[Ext]......|.[Tar].ParentOf.[Ext]......|
            // |[Ref].M-ParentOf.[Tar]..........|.[Ref].Married.[Ext].......|.[Tar].ChildOf.[Ext].......|
            // |[Ref].M-ParentOf-MLPChild.[Tar].|.[Ref].Married.[Ext].......|.[Tar].MLP-ChildOf.[Ext]...|
            // |[Ref].ChildOf.[Tar].............|.[Ref].ChildOf.[Ext].......|.[Tar].Married.[Ext].......|
            // |[Ref].GroupHhold.[Tar]..........|.[Ref].GroupHhol.[Ext].....|.[Tar].GroupHhold.[Ext]....|
            // |[Ref].RelativeOther.[Tar].......|.[Ref].RelativeOther.[Ext].|.[Tar].RelativeOther.[Ext].|

            dependentLinkTypes = HashBasedTable.create();
            dependentLinkTypes.put(MyLink.MALE_MARRIED.getLink(), MyLink.M_MALE_PARENTOF_CHILD.getLink(),

                                   MyLink.M_FEMALE_PARENTOF_CHILD.getLink());
            dependentLinkTypes.put(MyLink.FEMALE_MARRIED.getLink(), MyLink
                                           .M_FEMALE_PARENTOF_CHILD.getLink(),
                                   MyLink.M_MALE_PARENTOF_CHILD.getLink());
            dependentLinkTypes.put(MyLink.M_MALE_PARENTOF_CHILD.getLink(), MyLink.MALE_MARRIED.getLink(), MyLink.CHILDOF.getLink());
            dependentLinkTypes.put(MyLink.M_FEMALE_PARENTOF_CHILD.getLink(), MyLink.FEMALE_MARRIED.getLink(), MyLink.CHILDOF.getLink());
            dependentLinkTypes.put(MyLink.M_MALE_PARENTOF_MLPCHILD.getLink(), MyLink.MALE_MARRIED.getLink(), MyLink.MLP_CHILDOF.getLink());
            dependentLinkTypes.put(MyLink.M_FEMALE_PARENTOF_MLPCHILD.getLink(),
                                   MyLink.FEMALE_MARRIED.getLink(),
                                   MyLink.MLP_CHILDOF.getLink());
            // dependentLinkTypes.put(LinkType.getInstance("ChildOf"), LinkType.getInstance("ChildOf"), LinkType.getInstance("Married"));
            dependentLinkTypes.put(MyLink.GROUPHOUSEHOLD.getLink(), MyLink
                    .GROUPHOUSEHOLD.getLink(), MyLink.GROUPHOUSEHOLD.getLink());
            dependentLinkTypes.put(MyLink.RELATIVE_OTHER.getLink(), MyLink.RELATIVE_OTHER.getLink(), MyLink.RELATIVE_OTHER.getLink());
        }

        @Override
        protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType newLinkType, List<Member> targets) {
            for (LinkType existingLinkOfRef : groupTemplate.getAdjacentMembers(reference).keySet()) {
                LinkType dependentLink = dependentLinkTypes.get(newLinkType, existingLinkOfRef);
                if (dependentLink != null) {
                    for (Member target : targets) {
                        List<Member> existingRelativesOfRef = groupTemplate.getAdjacentMembers(reference, existingLinkOfRef);
                        existingRelativesOfRef.removeIf(ext -> target == ext);
                        if (!existingRelativesOfRef.isEmpty())
                            groupTemplate.putAll(target, dependentLink, existingRelativesOfRef);
                    }
                }
            }
        }
    }
}
