package com.oetpst.tensura_keybinding.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Switches the client-side key slot config to the world being joined and
 * detaches it again on logout.
 */
public final class KeySlotWorldEvents {
    private KeySlotWorldEvents() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        KeySlotData.bindToCurrentWorld();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        KeySlotData.unbind();
    }
}
