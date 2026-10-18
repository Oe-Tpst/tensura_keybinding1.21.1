package com.oetpst.tensura_keybinding.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oetpst.tensura_keybinding.TensuraKeybinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.manasmods.manascore.skill.impl.SkillStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Client-side storage for the three configurable skill slots. The actual skill
 * activation is still performed by Tensura on the server; only the slot
 * contents and the selected mode are kept locally.
 *
 * <p>In singleplayer the slots are stored inside the world save folder, so
 * every world has its own configuration. On a remote server there is no local
 * world folder, so the data is stored per server address under
 * {@code config/tensura_keybinding/servers/}.
 */
public final class KeySlotData {
    public static final int SLOT_COUNT = 3;

    private static final String FILE_NAME = "tensura_keybinding-client.json";
    private static final String LEGACY_FILE_NAME = "tensura_keybinding-client.json";
    private static final String MIGRATED_FILE_NAME = "tensura_keybinding-client.json.migrated";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SlotEntry[] SLOTS = new SlotEntry[SLOT_COUNT];

    private static Path currentFile;

    private KeySlotData() {
    }

    public static final class SlotEntry {
        public String skillId;
        public int mode;

        public SlotEntry() {
        }

        public SlotEntry(String skillId, int mode) {
            this.skillId = skillId;
            this.mode = mode;
        }
    }

    public static SlotEntry get(int index) {
        return index >= 0 && index < SLOT_COUNT ? SLOTS[index] : null;
    }

    public static void set(int index, String skillId, int mode) {
        if (index < 0 || index >= SLOT_COUNT) {
            return;
        }
        SLOTS[index] = new SlotEntry(skillId, Math.max(0, mode));
        save();
    }

    public static void clear(int index) {
        if (index >= 0 && index < SLOT_COUNT) {
            SLOTS[index] = null;
            save();
        }
    }

    public static void setMode(int index, int mode) {
        SlotEntry entry = get(index);
        if (entry != null) {
            entry.mode = Math.max(0, mode);
            save();
        }
    }

    /**
     * Clears every slot that still references the given skill. Called when the
     * server reports that the player has lost that skill.
     */
    public static void clearSkill(ResourceLocation skillId) {
        if (skillId == null) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            SlotEntry entry = SLOTS[i];
            if (entry == null || entry.skillId == null) {
                continue;
            }

            if (skillId.equals(ResourceLocation.tryParse(entry.skillId))) {
                SLOTS[i] = null;
                changed = true;
            }
        }

        if (changed) {
            save();
            TensuraKeybinding.LOGGER.debug("[KeySlot] cleared slots for lost skill {}", skillId);
        }
    }

    /**
     * Clears slots whose skill is no longer learned. Used as a client-side
     * safety net (for example for slots left over from before this feature,
     * or skills removed by a path that does not fire the server event).
     */
    public static void reconcileWith(SkillStorage skills) {
        // An empty list can also mean the client has not received its skill
        // data yet; do not wipe valid slots in that case. The server-side
        // removal event covers losing the last skill.
        if (skills == null || skills.getLearnedSkills().isEmpty()) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            SlotEntry entry = SLOTS[i];
            if (entry == null || entry.skillId == null) {
                continue;
            }

            ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
            if (skillId == null || skills.getSkill(skillId).isEmpty()) {
                SLOTS[i] = null;
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    /**
     * Binds the slot storage to the world currently being joined and loads its
     * file. Called from {@link KeySlotWorldEvents} on login.
     */
    public static void bindToCurrentWorld() {
        Path path = resolveCurrentFile();
        currentFile = path;
        reset();

        if (path == null) {
            TensuraKeybinding.LOGGER.warn("Could not determine a per-world config path for Tensura key slots");
            return;
        }

        boolean existed = Files.isRegularFile(path);
        load(path);
        if (!existed) {
            migrateLegacyConfig();
        }
        TensuraKeybinding.LOGGER.debug("Loaded Tensura key slots from {}", path);
    }

    /**
     * Saves and detaches from the current world. Called on logout.
     */
    public static void unbind() {
        save();
        currentFile = null;
        reset();
    }

    private static Path resolveCurrentFile() {
        Minecraft minecraft = Minecraft.getInstance();

        // Singleplayer/LAN: keep the config inside the save so it moves with
        // the world and never leaks into other saves.
        MinecraftServer integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer != null) {
            return integratedServer.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        }

        // Remote server: the client has no access to the server's world
        // folder, so key the file by server address instead.
        ServerData serverData = minecraft.getCurrentServer();
        if (serverData == null) {
            return null;
        }

        String key = serverData.ip;
        if (key == null || key.isBlank()) {
            key = serverData.name;
        }
        if (key == null || key.isBlank()) {
            key = "_unknown";
        }
        key = key.replaceAll("[^A-Za-z0-9._-]", "_");

        return FMLPaths.CONFIGDIR.get()
                .resolve("tensura_keybinding")
                .resolve("servers")
                .resolve(key + ".json");
    }

    /**
     * Moves the old global config into the first world that is joined after
     * this update, then archives it so later worlds start empty.
     */
    private static void migrateLegacyConfig() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve(LEGACY_FILE_NAME);
        if (!Files.isRegularFile(legacy)) {
            return;
        }

        if (!load(legacy)) {
            return;
        }

        save();
        try {
            Files.move(legacy, legacy.resolveSibling(MIGRATED_FILE_NAME));
            TensuraKeybinding.LOGGER.info("Migrated legacy Tensura key slot config to {}", currentFile);
        } catch (Exception exception) {
            TensuraKeybinding.LOGGER.warn("Migrated Tensura key slots but failed to archive {}", legacy, exception);
        }
    }

    private static boolean load(Path path) {
        reset();
        if (!Files.isRegularFile(path)) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            SlotEntry[] loaded = GSON.fromJson(reader, SlotEntry[].class);
            if (loaded == null) {
                return false;
            }

            for (int i = 0; i < SLOT_COUNT; i++) {
                SlotEntry entry = i < loaded.length ? loaded[i] : null;
                SLOTS[i] = entry != null && entry.skillId != null && !entry.skillId.isBlank()
                        ? entry
                        : null;
            }
            return true;
        } catch (Exception exception) {
            TensuraKeybinding.LOGGER.warn("Failed to load Tensura key slots from {}", path, exception);
            return false;
        }
    }

    public static void save() {
        Path path = currentFile;
        if (path == null) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(SLOTS, writer);
            }
        } catch (Exception exception) {
            TensuraKeybinding.LOGGER.warn("Failed to save Tensura key slots to {}", path, exception);
        }
    }

    private static void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOTS[i] = null;
        }
    }
}
