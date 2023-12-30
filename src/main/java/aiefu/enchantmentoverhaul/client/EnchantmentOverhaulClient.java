package aiefu.enchantmentoverhaul.client;

import aiefu.enchantmentoverhaul.EnchDescCompat;
import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import aiefu.enchantmentoverhaul.client.gui.EnchantingTableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class EnchantmentOverhaulClient implements ClientModInitializer {
    private static Function<Enchantment, MutableComponent> getDescription;
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        MenuScreens.register(EnchantmentOverhaul.enchantment_menu_ovr, EnchantingTableScreen::new);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if(FabricLoaderImpl.INSTANCE.isModLoaded("enchdesc")){
                getDescription = EnchDescCompat::getEnchantmentDescription;
            } else {
                BuiltInRegistries.ENCHANTMENT.entrySet().forEach( e -> {
                    //copy-paste from enchantment description mod in case it's not present
                    String ed = e.getValue().getDescriptionId() + ".desc";
                    Language language = Language.getInstance();
                    if (!language.has(ed) && language.has(ed + ".description")) {

                        ed = ed + ".description";
                    }
                    descriptions.put(e.getValue(), Component.translatable(ed));
                });
                getDescription = descriptions::get;
            }
        });
    }

    public static MutableComponent getEnchantmentDescription(Enchantment e){
        return getDescription.apply(e);
    }

    public static Player getClientPlayer(){
        return Minecraft.getInstance().player;
    }

    public static void updateEnchantmentsCriteria(SimpleContainer container){
        if(Minecraft.getInstance().screen instanceof EnchantingTableScreen screen){
            screen.recalculateAvailability(container);
        }
    }

    public static void updateButtons(){
        if(Minecraft.getInstance().screen instanceof EnchantingTableScreen screen){
            screen.updateButtons();
        }
    }
}
