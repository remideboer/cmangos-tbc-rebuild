package org.tbc.editor;

/** Editable scalars applied on update. Race/class stay on the stored row. */
public record CharacterDraft(
        String name,
        int gender,
        int skin,
        int face,
        int hairStyle,
        int hairColor,
        int facialHair,
        int level,
        int xp,
        int money,
        int map,
        int zone,
        float x,
        float y,
        float z,
        float o,
        int bindMap,
        int bindZone,
        float bindX,
        float bindY,
        float bindZ,
        int atLogin,
        int cinematic) {}
