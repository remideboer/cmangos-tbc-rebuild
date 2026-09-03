package org.tbc.world.pvp;

/** Timers and world-state ids from spec battleground-av/ab/ey and outdoor-pvp. Do not invent. */
public final class PvpObjectives {
    public static final int AV_CAP_MS = 240_000;
    public static final int AV_SNOWFALL_MS = 300_000;
    public static final int AB_CONTEST_MS = 60_000;
    public static final int[] AB_TICK_MS = {0, 12_000, 9_000, 6_000, 3_000, 1_000};
    public static final int AB_STABLES = 180087;
    public static final int AB_BLACKSMITH = 180088;
    public static final int EY_FLAG_AURA = 34976;
    public static final int SILITHYST_MAX = 200;
    public static final int SILITHYST_WIN = 30754;
    public static final int WS_SILITHYST_A = 2313;
    public static final int WS_SILITHYST_H = 2314;
    public static final int TEROKKAR_BLESSING = 33377;
    public static final int HALAA_BUFF = 33795;
    public static final int HALAA_GY = 993;
    public static final int IDLE_AFK = 43680;
    public static final int GHOST_AURA = 8326;
    public static final int SICKNESS = 15007;
    public static final int TALENT_WIPE = 14867;
    public static final int MOUNT_AURA = 78;
    public static final int WSG_FLAG_A = 23333;
    public static final int WSG_FLAG_H = 23335;
    public static final int WS_WSG_A = 1545;
    public static final int WS_WSG_H = 1546;
    public static final int WS_HF_TOWER_A = 2476;
    public static final int WS_HF_TOWER_H = 2478;
    public static final int WS_AB_RES_A = 1776;
    public static final int WS_AB_RES_H = 1777;
    public static final int WS_AB_BLACKSMITH_A = 1782;
    public static final int WS_AB_BLACKSMITH_H = 1783;
    public static final int WS_AB_BLACKSMITH_CONT_A = 1784;
    public static final int WS_AB_BLACKSMITH_CONT_H = 1785;
    /** battleground-ab.md Stables node 0 */
    public static final int WS_AB_STABLES_OCC_A = 1767;
    public static final int WS_AB_STABLES_OCC_H = 1768;
    public static final int WS_AB_STABLES_CONT_A = 1769;
    public static final int WS_AB_STABLES_CONT_H = 1770;
    public static final int WS_AV_SCORE_A = 3127;
    public static final int WS_AV_SCORE_H = 3128;
    public static final int[] EY_FLAG_POINTS = {75, 85, 100, 500};
    public static final int TIMER_TF_LOCK_MS = 6 * 60 * 60 * 1000;
    public static final int WS_TF_LOCK_A = 2767;
    public static final int WS_TF_LOCK_H = 2768;
    public static final int WS_EP_NORTHPASS_A = 2372;
    public static final int WS_EP_NORTHPASS_H = 2373;
    public static final int WS_EP_NORTHPASS_N = 2352;
    public static final int GO_EP_NORTHPASS = 181899;
    public static final int GO_ZM_EAST = 182523;
    public static final int WS_ZM_EAST_A = 2558;
    public static final int WS_ZM_EAST_H = 2559;
    public static final int WS_ZM_EAST_N = 2560;
    public static final int WS_EY_RES_A = 2749;
    public static final int WS_EY_RES_H = 2750;
    public static final int WS_EY_TOWERS_A = 2752;
    public static final int WS_EY_TOWERS_H = 2753;
    public static final int HALAA_GUARDS = 15;
    public static final int GO_HALAA_BANNER = 182210;
    /** outdoor-pvp.md Nagrand controlled A/H/N */
    public static final int WS_HALAA_A = 2673;
    public static final int WS_HALAA_H = 2672;
    public static final int WS_HALAA_N = 2671;

    public static int avCaptureMs(boolean snowfallFirstClaim) {
        return snowfallFirstClaim ? AV_SNOWFALL_MS : AV_CAP_MS;
    }

    public static int abTickMs(int ownedNodes) {
        int i = ownedNodes;
        if (i < 0) {
            i = 0;
        }
        if (i >= AB_TICK_MS.length) {
            i = AB_TICK_MS.length - 1;
        }
        return AB_TICK_MS[i];
    }

    public static int eyFlagPoints(int towersOwned) {
        if (towersOwned <= 0) {
            return 0;
        }
        int i = towersOwned - 1;
        if (i >= EY_FLAG_POINTS.length) {
            i = EY_FLAG_POINTS.length - 1;
        }
        return EY_FLAG_POINTS[i];
    }

    private PvpObjectives() {}
}
