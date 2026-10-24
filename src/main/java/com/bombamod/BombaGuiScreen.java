package com.bombamod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.UUID;

public class BombaGuiScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLUMN_GAP = 6;
    private static final int ROW_SPACING = 24;
    private static final int TOP_Y = 40;
    private static final int BOTTOM_MARGIN = 30;

    private boolean noTargets = false;
    private boolean truncated = false;

    public BombaGuiScreen() {
        super(Text.literal("Bomba Mod - Panel Graczy"));
    }

    @Override
    protected void init() {
        super.init();
        if (this.client == null || this.client.player == null || this.client.getNetworkHandler() == null) {
            noTargets = true;
            return;
        }

        UUID self = this.client.player.getUuid();
        int gridWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
        int startX = this.width / 2 - gridWidth / 2;

        int index = 0;
        truncated = false;
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getId().equals(self)) continue;

            int column = index % 2;
            int row = index / 2;
            int y = TOP_Y + row * ROW_SPACING;
            if (y + BUTTON_HEIGHT > this.height - BOTTOM_MARGIN) {
                truncated = true;
                break;
            }

            String playerName = entry.getProfile().getName();
            int x = startX + column * (BUTTON_WIDTH + COLUMN_GAP);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("BOMBA: " + playerName), button -> sendBomb(playerName))
                    .dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            index++;
        }
        noTargets = index == 0;
    }

    private void sendBomb(String playerName) {
        ClientPlayNetworking.send(new Networking.BombPayload(playerName));
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal("§cWysłano żądanie detonacji na: §e" + playerName), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFF5555);
        super.render(context, mouseX, mouseY, delta);

        if (noTargets) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Brak innych graczy na serwerze"), this.width / 2, TOP_Y, 0xAAAAAA);
        } else if (truncated) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Za dużo graczy – nie wszyscy mieszczą się na ekranie"), this.width / 2, this.height - 20, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
