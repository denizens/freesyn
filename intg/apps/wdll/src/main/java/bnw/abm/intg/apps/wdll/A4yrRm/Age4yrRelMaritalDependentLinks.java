package bnw.abm.intg.apps.wdll.A4yrRm;

import java.util.List;

import com.google.common.collect.HashBasedTable;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.templates.LinkCondition;

public class Age4yrRelMaritalDependentLinks extends LinkCondition {

    HashBasedTable<LinkType, LinkType, LinkType> dependentLinkTypes;
    private final int maxMaleIndex;

    /**
     * Constructs dependent links link condition instance
     * 
     * @param maxMaleIndex
     *            maximum male index (inclusive)
     */
    public Age4yrRelMaritalDependentLinks(int maxMaleIndex) {
        this.maxMaleIndex = maxMaleIndex;
        // |.......newlink............|.......existing.............|.......dependent..........................|
        // |--------------------------|----------------------------|------------------------------------------|
        // |[Ref].Married.[Tar].......|.[Ref].ParentOf.[Ext].......|.[Tar].ParentOf.[Ext].....................|
        // |[Ref].ParentOf.[Tar]......|.[Ref].Married.[Ext]........|.[Tar].ChildOfMother|ChildOfFather.[Ext]..|
        // |[Ref].ChildOfMother.[Tar].|.[Ref].ChildOfFather.[Ext]..|.[Tar].Married.[Ext]......................|
        // |[Ref].ChildOfFather.[Tar].|.[Ref].ChildOfMother.[Ext]..|.[Tar].Married.[Ext]......................|

        dependentLinkTypes = HashBasedTable.create();
        dependentLinkTypes.put(LinkType.getInstance("Married"), LinkType.getInstance("ParentOf"), LinkType.getInstance("ParentOf"));
        dependentLinkTypes.put(LinkType.getInstance("ParentOf"), LinkType.getInstance("Married"), LinkType.getInstance("ChildOfMother"));
        dependentLinkTypes.put(LinkType.getInstance("ChildOfMother"), LinkType.getInstance("ChildOfFather"), LinkType.getInstance("Married"));
        dependentLinkTypes.put(LinkType.getInstance("ChildOfFather"), LinkType.getInstance("ChildOfMother"), LinkType.getInstance("Married"));
    }

    @Override
    protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {

        for (LinkType existingLinkOfRef : groupTemplate.getAdjacentMembers(reference).keySet()) {
            LinkType dependentLink = dependentLinkTypes.get(linkType, existingLinkOfRef);
            if (dependentLink != null) {
                List<Member> existingRelativesOfRef = groupTemplate.getAdjacentMembers(reference, existingLinkOfRef);
                existingRelativesOfRef.removeIf(ext -> targets.contains(ext));
                if (existingRelativesOfRef.isEmpty()) {
                    continue;
                }
                if (dependentLink.isSameType(LinkType.getExisting("ChildOfMother"))) {
                    if (existingRelativesOfRef.get(0).getTypeID() <= this.maxMaleIndex) {
                        // parent is male
                        dependentLink = LinkType.getExisting("ChildOfFather");
                    } else {
                        // parent is female
                        dependentLink = LinkType.getExisting("ChildOfMother");
                    }
                }
                for (Member target : targets) {
                    groupTemplate.putAll(target, dependentLink, existingRelativesOfRef);
                }
            }
        }
    }

}