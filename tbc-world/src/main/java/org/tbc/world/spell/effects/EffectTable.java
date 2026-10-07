package org.tbc.world.spell.effects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Effect id → {@link EffectHandler}. Families register themselves in {@link #standard()}. */
public final class EffectTable {
    private final Map<Integer, EffectHandler> handlers = new HashMap<>();

    /** Every family SpellEngine.apply dispatches through; effects not listed stay in SpellEngine. */
    public static EffectTable standard() {
        EffectTable t = new EffectTable();
        MovementEffects.register(t);
        ItemEffects.register(t);
        PetEffects.register(t);
        SummonEffects.register(t);
        PlayerEffects.register(t);
        UnitEffects.register(t);
        TriggerEffects.register(t);
        return t;
    }

    public void register(int effect, EffectHandler handler) {
        if (handlers.putIfAbsent(effect, handler) != null) {
            throw new IllegalStateException("effect " + effect + " registered twice");
        }
    }

    /** Handler for {@code effect}, or {@code null} when SpellEngine handles it inline. */
    public EffectHandler get(int effect) {
        return handlers.get(effect);
    }

    public Set<Integer> effects() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
}
