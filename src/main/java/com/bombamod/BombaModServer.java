package com.bombamod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.common.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.common.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BombaModServer implements ModInitializer {
    /** Czas trwania bomby w tickach (20 tick = 1 sekunda). */
    private static final int BOMB_DURATION_TICKS = 600;
    private static final int TICKS_PER_SECOND = 20;
    /** Poziom uprawnień wymagany do użycia bomby (2 = operator). */
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private static final Map<UUID, Integer> activeBombs = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(Networking.BOMB_PACKET_TYPE, Networking.BombPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Networking.BOMB_PACKET_TYPE, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            String targetName = payload.targetPlayerName();
            MinecraftServer server = context.server();
            server.execute(() -> handleBomb(sender, targetName, server));
        });

        ServerTickEvents.START_SERVER_TICK.register(BombaModServer::tickBombs);

        // Czyścimy stan przy zamykaniu serwera, żeby bomby nie przeciekały między światami (singleplayer).
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> activeBombs.clear());
    }

    private static void handleBomb(ServerPlayerEntity sender, String targetName, MinecraftServer server) {
        if (!sender.hasPermissionLevel(REQUIRED_PERMISSION_LEVEL)) {
            sender.sendMessage(Text.literal("§cNie masz uprawnień do używania bomb."), false);
            return;
        }

        if (sender.getNameForScoreboard().equalsIgnoreCase(targetName)) {
            sender.sendMessage(Text.literal("§cNie możesz zdetonować bomby na sobie."), false);
            return;
        }

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Text.literal("§cNie znaleziono gracza: §e" + targetName), false);
            return;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BOMB_DURATION_TICKS, 3, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, BOMB_DURATION_TICKS, 0, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BOMB_DURATION_TICKS, 0, false, false, true));

        activeBombs.put(target.getUuid(), BOMB_DURATION_TICKS);

        sender.sendMessage(Text.literal("§aZdetonowano bombę na graczu: §e" + target.getNameForScoreboard()), false);
    }

    private static void tickBombs(MinecraftServer server) {
        activeBombs.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            int ticksLeft = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            boolean alive = player != null && player.isAlive();

            if (alive) {
                // Cząsteczki co 4 ticki zamiast co tick – mniej obciążenia.
                if (ticksLeft % 4 == 0) {
                    ServerWorld world = player.getServerWorld();
                    world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 2, 0.5, 0.5, 0.5, 0.1);
                    world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 5, 0.5, 1.0, 0.5, 0.05);
                }
                // Tytuł odświeżany raz na sekundę zamiast co tick.
                if (ticksLeft % TICKS_PER_SECOND == 0) {
                    int seconds = ticksLeft / TICKS_PER_SECOND;
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§c§lBOMBA!")));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§ePozostało: §f" + seconds + "s")));
                }
            }

            if (ticksLeft <= 1) {
                // Komunikat o przetrwaniu tylko dla żywego gracza.
                if (alive) {
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§a§lCZAS MINĄŁ!")));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§ePrzetrwałeś wybuch!")));
                }
                return true;
            }

            entry.setValue(ticksLeft - 1);
            return false;
        });
    }
}
