package bnw.abm.intg.latch.testcase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.templates.LinkCondition;

import com.google.common.collect.HashBasedTable;

public class LinkConditionsBuilder {
    public static class InverseLink extends LinkCondition {

        Map<LinkType, LinkType> inversions;

        public InverseLink() {
            this.inversions = new HashMap<>();
            inversions.put(LinkType.NONE, LinkType.NONE);
            inversions.put(LinkType.getInstance("Married"), LinkType.getInstance("Married"));
            inversions.put(LinkType.getInstance("ParentOf"), LinkType.getInstance("ChildOf"));
            inversions.put(LinkType.getInstance("ChildOf"), LinkType.getInstance("ParentOf"));
        }

        @Override
        protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {
            LinkType invesrseLinkType = inversions.get(linkType);
            for (Member targetMember : targets)
                groupTemplate.put(targetMember, invesrseLinkType, reference);
        }
    }

    public static class DependentLinks extends LinkCondition {
        HashBasedTable<LinkType, LinkType, LinkType> dependentLinkTypes;

        public DependentLinks() {
            // |.......newlink.......|.......existing.......|.......dependent......|
            // |---------------------|----------------------|----------------------|
            // |[Ref].Married.[Tar]..|.[Ref].ParentOf.[Ext].|.[Tar].ParentOf.[Ext].|
            // |[Ref].ParentOf.[Tar].|.[Ref].Married.[Ext]..|.[Tar].ChildOf.[Ext]..|
            // |[Ref].ChildOf.[Tar]..|.[Ref].ChildOf.[Ext]..|.[Tar].Married.[Ext]..|
            // |[Ref].none.[Tar].....|.[Ref].none.[Ext].....|.[Tar].none.[Ext].....|

            dependentLinkTypes = HashBasedTable.create();
            dependentLinkTypes.put(LinkType.getInstance("Married"), LinkType.getInstance("ParentOf"), LinkType.getInstance("ParentOf"));
            dependentLinkTypes.put(LinkType.getInstance("ParentOf"), LinkType.getInstance("Married"), LinkType.getInstance("ChildOf"));
            dependentLinkTypes.put(LinkType.getInstance("ChildOf"), LinkType.getInstance("ChildOf"), LinkType.getInstance("Married"));
            dependentLinkTypes.put(LinkType.NONE, LinkType.NONE, LinkType.NONE);
            // dependentLinkTypes.put(LinkType.getInstance("ChildOf"), LinkType.getInstance("none"), LinkType.getInstance("ParentOf"));
            // dependentLinkTypes.put(LinkType.getInstance("none"), LinkType.getInstance("ChildOf"), LinkType.getInstance("ChildOf"));
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