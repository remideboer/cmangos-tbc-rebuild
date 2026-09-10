package org.tbc.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PetTest {
    @Test
    void checkNameWhenEmptyShouldTooShort() {
        assertEquals(Pet.PET_NAME_TOO_SHORT, Pet.checkName(""));
    }

    @Test
    void checkNameWhenOneLetterShouldTooShort() {
        assertEquals(Pet.PET_NAME_TOO_SHORT, Pet.checkName("A"));
    }

    @Test
    void checkNameWhenThirteenLettersShouldTooLong() {
        assertEquals(Pet.PET_NAME_TOO_LONG, Pet.checkName("Thirteenchars"));
    }

    @Test
    void checkNameWhenWolfieShouldSucceed() {
        assertEquals(Pet.PET_NAME_SUCCESS, Pet.checkName("Wolfie"));
    }

    @Test
    void unitBytes2WhenCanRenameShouldSetPetFlagByte() {
        Pet pet = new Pet();
        pet.canRename = true;
        assertEquals(Pet.UNIT_CAN_BE_RENAMED << 16, pet.unitBytes2());
        pet.canRename = false;
        assertEquals(0, pet.unitBytes2());
    }
}
