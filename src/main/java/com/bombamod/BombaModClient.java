package com.bombamod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BombaModClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bombamod.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.bombamod.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                client.setScreen(new BombaGuiScreen());
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.getNetworkHandler() == null) return;
            if (client.options.hudHidden) return; // respektuj F1

            int y = 10;
            drawContext.drawTextWithShadow(client.textRenderer, "§c§lTracker Graczy:", 10, y, 0xFFFFFF);
            y += 12;

            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().getId().equals(client.player.getUuid())) continue;

                var playerEntity = client.world.getPlayerByUuid(entry.getProfile().getId());
                if (playerEntity == null) continue;

                double distance = client.player.distanceTo(playerEntity);
                String info = String.format("§e%s: §f%.1f bloków", entry.getProfile().getName(), distance);
                drawContext.drawTextWithShadow(client.textRenderer, info, 10, y, 0xFFFFFF);
                y += 10;
            }
        });
    }
}
