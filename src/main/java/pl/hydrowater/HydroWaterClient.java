package pl.hydrowater;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class HydroWaterClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private boolean toggleWasDown = false;

    @Override
    public void onInitializeClient() {
        HydroConfig.load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hydrowater.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.hydrowater"));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) {
            mc.setScreen(new HydroScreen());
        }

        HydroConfig cfg = HydroConfig.get();
        if (mc.player != null && mc.currentScreen == null && cfg.toggleKey >= 0) {
            boolean down = InputUtil.isKeyPressed(mc.getWindow().getHandle(), cfg.toggleKey);
            if (down && !toggleWasDown) {
                cfg.enabled = !cfg.enabled;
                HydroConfig.save();
                mc.player.sendMessage(Text.literal("HydroWater: ")
                        .formatted(Formatting.AQUA)
                        .append(Text.literal(cfg.enabled ? "ON" : "OFF")
                                .formatted(cfg.enabled ? Formatting.GREEN : Formatting.RED)), true);
            }
            toggleWasDown = down;
        } else {
            toggleWasDown = false;
        }

        HydroLogic.tick(mc);
    }
}
