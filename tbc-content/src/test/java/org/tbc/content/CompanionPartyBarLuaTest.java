package org.tbc.content;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionPartyBarLuaTest {
    private static final Pattern FOUR_ARGUMENT_SET_TEXTURE =
            Pattern.compile("SetTexture\\([^\\r\\n,]+,[^\\r\\n,]+,[^\\r\\n,]+,[^\\r\\n,)]+\\)");

    @Test
    void companionPartyBarWhenLoadedByTbcClientShouldUseCompatibleTextureApi() throws Exception {
        String lua = Files.readString(addonLua());

        assertFalse(FOUR_ARGUMENT_SET_TEXTURE.matcher(lua).find(),
                "TBC 2.4.3 SetTexture accepts RGB only; alpha must use SetAlpha");
        assertTrue(lua.indexOf("CompanionPartyBarDB = CompanionPartyBarDB or {}")
                        < lua.indexOf("CreateFrame("),
                "SavedVariables must initialize before frame construction");
    }

    @Test
    void companionPartyBarWhenInitialEnableIsMissedShouldRetryAndExposeSyncStatus() throws Exception {
        String lua = Files.readString(addonLua());

        assertFalse(lua.contains("strsplit("), "parse all ten slots without relying on vararg strsplit");
        assertTrue(lua.contains("local function splitState(message)"));
        assertTrue(lua.contains("requestState()") && lua.contains("syncElapsed"),
                "inactive OnUpdate must retry the state request");
        assertTrue(lua.contains("command == \"status\""));
        assertTrue(lua.contains("CompanionPartyBarDB.lastState"));
    }

    @Test
    void companionPartyBarWhenActiveShouldForceFrameOnScreenAboveStockUi() throws Exception {
        String lua = Files.readString(addonLua());

        assertTrue(lua.contains("bar:SetClampedToScreen(true)"));
        assertTrue(lua.contains("bar:SetFrameStrata(\"HIGH\")"));
        assertTrue(lua.contains("bar:SetAlpha(1)"));
        assertTrue(lua.contains("command == \"show\""));
        assertTrue(lua.contains("bar:SetPoint(\"TOPLEFT\", UIParent, \"TOPLEFT\""));
    }

    private static Path addonLua() {
        Path lua = Path.of(System.getProperty("user.dir"))
                .resolve("../content/addons/CompanionPartyBar/CompanionPartyBar.lua")
                .normalize();
        if (!Files.isRegularFile(lua)) {
            lua = Path.of("d:/projecten/wow/tbc-server/content/addons/CompanionPartyBar/CompanionPartyBar.lua");
        }
        assertTrue(Files.isRegularFile(lua), "missing " + lua.toAbsolutePath());
        return lua;
    }
}
