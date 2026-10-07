package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Null-safe text helper shared by the SQL loaders (moved out of ObjectMgr). */
class SqlTextTest {

    @Test
    void nzWhenNullShouldReturnEmptyString() {
        assertEquals("", SqlText.nz(null));
    }

    @Test
    void nzWhenTextShouldReturnSameText() {
        assertEquals("Worn Shortsword", SqlText.nz("Worn Shortsword"));
    }
}
