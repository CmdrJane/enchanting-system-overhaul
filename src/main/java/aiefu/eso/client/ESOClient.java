package aiefu.eso.client;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.gui.EnchantingTableScreen;
import aiefu.eso.data.ColorsDataReloadListener;
import aiefu.eso.data.LanguageReloadListener;
import aiefu.eso.data.client.ColorDataHolder;
import aiefu.eso.network.ClientsideNetworkManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentHashMap;

public class ESOClient implements ClientModInitializer {
    public static ColorDataHolder colorData;
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();
    public static KeyMapping recipeKey;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ESOCommon.enchantment_menu_ovr, EnchantingTableScreen::new);
        ClientsideNetworkManager.registerGlobalReceivers();
        LanguageReloadListener.registerListener();
        ColorsDataReloadListener.registerListener(ESOCommon.getGson());
        recipeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("eso.recipekeybind",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "eso.modname"));
    }

    public static MutableComponent getEnchantmentDescription(Enchantment e){
        return descriptions.computeIfAbsent(e, (enchantment) -> {
            String desc = enchantment.getDescriptionId();
            String ed = desc + ".desc";
            Language language = Language.getInstance();
            if (!language.has(ed) && language.has(desc + ".description")) {

                ed = desc + ".description";
            }
           return Component.translatable(ed).withStyle(ChatFormatting.DARK_GRAY);
        });
    }

    public static Player getClientPlayer(){
        return Minecraft.getInstance().player;
    }
}
