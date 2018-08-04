package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;

/**
 * Abstract class to be used for specifying population's constraints like Inverse links and Dependent links
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
public abstract class LinkCondition {
    private static List<LinkCondition> constraints= new ArrayList<>();

   

    /**
     * Registers the new constrains. Call this method for each instance of LinkCondition's subclass.
     */
    public final void register() {
        constraints.add(this);
    }

    /**
     * Apply all the registered constraints. Intended for internal use.
     * 
     * @param groupTemplate
     *            Group Template instance that was just changed
     * @param reference
     *            The reference agent type instance involved in the change
     * @param linkType
     *            The link type instance involved in the change
     * @param targets
     *            The target agent instance that was involved in the change
     */
    public final static void applyAll(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {
        for (LinkCondition condition : constraints) {
            condition.applyCondition(groupTemplate, reference, linkType, targets);
        }
    }

    /**
     * Subclass LinkCondition class and implement this method to add a new constraint to the population. This method is internally called
     * every time the group template is changed. reference, linkType and target are the entities that were just added to groupTemplate (They are
     * already in the groupTemplate). You need to specify what changes should happen to groupTemplate because of these new additions. If any of
     * the changes specified in this method violates ValidationCriterion of the population, groupTemplate is discarded. To enable this method call register()
     * after instantiating LinkCondition class's subclass.
     * 
     * @param groupTemplate
     *            Group Template that was just updated
     * @param reference
     *            The reference Member that was involved in the update
     * @param linkType
     *            The link type that was added to the reference agent type
     * @param targets
     *            The new target agent that was added to the group template
     */
    protected abstract void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets);
    // This will call groupTemplate.add();
}
