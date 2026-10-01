package org.tbc.content.domain;

import org.tbc.content.compile.CompileContext;
import org.tbc.content.compile.ContentDelta;
import org.tbc.content.dbc.WdbcFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Appends {@code CharBaseInfo.dbc} race×class rows so the 8606 create UI lists a class
 * for the given races (e.g. Classless id 6 for all playable races).
 */
public final class CharBaseInfoDomain implements ContentDomain {
    @Override
    public String kind() {
        return "charbaseinfo";
    }

    @Override
    public void apply(ContentDelta delta, CompileContext ctx) {
        Map<String, Map<String, Object>> client = delta.client();
        if (client.isEmpty()) {
            return;
        }
        Map<String, Object> fields = client.get("CharBaseInfo");
        if (fields == null) {
            throw new IllegalArgumentException(delta.source()
                    + ": charbaseinfo kind requires client.CharBaseInfo");
        }
        Object racesObj = fields.get("races");
        if (!(racesObj instanceof List<?> raceList) || raceList.isEmpty()) {
            throw new IllegalArgumentException(delta.source()
                    + ": client.CharBaseInfo.races must be a non-empty list");
        }
        List<Integer> races = new ArrayList<>(raceList.size());
        for (Object r : raceList) {
            if (!(r instanceof Number n)) {
                throw new IllegalArgumentException(delta.source()
                        + ": races entries must be numbers, got " + r);
            }
            races.add(n.intValue());
        }
        int classId = delta.id();
        ctx.requireBinding("CharBaseInfo");
        WdbcFile dbc = ctx.requireDbc("CharBaseInfo");
        if (!dbc.packedBytes() || dbc.fieldCount() != 2) {
            throw new IllegalStateException("CharBaseInfo.dbc must be packed uint8×2 (recordSize=2)");
        }
        for (int race : races) {
            dbc.appendUniqueRecord(race, classId);
        }
        ctx.markTouched("CharBaseInfo");
    }
}
