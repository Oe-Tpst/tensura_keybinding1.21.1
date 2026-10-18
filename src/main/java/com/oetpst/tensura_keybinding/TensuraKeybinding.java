package com.oetpst.tensura_keybinding;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.oetpst.tensura_keybinding.network.KeySlotActionHandler;
import com.oetpst.tensura_keybinding.network.KeySlotActionPayload;
import com.oetpst.tensura_keybinding.network.SkillLostHandler;
import com.oetpst.tensura_keybinding.network.SkillLostPayload;
import com.mojang.logging.LogUtils;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(TensuraKeybinding.MODID)
public class TensuraKeybinding {
    public static final String MODID = "tensura_keybinding";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<PendingRemoval> PENDING_REMOVALS = ConcurrentHashMap.newKeySet();

    public TensuraKeybinding(IEventBus modEventBus) {
        LOGGER.info("Tensura: Key binding initialized");

        modEventBus.addListener(TensuraKeybinding::registerPayloads);
        NeoForge.EVENT_BUS.register(TensuraKeybinding.class);

        // Fires before the skill is actually removed. Defer the notification to
        // the next server tick so a removal cancelled by another listener does
        // not clear a still-valid slot.
        SkillEvents.REMOVE_SKILL.register((instance, entity, message) -> {
            if (entity instanceof ServerPlayer player) {
                PENDING_REMOVALS.add(new PendingRemoval(player.getUUID(), instance.getSkillId()));
            }
            return EventResult.pass();
        });
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                KeySlotActionPayload.TYPE,
                KeySlotActionPayload.STREAM_CODEC,
                KeySlotActionHandler::handle);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    SkillLostPayload.TYPE,
                    SkillLostPayload.STREAM_CODEC,
                    SkillLostHandler::handle);
        } else {
            // The server only needs to know how to encode this payload; it
            // never handles a clientbound packet itself. Keep client-only
            // classes out of the dedicated-server class path.
            registrar.playToClient(
                    SkillLostPayload.TYPE,
                    SkillLostPayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_REMOVALS.isEmpty()) {
            return;
        }

        Iterator<PendingRemoval> iterator = PENDING_REMOVALS.iterator();
        while (iterator.hasNext()) {
            PendingRemoval pending = iterator.next();
            iterator.remove();

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(pending.playerId());
            if (player == null) {
                continue;
            }

            // Only tell the client when the skill is really gone; a listener
            // may have interrupted/cancelled the removal.
            if (SkillAPI.getSkillsFrom(player).getSkill(pending.skillId()).isEmpty()) {
                PacketDistributor.sendToPlayer(player, new SkillLostPayload(pending.skillId()));
                LOGGER.debug("[KeySlot] cleared slots for removed skill {}", pending.skillId());
            }
        }
    }

    private record PendingRemoval(UUID playerId, ResourceLocation skillId) {
    }
}
