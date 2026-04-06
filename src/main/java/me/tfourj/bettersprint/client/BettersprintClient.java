package me.tfourj.bettersprint.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.tfourj.bettersprint.config.BetterSprintConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BettersprintClient implements ClientModInitializer {
    private static final Component INDICATOR_ON =
            Component.translatable("text.bettersprint.indicator.enabled");
    private static final Component INDICATOR_OFF =
            Component.translatable("text.bettersprint.indicator.disabled");

    private static KeyMapping toggleSprintKey;

    private static boolean sprintToggled;
    private static boolean wasSprinting = false;
    private static boolean lastSneakState = false;
    private static int fovStabilizationTicks = 0;

    @Override
    public void onInitializeClient() {
        BetterSprintConfig.load();
        sprintToggled = BetterSprintConfig.get().sprintToggled;

        registerKeyBindings();
        registerClientEvents();
    }

    private void registerKeyBindings() {
        toggleSprintKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.bettersprint.toggle_sprint",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MOVEMENT
        ));
    }

    private void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleSprintKey.consumeClick()) {
                sprintToggled = !sprintToggled;
                BetterSprintConfig.get().sprintToggled = sprintToggled;
                BetterSprintConfig.save();
            }

            if (client.player == null || client.level == null) {
                return;
            }

            applySprint(client, sprintToggled);
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("bettersprint", "sprint_indicator"),
                this::renderHudIndicator
        );
    }

    private void applySprint(Minecraft client, boolean toggled) {
        if (client.player == null) {
            return;
        }

        KeyMapping sprintKey = client.options.keySprint;
        boolean currentSneaking = client.player.isShiftKeyDown();
        boolean autoSprint = toggled && shouldAutoSprint(client);

        BetterSprintConfig config = BetterSprintConfig.get();

        if (config.reduceFovJitter) {
            if (currentSneaking != lastSneakState) {
                fovStabilizationTicks = 5;
                lastSneakState = currentSneaking;
            }

            if (fovStabilizationTicks > 0) {
                fovStabilizationTicks--;
            }

            if (currentSneaking) {
                sprintKey.setDown(false);

                if (client.player.isSprinting() && fovStabilizationTicks <= 3) {
                    client.player.setSprinting(false);
                    wasSprinting = true;
                }
                return;
            }

            if (wasSprinting && !currentSneaking && !client.player.isSprinting()) {
                wasSprinting = false;

                if (fovStabilizationTicks > 0) {
                    sprintKey.setDown(false);
                    return;
                }
            }
        } else {
            if (currentSneaking) {
                sprintKey.setDown(false);

                if (client.player.isSprinting()) {
                    client.player.setSprinting(false);
                }
                return;
            }
        }

        sprintKey.setDown(autoSprint);

        if (!autoSprint && client.player.isSprinting()) {
            client.player.setSprinting(false);
        }
    }

    private boolean shouldAutoSprint(Minecraft client) {
        if (client.player == null) {
            return false;
        }

        if (client.player.isShiftKeyDown()) {
            return false;
        }

        return client.options.keyUp.isDown();
    }

    private void renderHudIndicator(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        try {
            BetterSprintConfig config = BetterSprintConfig.get();
            Minecraft client = Minecraft.getInstance();

            if (client.player == null || client.font == null || config == null) {
                return;
            }

            if (!config.showIndicator) {
                return;
            }

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();

            if (screenWidth <= 0 || screenHeight <= 0) {
                return;
            }

            float scale = Math.max(0.1f, Math.min(5.0f, (float) config.indicatorScale));
            double safeX = Math.max(0.0, Math.min(1.0, config.indicatorX));
            double safeY = Math.max(0.0, Math.min(1.0, config.indicatorY));

            Component text = sprintToggled ? INDICATOR_ON : INDICATOR_OFF;

            int textWidth = client.font.width(text);
            int textHeight = client.font.lineHeight;

            int scaledWidth = Math.max(1, Math.round(textWidth * scale));
            int scaledHeight = Math.max(1, Math.round(textHeight * scale));

            int centerX = (int) Math.round(safeX * screenWidth);
            int centerY = (int) Math.round(safeY * screenHeight);

            int finalX = centerX - (scaledWidth / 2);
            int finalY = centerY - (scaledHeight / 2);

            int bgX1 = finalX - 3;
            int bgY1 = finalY - 2;
            int bgX2 = finalX + scaledWidth + 3;
            int bgY2 = finalY + scaledHeight + 2;

            if (config.indicatorBackground) {
                graphics.fill(bgX1, bgY1, bgX2, bgY2, 0x80000000);
            }

            if (config.indicatorBorder) {
                graphics.fill(bgX1 - 1, bgY1 - 1, bgX2 + 1, bgY1, 0xFFFFFFFF);
                graphics.fill(bgX1 - 1, bgY2, bgX2 + 1, bgY2 + 1, 0xFFFFFFFF);
                graphics.fill(bgX1 - 1, bgY1, bgX1, bgY2, 0xFFFFFFFF);
                graphics.fill(bgX2, bgY1, bgX2 + 1, bgY2, 0xFFFFFFFF);
            }

            int textColor = sprintToggled ? 0xFF00FF00 : 0xFFFF0000;

            // GuiGraphicsExtractor text drawing is the 26.1 GUI/HUD path
            graphics.text(
                    client.font,
                    text,
                    finalX,
                    finalY,
                    textColor,
                    config.indicatorShadow
            );

        } catch (Exception e) {
            System.err.println("Error rendering sprint indicator: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
