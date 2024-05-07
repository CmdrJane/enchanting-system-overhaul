package aiefu.eso.client;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.gui.EnchantingTableScreen;
import aiefu.eso.compat.EnchDescCompat;
import aiefu.eso.data.ColorsDataReloadListener;
import aiefu.eso.data.LanguageReloadListener;
import aiefu.eso.data.client.ColorDataHolder;
import aiefu.eso.network.ClientsideNetworkManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.ConcurrentHashMap;

public class ESOClient implements ClientModInitializer {
    public static ColorDataHolder colorData;
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();
    private static boolean ench_desc_loaded = false;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ESOCommon.enchantment_menu_ovr, EnchantingTableScreen::new);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if(FabricLoaderImpl.INSTANCE.isModLoaded("enchdesc")){
                ench_desc_loaded = true;
            }
        });
        ClientsideNetworkManager.registerGlobalReceivers();
        LanguageReloadListener.registerListener();
        ColorsDataReloadListener.registerListener(ESOCommon.getGson());
    }

    public static MutableComponent getEnchantmentDescription(Enchantment e){
        return ench_desc_loaded ? EnchDescCompat.getEnchantmentDescription(e) : descriptions.computeIfAbsent(e, (enchantment) -> {
            String ed = enchantment.getDescriptionId() + ".desc";
            Language language = Language.getInstance();
            if (!language.has(ed) && language.has(ed + ".description")) {

                ed = ed + ".description";
            }
           return Component.translatable(ed).withStyle(ChatFormatting.DARK_GRAY);
        });
    }

    public static Player getClientPlayer(){
        return Minecraft.getInstance().player;
    }
}
