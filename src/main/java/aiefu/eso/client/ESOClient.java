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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ESOCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ESOClient{
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();

    private static final ResourceLocation language_reload_listener = new ResourceLocation(ESOCommon.MOD_ID, "language_reload_listener");

    private static boolean ench_desc_loaded = false;

    public ESOClient(){
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onInitializeClient);
        modEventBus.addListener(this::onLoadComplete);

    }

    public void onLoadComplete(FMLLoadCompleteEvent event){
       if(ModList.get().isLoaded("enchdesc")){
           ench_desc_loaded = true;
       }
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
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
