package org.tbc.world.content;

/**
 * SharedDefines.h SkillType + Item.cpp item_weapon_skills[MAX_ITEM_SUBCLASS_WEAPON].
 * ITEM_CLASS_WEAPON = 2.
 */
public final class WeaponSkills {
    public static final int ITEM_CLASS_WEAPON = 2;
    public static final int ITEM_SUBCLASS_WEAPON_FISHING_POLE = 20;

    public static final int SKILL_SWORDS = 43;
    public static final int SKILL_AXES = 44;
    public static final int SKILL_BOWS = 45;
    public static final int SKILL_GUNS = 46;
    public static final int SKILL_MACES = 54;
    public static final int SKILL_2H_SWORDS = 55;
    public static final int SKILL_DEFENSE = 95;
    public static final int SKILL_STAVES = 136;
    public static final int SKILL_2H_MACES = 160;
    public static final int SKILL_UNARMED = 162;
    public static final int SKILL_2H_AXES = 172;
    public static final int SKILL_DAGGERS = 173;
    public static final int SKILL_THROWN = 176;
    public static final int SKILL_CROSSBOWS = 226;
    public static final int SKILL_WANDS = 228;
    public static final int SKILL_POLEARMS = 229;
    public static final int SKILL_ASSASSINATION = 253;
    public static final int SKILL_FISHING = 356;

    /** CMaNGOS CONFIG_UINT32_SKILL_GAIN_WEAPON / DEFENSE default. */
    public static final int SKILL_GAIN_WEAPON = 1;
    public static final int SKILL_GAIN_DEFENSE = 1;

    private static final int[] WEAPON_SUBCLASS_SKILL = {
            SKILL_AXES, SKILL_2H_AXES, SKILL_BOWS, SKILL_GUNS, SKILL_MACES,
            SKILL_2H_MACES, SKILL_POLEARMS, SKILL_SWORDS, SKILL_2H_SWORDS, 0,
            SKILL_STAVES, 0, 0, SKILL_UNARMED, 0,
            SKILL_DAGGERS, SKILL_THROWN, SKILL_ASSASSINATION, SKILL_CROSSBOWS, SKILL_WANDS,
            SKILL_FISHING
    };

    private WeaponSkills() {
    }

    /** Item::GetSkill for ITEM_CLASS_WEAPON; 0 if unknown subclass. */
    public static int skillForWeaponSubclass(int subClass) {
        if (subClass < 0 || subClass >= WEAPON_SUBCLASS_SKILL.length) {
            return 0;
        }
        return WEAPON_SUBCLASS_SKILL[subClass];
    }
}
