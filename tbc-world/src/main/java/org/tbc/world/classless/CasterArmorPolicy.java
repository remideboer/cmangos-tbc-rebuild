package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.catalog.ItemCatalog;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.spell.SpellEngine.SpellInfo;

/**
 * Hero: mage/warlock/priest-style magic halves in effectiveness for each armor step
 * above the Battlecaster-unlocked baseline (cloth → leather → mail → plate).
 * Stealth is 1/3 as effective in mail/plate (observers within detect yards see the player).
 */
public final class CasterArmorPolicy {
    /**
     * Custom trainer passives (not Spell.dbc). Leather / mail / plate Battlecaster ranks.
     */
    public static final int SPELL_BATTLECASTER_LEATHER = 900_101;
    public static final int SPELL_BATTLECASTER_MAIL = 900_102;
    public static final int SPELL_BATTLECASTER_PLATE = 900_103;

    /** Trainer reqLevel: floor(MAX_LEVEL / 8) = 8 at 70. */
    public static final int REQ_LEVEL_BATTLECASTER_LEATHER = Player.MAX_LEVEL / 8;
    /** floor(MAX_LEVEL / 4) = 17. */
    public static final int REQ_LEVEL_BATTLECASTER_MAIL = Player.MAX_LEVEL / 4;
    /** floor(MAX_LEVEL / 2) = 35. */
    public static final int REQ_LEVEL_BATTLECASTER_PLATE = Player.MAX_LEVEL / 2;

    public static final int TRAINER_COST_BATTLECASTER = 100;

    /**
     * With mail/plate, stealthed Heroes are revealed within this 2D range
     * (cloth/leather stealth stays fully hidden).
     */
    public static final float HEAVY_ARMOR_STEALTH_DETECT_YARDS = 15f;

    /** Spell school mask bits (and small index values used in some seeds). */
    public static final int SCHOOL_MASK_HOLY = 0x2;
    public static final int SCHOOL_MASK_FIRE = 0x4;
    public static final int SCHOOL_MASK_FROST = 0x10;
    public static final int SCHOOL_MASK_SHADOW = 0x20;
    public static final int SCHOOL_MASK_ARCANE = 0x40;
    public static final int CASTER_SCHOOL_MASK =
            SCHOOL_MASK_HOLY | SCHOOL_MASK_FIRE | SCHOOL_MASK_FROST | SCHOOL_MASK_SHADOW | SCHOOL_MASK_ARCANE;

    private CasterArmorPolicy() {
    }

    /** Highest cloth/leather/mail/plate step among equipped armor (0 if none / cloth only). */
    public static int heaviestArmorStep(Player p, ItemCatalog mgr) {
        if (p == null || mgr == null) {
            return 0;
        }
        int heaviest = 0;
        for (int slot = 0; slot < Player.EQUIPMENT_SLOT_END; slot++) {
            Item it = p.itemAt(0, slot);
            if (it == null) {
                continue;
            }
            ObjectMgr.ItemTemplate t = mgr.item(it.entry);
            if (t == null || t.itemClass != Player.ITEM_CLASS_ARMOR) {
                continue;
            }
            int sub = t.subClass;
            if (sub < ClasslessConfig.ARMOR_CLOTH || sub > ClasslessConfig.ARMOR_PLATE) {
                continue;
            }
            heaviest = Math.max(heaviest, ClasslessConfig.get().armorStep(sub));
        }
        return heaviest;
    }

    /**
     * Battlecaster unlock step: 0 cloth-only, 1 leather, 2 mail, 3 plate.
     * Higher ranks include lower (plate implies mail+leather baseline).
     */
    public static int battlecasterUnlockedStep(Player p) {
        if (p == null || p.spells == null) {
            return 0;
        }
        if (p.spells.contains(SPELL_BATTLECASTER_PLATE)) {
            return 3;
        }
        if (p.spells.contains(SPELL_BATTLECASTER_MAIL)) {
            return 2;
        }
        if (p.spells.contains(SPELL_BATTLECASTER_LEATHER)) {
            return 1;
        }
        return 0;
    }

    /** Steps of penalty after Battlecaster: max(0, worn − unlocked). */
    public static int effectivePenaltyStep(Player p, ItemCatalog mgr) {
        return Math.max(0, heaviestArmorStep(p, mgr) - battlecasterUnlockedStep(p));
    }

    /** Relative to unlocked baseline: 1, 1/2, 1/4, 1/8, … */
    public static float casterEffectiveness(Player p, ItemCatalog mgr) {
        int step = effectivePenaltyStep(p, mgr);
        if (step <= 0) {
            return 1f;
        }
        if (step >= 31) {
            return 0f;
        }
        return 1f / (1 << step);
    }

    /**
     * Mage/warlock/priest schools: holy/fire/frost/shadow/arcane (mask or index form).
     * Nature and physical are excluded.
     */
    public static boolean isCasterSchool(int school) {
        if (school <= 0) {
            return false;
        }
        // Small values: school index (1 holy … 6 arcane). Nature index 3 excluded.
        if (school <= 6) {
            return school == 1 || school == 2 || school == 4 || school == 5 || school == 6;
        }
        // Larger: Spell.dbc SchoolMask bits.
        return (school & CASTER_SCHOOL_MASK) != 0;
    }

    public static int scaleCasterAmount(Player caster, int amount, SpellInfo sp, ItemCatalog mgr) {
        if (amount <= 0 || sp == null || !ClasslessCharacterPolicy.isClassless(caster)) {
            return amount;
        }
        if (!isCasterSchool(sp.school())) {
            return amount;
        }
        float mult = casterEffectiveness(caster, mgr);
        if (mult >= 1f) {
            return amount;
        }
        return Math.max(1, Math.round(amount * mult));
    }

    /** 1.0 cloth/leather; 1/3 in mail or plate (worn step ≥ 2). */
    public static float stealthEffectiveness(Player p, ItemCatalog mgr) {
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return 1f;
        }
        if (heaviestArmorStep(p, mgr) >= 2) {
            return 1f / 3f;
        }
        return 1f;
    }

    /**
     * Visibility including Hero mail/plate stealth reveal (1/3 effectiveness → detect yards).
     */
    public static boolean visibleTo(Unit subject, Unit observer, ItemCatalog mgr) {
        if (subject == null || observer == null) {
            return true;
        }
        if (observer.guid == subject.guid) {
            return true;
        }
        if (subject.visibility() == org.tbc.world.entity.Unit.Visibility.GROUP_STEALTH
                && subject instanceof Player stealthed
                && ClasslessCharacterPolicy.isClassless(stealthed)
                && stealthEffectiveness(stealthed, mgr) < 1f
                && subject.distance2d(observer) <= HEAVY_ARMOR_STEALTH_DETECT_YARDS) {
            return true;
        }
        return subject.isVisibleTo(observer);
    }

    public static boolean isBattlecasterSpell(int spellId) {
        return spellId == SPELL_BATTLECASTER_LEATHER
                || spellId == SPELL_BATTLECASTER_MAIL
                || spellId == SPELL_BATTLECASTER_PLATE;
    }
}
