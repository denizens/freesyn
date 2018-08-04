package bnw.abm.intg.apps.latch.algov2;

/**
 * @author wniroshan 17 May 2018
 */
public enum AT {
    ST_M_MAR(0),
    LS_M_MAR(6),
    ST_F_MAR(8),
    LS_F_MAR(15),
    ST_M_LNPAR(16),
    LS_M_LNPAR(22),
    ST_F_LNPAR(24),
    LS_F_LNPAR(30),
    ST_M_CHLD(32),
    LS_M_CHLD(39),
    ST_F_CHLD(40),
    LS_F_CHLD(47),
    ST_M_GRP(48),
    LS_M_GRP(54),
    ST_F_GRP(56),
    LS_F_GRP(62),
    ST_M_1P(64),
    LS_M_1P(70),
    ST_F_1P(72),
    LS_F_1P(78),
    ST_REL(80),
    LS_REL(95);

    int catId;

    AT(int catId) {
        this.catId = catId;
    }

    public int getCatId() {
        return this.catId;
    }
}
