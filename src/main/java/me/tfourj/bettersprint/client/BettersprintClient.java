package me.tfourj.bettersprint.client;

import me.tfourj.bettersprint.config.BetterSprintConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class BettersprintClient implements ClientModInitializer {
    private static final Text INDICATOR_ON = Text.translatable("text.bettersprint.indicator.enabled");
    private static final Text INDICATOR_OFF = Text.translatable("text.bettersprint.indicator.disabled");

    private static KeyBinding toggleSprintKey;
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
        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bettersprint.toggle_sprint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                Category.MOVEMENT
        ));
    }

    private void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                applySprintKey(client, false);
                return;
            }

            while (toggleSprintKey.wasPressed()) {
                sprintToggled = !sprintToggled;
                BetterSprintConfig.get().sprintToggled = sprintToggled;
                BetterSprintConfig.save();
            }

            applySprintKey(client, sprintToggled);
        });

        System.out.println("BetterSprint: Registering HUD element");
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT, 
            Identifier.of("bettersprint", "sprint_indicator"), 
            this::renderHudIndicator
        );
    }

    private void applySprintKey(MinecraftClient client, boolean toggled) {
        if (client.player == null) return;
        
        KeyBinding sprintKey = client.options.sprintKey;
        boolean physicallyPressed = isPhysicallyPressed(client, sprintKey);
        boolean autoSprint = toggled && shouldAutoSprint(client);
        boolean currentSneaking = client.player.isSneaking();
        
        BetterSprintConfig config = BetterSprintConfig.get();
        
        // FOV jitter fix
        if (config.reduceFovJitter) {
            if (currentSneaking != lastSneakState) {
                fovStabilizationTicks = 5;
                lastSneakState = currentSneaking;
            }

            if (fovStabilizationTicks > 0) {
                fovStabilizationTicks--;
            }

            if (currentSneaking) {
                sprintKey.setPressed(false);

                if (client.player.isSprinting() && fovStabilizationTicks <= 3) {
                    client.player.setSprinting(false);
                    wasSprinting = true;
                }
                return;
            }

            if (wasSprinting && !currentSneaking && !client.player.isSprinting()) {
                wasSprinting = false;
                if (fovStabilizationTicks > 0) {
                    sprintKey.setPressed(false);
                    return;
                }
            }
        } else {
            if (currentSneaking) {
                sprintKey.setPressed(false);
                if (client.player.isSprinting()) {
                    client.player.setSprinting(false);
                }
                return;
            }
        }

        sprintKey.setPressed(physicallyPressed || autoSprint);

        if (!physicallyPressed && !autoSprint && client.player.isSprinting()) {
            client.player.setSprinting(false);
        }
    }

    private boolean isPhysicallyPressed(MinecraftClient client, KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }

        var boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);

        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), boundKey.getCode()) == GLFW.GLFW_PRESS;
        }

        return InputUtil.isKeyPressed(client.getWindow(), boundKey.getCode());
    }

    private boolean shouldAutoSprint(MinecraftClient client) {
        if (client.player == null) {
            return false;
        }

        if (client.player.isSneaking()) {
            return false;
        }

        return client.options.forwardKey.isPressed();
    }

    private void renderHudIndicator(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        try {
            BetterSprintConfig config = BetterSprintConfig.get();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null || client.textRenderer == null || config == null) {
                return;
            }

            if (!config.showIndicator) {
                return;
            }

            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();

            if (screenWidth <= 0 || screenHeight <= 0) {
                return;
            }

            float scale = Math.max(0.1f, Math.min(5.0f, (float) config.indicatorScale));
            double safeX = Math.max(0.0, Math.min(1.0, config.indicatorX));
            double safeY = Math.max(0.0, Math.min(1.0, config.indicatorY));

            float x = (float) (safeX * screenWidth);
            float y = (float) (safeY * screenHeight);

            Text text = sprintToggled ? INDICATOR_ON : INDICATOR_OFF;

            int textWidth = client.textRenderer.getWidth(text);
            int textHeight = client.textRenderer.fontHeight;
            int scaledWidth = (int) (textWidth * scale);
            int scaledHeight = (int) (textHeight * scale);
            int finalX = (int) (x - scaledWidth / 2);
            int finalY = (int) (y - scaledHeight / 2);

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getWindow() == null) return;

            if (config.indicatorBackground || config.indicatorBorder) {
                int bgX1 = finalX - 3;
                int bgY1 = finalY - 2;
                int bgX2 = finalX + scaledWidth + 3;
                int bgY2 = finalY + scaledHeight + 2;

                if (config.indicatorBackground) {
                    context.fill(bgX1, bgY1, bgX2, bgY2, 0x80000000); // Semi-transparent black background
                }

                if (config.indicatorBorder) {
                    context.fill(bgX1 - 1, bgY1 - 1, bgX2 + 1, bgY1, 0xFFFFFFFF); // Top border
                    context.fill(bgX1 - 1, bgY2, bgX2 + 1, bgY2 + 1, 0xFFFFFFFF); // Bottom border
                    context.fill(bgX1 - 1, bgY1, bgX1, bgY2, 0xFFFFFFFF); // Left border
                    context.fill(bgX2, bgY1, bgX2 + 1, bgY2, 0xFFFFFFFF); // Right border
                }
            }

            int textColor = sprintToggled ? 0xFF00FF00 : 0xFFFF0000; // Bright green/red
            context.drawText(mc.textRenderer, text, finalX, finalY, textColor, false);

            float tickProgress = tickCounter.getTickProgress(false);

        } catch (Exception e) {
            System.err.println("Error rendering sprint indicator: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
