package com.aurasystem.rpg.client;

import com.aurasystem.rpg.AuraSystemMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ClientKeys {
    private ClientKeys() {}

    // Desde 1.21.9 las categorias de teclas son objetos y solo pueden registrarse una vez.
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(AuraSystemMod.id("main"));

    public static KeyBinding OPEN_MENU;
    public static KeyBinding CAST_BOLT;

    public static void register() {
        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aurasystemrpg.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        CAST_BOLT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aurasystemrpg.cast_bolt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
    }
}
