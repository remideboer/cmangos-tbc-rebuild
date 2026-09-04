package org.tbc.world.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Map::ScriptsStart / ScriptsProcess. spec/05-domain/scripting-plugin-contract.md */
public final class DbScriptEngine {
    private static final Logger log = LoggerFactory.getLogger(DbScriptEngine.class);

    @FunctionalInterface
    public interface CastSink {
        void cast(Unit source, Unit target, int spellId);
    }

    private record Scheduled(long dueMs, int type, int id, long sourceGuid, long targetGuid,
                             DbScriptStore.Step step, Unit source, Unit target) {
    }

    private long clock;
    private final List<Scheduled> queue = new ArrayList<>();

    public boolean start(DbScriptStore store, int type, int id, Unit source, Unit target, CastSink sink) {
        if (store == null) {
            return false;
        }
        List<DbScriptStore.Step> rows = new ArrayList<>(store.scriptsFor(type, id));
        if (rows.isEmpty()) {
            return false;
        }
        long sourceGuid = guid(source);
        long targetGuid = guid(target);
        if (queued(type, id, sourceGuid, targetGuid)) {
            return true;
        }
        rows.sort(Comparator.comparingInt(DbScriptStore.Step::delay));
        int i = 0;
        for (; i < rows.size(); i++) {
            DbScriptStore.Step step = rows.get(i);
            if (step.delay() > 0) {
                break;
            }
            if (handle(step, source, target, sink)) {
                return true;
            }
        }
        for (; i < rows.size(); i++) {
            DbScriptStore.Step step = rows.get(i);
            queue.add(new Scheduled(clock + step.delay(), type, id, sourceGuid, targetGuid, step, source, target));
        }
        return true;
    }

    public void process(int diff, CastSink sink) {
        clock += Math.max(0, diff);
        queue.sort(Comparator.comparingLong(Scheduled::dueMs));
        while (!queue.isEmpty() && queue.get(0).dueMs() <= clock) {
            Scheduled scheduled = queue.remove(0);
            if (handle(scheduled.step(), scheduled.source(), scheduled.target(), sink)) {
                dropRemaining(scheduled.type(), scheduled.id(), scheduled.sourceGuid(), scheduled.targetGuid());
            }
        }
    }

    private boolean queued(int type, int id, long sourceGuid, long targetGuid) {
        for (Scheduled scheduled : queue) {
            if (sameScript(scheduled, type, id, sourceGuid, targetGuid)) {
                return true;
            }
        }
        return false;
    }

    private void dropRemaining(int type, int id, long sourceGuid, long targetGuid) {
        Iterator<Scheduled> it = queue.iterator();
        while (it.hasNext()) {
            if (sameScript(it.next(), type, id, sourceGuid, targetGuid)) {
                it.remove();
            }
        }
    }

    private static boolean sameScript(Scheduled scheduled, int type, int id, long sourceGuid, long targetGuid) {
        return scheduled.type() == type && scheduled.id() == id
                && scheduled.sourceGuid() == sourceGuid && scheduled.targetGuid() == targetGuid;
    }

    private static boolean handle(DbScriptStore.Step step, Unit source, Unit target, CastSink sink) {
        if (step.conditionId() != 0) {
            return false;
        }
        if (step.command() == DbScriptStore.COMMAND_CAST_SPELL) {
            if (sink != null) {
                sink.cast(source, target, step.datalong());
            }
            return false;
        }
        if (step.command() == DbScriptStore.COMMAND_TERMINATE_SCRIPT) {
            return true;
        }
        log.warn("unknown dbscript command {} id {}", step.command(), step.id());
        return false;
    }

    private static long guid(Unit unit) {
        return unit == null ? 0 : unit.guid;
    }
}
