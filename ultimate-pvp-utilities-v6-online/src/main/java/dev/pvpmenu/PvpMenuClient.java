package dev.pvpmenu;

import dev.pvpmenu.feature.Freelook;
import dev.pvpmenu.feature.ScreenFeatures;
import dev.pvpmenu.feature.Zoom;
import dev.pvpmenu.gui.MenuScreen;
import dev.pvpmenu.module.Module;
import dev.pvpmenu.module.Modules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import dev.pvpmenu.feature.AppleSkin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class PvpMenuClient implements ClientModInitializer {
    public static final String MOD_ID = "pvpmenu";
    public static final String MOD_NAME = "Ultimate PvP Utilities";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding MENU_KEY;
    public static KeyBinding HIDE_HUD_KEY;
    public static KeyBinding FREELOOK_KEY;
    public static KeyBinding ZOOM_KEY;
    private static boolean togglesApplied;

    /** One optional key per module, unbound until you set it in Options > Controls. */
    public static final Map<String, KeyBinding> TOGGLE_KEYS = new LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmenu.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
        HIDE_HUD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmenu.hide_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        FREELOOK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmenu.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmenu.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        for (Module m : Modules.ALL) {
            TOGGLE_KEYS.put(m.id, KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.pvpmenu.toggle." + m.id,
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    category
            )));
        }

        ClientTickEvents.END_CLIENT_TICK.register(PvpMenuClient::onTick);
        HudRenderCallback.EVENT.register(HudRenderer::render);
        AttackEntityCallback.EVENT.register(CrosshairRenderer::onAttack);
        AttackEntityCallback.EVENT.register(Stats::onAttack);
        ScreenFeatures.register();
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) ->
                Guard.run("appleskin", () -> AppleSkin.tooltip(stack, lines)));

        // Custom crosshair: when enabled it replaces the vanilla one, otherwise vanilla draws as usual.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (context, tickCounter) -> {
            if (!Modules.CROSSHAIR.isEnabled()) {
                original.render(context, tickCounter);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson()) return;
            Guard.run("crosshair", () -> CrosshairRenderer.drawLive(context, mc));
        });

        LOGGER.info("{} loaded. Press the menu key in-game to open it.", MOD_NAME);
    }

    private static void onTick(MinecraftClient client) {
        Guard.run("cps", () -> Stats.tick(client));
        Guard.run("freelook", () -> Freelook.tick(client));
        Guard.run("zoom", () -> Zoom.tick(client));
        Guard.run("toggles", () -> applyToggles(client));
        while (MENU_KEY.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new MenuScreen());
            }
        }
        while (HIDE_HUD_KEY.wasPressed()) {
            if (client.currentScreen == null) HudRenderer.hidden = !HudRenderer.hidden;
        }
        for (Map.Entry<String, KeyBinding> e : TOGGLE_KEYS.entrySet()) {
            while (e.getValue().wasPressed()) {
                if (client.currentScreen != null) continue;
                Module m = Modules.byId(e.getKey());
                if (m != null) m.toggle();
            }
        }
    }

    /** Uses the game's own "toggle sprint / toggle sneak" options while the module is on. */
    private static void applyToggles(MinecraftClient client) {
        if (client.options == null) return;
        if (Modules.TOGGLES.isEnabled()) {
            boolean sprint = Modules.TOGGLES.flagOf("sprint", true);
            boolean sneak = Modules.TOGGLES.flagOf("sneak", false);
            if (client.options.getSprintToggled().getValue() != sprint) client.options.getSprintToggled().setValue(sprint);
            if (client.options.getSneakToggled().getValue() != sneak) client.options.getSneakToggled().setValue(sneak);
            togglesApplied = true;
        } else if (togglesApplied) {
            togglesApplied = false;
            client.options.getSprintToggled().setValue(false);
            client.options.getSneakToggled().setValue(false);
        }
    }
}
