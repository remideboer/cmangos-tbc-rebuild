package org.tbc.content.domain;

import org.tbc.content.compile.CompileContext;
import org.tbc.content.compile.ContentDelta;

/** One content kind (spell, item, …). Unknown kinds fail the build. */
public interface ContentDomain {
    String kind();

    void apply(ContentDelta delta, CompileContext ctx);
}
