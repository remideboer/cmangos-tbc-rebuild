package org.tbc.world.classless;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tunable classless rules. Category→step mapping is separate from per-step effects
 * so equipment handling does not hard-code percentages.
 */
public final class ClasslessConfig {
    /** ChrClasses.dbc unused TBC id — death knight slot. */
    public static final int CLASS_CLASSLESS = 6;

    public static final int AUTO_ATTACK = 6603;
    /** Worn Shortsword — starter weapon without sword proficiency. */
    public static final int STARTER_WEAPON = 25;
    /** Cloth armor proficiency mask (1 &lt;&lt; ITEM_SUBCLASS_ARMOR_CLOTH). */
    public static final int ARMOR_CLOTH_MASK = 1 << 1;
    /** Unarmed weapon proficiency mask (1 &lt;&lt; ITEM_SUBCLASS_WEAPON_UNARMED). */
    public static final int WEAPON_UNARMED_MASK = 1 << 13;

    /** Armor subclass: cloth / leather / mail / plate. */
    public static final int ARMOR_CLOTH = 1;
    public static final int ARMOR_LEATHER = 2;
    public static final int ARMOR_MAIL = 3;
    public static final int ARMOR_PLATE = 4;

    private static volatile ClasslessConfig instance = defaults();

    private final boolean enabled;
    /** Per-step armor contribution reduction (index = step 0..3). */
    private final float[] armorReductionPct;
    /** Per-step item Str/Agi reduction. */
    private final float[] strAgiReductionPct;
    /** Per-step move-speed reduction (summed across unproficient pieces, clamped). */
    private final float[] speedReductionPct;
    /** Flat miss-chance add (percent points) when swinging an unproficient weapon. */
    private final double untrainedWeaponMissAddPct;
    /** Optional damage multiplier for untrained weapons; 1.0 = off. */
    private final float untrainedWeaponDamageMult;
    private final Set<Integer> trainerEligibleSpells;
    private final int baseHealth;
    private final int baseMana;
    private final int str;
    private final int agi;
    private final int sta;
    private final int inte;
    private final int spi;

    public ClasslessConfig(
            boolean enabled,
            float[] armorReductionPct,
            float[] strAgiReductionPct,
            float[] speedReductionPct,
            double untrainedWeaponMissAddPct,
            float untrainedWeaponDamageMult,
            Set<Integer> trainerEligibleSpells,
            int baseHealth,
            int baseMana,
            int str,
            int agi,
            int sta,
            int inte,
            int spi) {
        this.enabled = enabled;
        this.armorReductionPct = armorReductionPct.clone();
        this.strAgiReductionPct = strAgiReductionPct.clone();
        this.speedReductionPct = speedReductionPct.clone();
        this.untrainedWeaponMissAddPct = untrainedWeaponMissAddPct;
        this.untrainedWeaponDamageMult = untrainedWeaponDamageMult;
        this.trainerEligibleSpells = Collections.unmodifiableSet(new LinkedHashSet<>(trainerEligibleSpells));
        this.baseHealth = baseHealth;
        this.baseMana = baseMana;
        this.str = str;
        this.agi = agi;
        this.sta = sta;
        this.inte = inte;
        this.spi = spi;
    }

    public static ClasslessConfig get() {
        return instance;
    }

    public static void set(ClasslessConfig config) {
        instance = config == null ? defaults() : config;
    }

    public static void reset() {
        instance = defaults();
    }

    public static boolean isClasslessId(int clazz) {
        return clazz == CLASS_CLASSLESS;
    }

    /** Locked v1 oracle from Slice 35 plan. */
    public static ClasslessConfig defaults() {
        Set<Integer> eligible = new LinkedHashSet<>();
        eligible.add(6673); // Battle Shout — warrior starter trainer
        return new ClasslessConfig(
                true,
                new float[]{0f, 0.10f, 0.20f, 0.30f},
                new float[]{0f, 0.10f, 0.20f, 0.30f},
                new float[]{0f, 0.05f, 0.10f, 0.15f},
                5.0,
                1.0f,
                eligible,
                20,
                100,
                20,
                20,
                22,
                20,
                20);
    }

    public ClasslessConfig withEnabled(boolean on) {
        return new ClasslessConfig(
                on,
                armorReductionPct,
                strAgiReductionPct,
                speedReductionPct,
                untrainedWeaponMissAddPct,
                untrainedWeaponDamageMult,
                trainerEligibleSpells,
                baseHealth,
                baseMana,
                str,
                agi,
                sta,
                inte,
                spi);
    }

    public boolean enabled() {
        return enabled;
    }

    public int armorStep(int armorSubClass) {
        return switch (armorSubClass) {
            case ARMOR_CLOTH -> 0;
            case ARMOR_LEATHER -> 1;
            case ARMOR_MAIL -> 2;
            case ARMOR_PLATE -> 3;
            default -> 0;
        };
    }

    public float armorReductionForStep(int step) {
        return reduction(armorReductionPct, step);
    }

    public float strAgiReductionForStep(int step) {
        return reduction(strAgiReductionPct, step);
    }

    public float speedReductionForStep(int step) {
        return reduction(speedReductionPct, step);
    }

    private static float reduction(float[] table, int step) {
        if (step < 0 || step >= table.length) {
            return 0f;
        }
        return table[step];
    }

    public double untrainedWeaponMissAddPct() {
        return untrainedWeaponMissAddPct;
    }

    public float untrainedWeaponDamageMult() {
        return untrainedWeaponDamageMult;
    }

    public boolean trainerSpellEligible(int spellId) {
        return trainerEligibleSpells.contains(spellId);
    }

    public Set<Integer> trainerEligibleSpells() {
        return trainerEligibleSpells;
    }

    public int baseHealth() {
        return baseHealth;
    }

    public int baseMana() {
        return baseMana;
    }

    public int str() {
        return str;
    }

    public int agi() {
        return agi;
    }

    public int sta() {
        return sta;
    }

    public int inte() {
        return inte;
    }

    public int spi() {
        return spi;
    }
}
