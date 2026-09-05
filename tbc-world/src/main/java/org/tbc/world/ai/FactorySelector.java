package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.script.BossScript;
import org.tbc.world.script.ScriptRegistry;

/**
 * C++ FactorySelector::selectAI. spec/05-domain/scripting-plugin-contract.md
 */
public final class FactorySelector {
    static final int PERMIT_BASE_NO = -1;
    static final int PERMIT_BASE_IDLE = 1;
    static final int PERMIT_BASE_REACTIVE = 100;
    static final int PERMIT_BASE_PROACTIVE = 200;
    static final int PERMIT_BASE_SPECIAL = 800;

    private FactorySelector() {
    }

    public static UnitAI selectAI(Creature creature, ScriptRegistry scripts) {
        if (creature == null) {
            return new NullCreatureAI();
        }
        boolean controlledPet = creature.pet && creature.playerControlledPet;
        if ((!creature.pet || !controlledPet) || creature.charmer) {
            UnitAI scripted = scriptedAI(creature, scripts);
            if (scripted != null) {
                return assign(creature, scripted);
            }
        }
        UnitAI factory;
        if (creature.pet) {
            factory = controlledPet ? new PetAI() : new GuardianAI();
        } else if (creature.charmer && !creature.temporarySummon) {
            factory = new PetAI();
        } else if (creature.totem) {
            factory = new TotemAI();
        } else if (creature.aiName != null && !creature.aiName.isEmpty()) {
            factory = byName(creature.aiName);
        } else if (isGuard(creature)) {
            factory = new GuardAI();
        } else {
            factory = permitBest(creature);
        }
        if (factory == null) {
            factory = new NullCreatureAI();
        }
        return assign(creature, factory);
    }

    private static UnitAI assign(Creature creature, UnitAI ai) {
        creature.ai = ai;
        if (ai instanceof EventCreatureAI && creature.eventAi == null) {
            creature.eventAi = new EventAi();
        }
        return ai;
    }

    private static UnitAI scriptedAI(Creature creature, ScriptRegistry scripts) {
        if (scripts == null || creature.scriptName == null || creature.scriptName.isEmpty()) {
            return null;
        }
        BossScript script = scripts.create(creature.scriptName);
        if (script == null) {
            return null;
        }
        creature.script = script;
        return new ScriptedCreatureAI(script);
    }

    private static UnitAI byName(String ainame) {
        return switch (ainame) {
            case "EventAI" -> new EventCreatureAI();
            case "GuardAI" -> new GuardAI();
            case "PetAI" -> new PetAI();
            case "GuardianAI" -> new GuardianAI();
            case "TotemAI" -> new TotemAI();
            case "NullAI", "NullCreatureAI" -> new NullCreatureAI();
            default -> null;
        };
    }

    private static boolean isGuard(Creature c) {
        return (c.extraFlags & Creature.CREATURE_EXTRA_FLAG_GUARD) != 0;
    }

    private static UnitAI permitBest(Creature creature) {
        int best = PERMIT_BASE_NO;
        UnitAI chosen = null;
        int eventPermit = permitEventAi(creature);
        if (eventPermit > best) {
            best = eventPermit;
            chosen = new EventCreatureAI();
        }
        int nullPermit = PERMIT_BASE_IDLE;
        if (nullPermit > best) {
            chosen = new NullCreatureAI();
        }
        int guardPermit = isGuard(creature) ? PERMIT_BASE_SPECIAL : PERMIT_BASE_NO;
        if (guardPermit > best) {
            best = guardPermit;
            chosen = new GuardAI();
        }
        int petPermit = creature.pet ? PERMIT_BASE_SPECIAL : PERMIT_BASE_NO;
        if (petPermit > best) {
            best = petPermit;
            chosen = creature.playerControlledPet ? new PetAI() : new GuardianAI();
        }
        int totemPermit = creature.totem ? PERMIT_BASE_PROACTIVE : PERMIT_BASE_NO;
        if (totemPermit > best) {
            chosen = new TotemAI();
        }
        return chosen;
    }

    private static int permitEventAi(Creature creature) {
        if ("EventAI".equals(creature.aiName)) {
            return PERMIT_BASE_SPECIAL;
        }
        if ((creature.extraFlags & Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT) != 0
                || creature.neutralToAll) {
            return PERMIT_BASE_REACTIVE;
        }
        return PERMIT_BASE_PROACTIVE;
    }
}
