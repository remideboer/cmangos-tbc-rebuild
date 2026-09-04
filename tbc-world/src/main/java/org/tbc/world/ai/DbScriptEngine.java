package org.tbc.world.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Map::ScriptsStart / ScriptsProcess. spec/05-domain/scripting-plugin-contract.md */
public final class DbScriptEngine {
    private static final Logger log = LoggerFactory.getLogger(DbScriptEngine.class);

    @FunctionalInterface
    public interface CastSink {
        void cast(Unit source, Unit target, int spellId);
    }

    public boolean start(DbScriptStore store, int type, int id, Unit source, Unit target, CastSink sink) {
        if (store == null) {
            return false;
        }
        List<DbScriptStore.Step> rows = new ArrayList<>(store.scriptsFor(type, id));
        if (rows.isEmpty()) {
            return false;
        }
        rows.sort(Comparator.comparingInt(DbScriptStore.Step::delay));
        for (DbScriptStore.Step step : rows) {
            if (step.delay() > 0) {
                break;
            }
            handle(step, source, target, sink);
        }
        return true;
    }

    private static void handle(DbScriptStore.Step step, Unit source, Unit target, CastSink sink) {
        if (step.conditionId() != 0) {
            return;
        }
        if (step.command() == DbScriptStore.COMMAND_CAST_SPELL) {
            if (sink != null) {
                sink.cast(source, target, step.datalong());
            }
            return;
        }
        log.warn("unknown dbscript command {} id {}", step.command(), step.id());
    }
}
