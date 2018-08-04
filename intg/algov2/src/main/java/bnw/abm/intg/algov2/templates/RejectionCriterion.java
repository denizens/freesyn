package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;

abstract public class RejectionCriterion {
    private static List<RejectionCriterion> criteria = new ArrayList<>(2);

    public enum Reject {
        YES,
        NO;
    }

    public static Reject validateAll(GroupTemplate groupTemplate) {
        Reject result = Reject.NO;
        for (RejectionCriterion criterion : criteria) {
            result = criterion.validate(groupTemplate);
            if (result == Reject.YES) {
                return Reject.YES;
            }
        }
        return Reject.NO;
    }

    public boolean register() {
        if (!criteria.contains(this))
            return criteria.add(this);
        else
            return false;
    }

    public boolean unregister() {
        return criteria.remove(this);
    }

    abstract public Reject validate(GroupTemplate groupTemplate);
}
