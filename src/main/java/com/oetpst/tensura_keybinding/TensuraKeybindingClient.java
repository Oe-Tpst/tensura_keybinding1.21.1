package com.oetpst.tensura_keybinding;

import com.oetpst.tensura_keybinding.client.ClientEvents;
import com.oetpst.tensura_keybinding.client.KeySlotHandler;
import com.oetpst.tensura_keybinding.client.KeySlotHud;
import com.oetpst.tensura_keybinding.client.KeySlotWorldEvents;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TensuraKeybinding.MODID, dist = Dist.CLIENT)
public class TensuraKeybindingClient {
    public TensuraKeybindingClient(IEventBus modEventBus) {
        // RegisterKeyMappingsEvent is a mod-bus event.
        modEventBus.addListener(KeySlotHandler::registerKeys);

        // Game-bus events: key handling and the extra button on Tensura's main screen.
        NeoForge.EVENT_BUS.register(KeySlotHandler.class);
        NeoForge.EVENT_BUS.register(ClientEvents.class);
        NeoForge.EVENT_BUS.register(KeySlotHud.class);
        NeoForge.EVENT_BUS.register(KeySlotWorldEvents.class);
    }
}
