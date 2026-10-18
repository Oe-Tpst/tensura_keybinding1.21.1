package com.example.tensura_keybinding;

import org.slf4j.Logger;

import com.example.tensura_keybinding.network.KeySlotActionHandler;
import com.example.tensura_keybinding.network.KeySlotActionPayload;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(TensuraKeybinding.MODID)
public class TensuraKeybinding {
    public static final String MODID = "tensura_keybinding";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraKeybinding(IEventBus modEventBus) {
        LOGGER.info("Tensura: Key binding initialized");

        modEventBus.addListener(TensuraKeybinding::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                KeySlotActionPayload.TYPE,
                KeySlotActionPayload.STREAM_CODEC,
                KeySlotActionHandler::handle);
    }
}
