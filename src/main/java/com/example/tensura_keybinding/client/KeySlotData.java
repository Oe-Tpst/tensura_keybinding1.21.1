package com.example.tensura_keybinding.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.tensura_keybinding.TensuraKeybinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Client-side storage for the three configurable skill slots. The actual skill
 * activation is still performed by Tensura on the server; only the slot
 * contents and the selected mode are kept locally.
 */
public final class KeySlotData {
    public static final int SLOT_COUNT = 3;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SlotEntry[] SLOTS = new SlotEntry[SLOT_COUNT];

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

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("tensura_keybinding-client.json");
    }

    public static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            SlotEntry[] loaded = GSON.fromJson(reader, SlotEntry[].class);
            if (loaded == null) {
                return;
            }

            for (int i = 0; i < SLOT_COUNT; i++) {
                SlotEntry entry = i < loaded.length ? loaded[i] : null;
                SLOTS[i] = entry != null && entry.skillId != null && !entry.skillId.isBlank()
                        ? entry
                        : null;
            }
        } catch (Exception exception) {
            TensuraKeybinding.LOGGER.warn("Failed to load Tensura key slots", exception);
        }
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(SLOTS, writer);
            }
        } catch (Exception exception) {
            TensuraKeybinding.LOGGER.warn("Failed to save Tensura key slots", exception);
        }
    }
}
