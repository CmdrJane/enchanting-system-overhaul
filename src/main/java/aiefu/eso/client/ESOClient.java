package aiefu.eso.client;

import aiefu.eso.ESOCommon;
import aiefu.eso.EnchDescCompat;
import aiefu.eso.client.gui.EnchantingTableScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.concurrent.ConcurrentHashMap;

public class ESOClient{
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();

    private static boolean ench_desc_loaded = false;

    public static void onLoadComplete(FMLLoadCompleteEvent event){
       if(ModList.get().isLoaded("enchdesc")){
           ench_desc_loaded = true;
           ESOCommon.LOGGER.info("Enchantment Descriptions mod are loaded");
       } else {
           ESOCommon.LOGGER.info("Enchantment Descriptions mod are not loaded, switching to fallback");
       }
    }

    public static void onInitializeClient(FMLClientSetupEvent event) {
        MenuScreens.register(ESOCommon.enchantment_menu_ovr.get(), EnchantingTableScreen::new);
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
