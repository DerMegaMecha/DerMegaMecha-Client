package com.dermegamecha.launcher.profiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {
    @Test
    void createsSeparatedForgeAndFabricProfiles(@TempDir Path temp) throws Exception {
        var profiles = new ProfileManager().defaultProfiles(temp.resolve("launcher"));
        assertEquals(2, profiles.size());
        assertTrue(profiles.get(0).gameDirectory().toString().contains("forge"));
        assertTrue(profiles.get(1).gameDirectory().toString().contains("fabric"));
        assertNotEquals(profiles.get(0).gameDirectory(), profiles.get(1).gameDirectory());
    }
}
