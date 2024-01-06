package aiefu.enchantmentoverhaul.client;

import aiefu.enchantmentoverhaul.EnchDescCompat;
import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import aiefu.enchantmentoverhaul.RecipeHolder;
import aiefu.enchantmentoverhaul.client.gui.EnchantingTableScreen;
import aiefu.enchantmentoverhaul.exception.ItemDoesNotExistException;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
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
        ClientPlayNetworking.registerGlobalReceiver(EnchantmentOverhaul.s2c_data_sync, (client, handler, buf, responseSender) -> {
            this.readData(buf);
        });
    }

    public static MutableComponent getEnchantmentDescription(Enchantment e){
        return getDescription.apply(e);
    }

    public static Player getClientPlayer(){
        return Minecraft.getInstance().player;
    }

    public void readData(FriendlyByteBuf buf){
        Interner<String> interner = Interners.newWeakInterner();
        String n = "null";
        interner.intern(n);

        int i = buf.readVarInt();
        ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> map = new ConcurrentHashMap<>();
        for (int j = 0; j < i; j++) {
            String s = buf.readUtf();
            ResourceLocation loc = new ResourceLocation(s);
            List<RecipeHolder> holders = new ArrayList<>();
            int jk = buf.readVarInt();
            for (int b = 0; b < jk; b++) {
                String eid = buf.readUtf();
                int maxLevel = buf.readVarInt();
                int r = buf.readVarInt();
                RecipeHolder holder = new RecipeHolder();
                holder.enchantment_id = eid;
                holder.maxLevel = maxLevel;
                Int2ObjectOpenHashMap<RecipeHolder.ItemData[]> int2ObjMap = new Int2ObjectOpenHashMap<>();
                for (int k = 0; k < r; k++) {
                    int level = buf.readVarInt();
                    int q = buf.readVarInt();
                    RecipeHolder.ItemData[] arr = new RecipeHolder.ItemData[q];
                    for (int l = 0; l < q; l++) {
                        RecipeHolder.ItemData data = new RecipeHolder.ItemData();
                        String id = interner.intern(buf.readUtf());
                        data.id = n == id ? null : id;
                        boolean bl = buf.readBoolean();
                        if(bl){
                            int lk = buf.readVarInt();
                            RecipeHolder.ItemDataSimple[] idsArr = new RecipeHolder.ItemDataSimple[lk];
                            for (int m = 0; m < lk; m++) {
                                RecipeHolder.ItemDataSimple ids = new RecipeHolder.ItemDataSimple();
                                ids.id = buf.readUtf();
                                ids.amount = buf.readVarInt();
                                String tag = interner.intern(buf.readUtf());
                                ids.tag = tag == n ? null : tag;
                                String rid = interner.intern(buf.readUtf());
                                ids.remainderId = rid == n ? null : rid;
                                ids.remainderAmount = buf.readVarInt();
                                String rtag = interner.intern(buf.readUtf());
                                ids.remainderTag = rtag == n ? null : rtag;
                                idsArr[m] = ids;
                            }
                            data.itemArray = idsArr;
                        }
                        data.amount = buf.readVarInt();
                        String tag = interner.intern(buf.readUtf());
                        data.tag = n == tag ? null : tag;
                        String remainder = interner.intern(buf.readUtf());
                        data.remainderId = n == remainder ? null : remainder;
                        data.remainderAmount = buf.readVarInt();
                        String remainderTag = interner.intern(buf.readUtf());
                        data.remainderTag = n == remainderTag ? null : remainderTag;
                        try {
                            data.makeId(eid);
                            data.processTags();
                        } catch (ItemDoesNotExistException e) {
                            throw new RuntimeException(e);
                        }
                        arr[l] = data;
                    }
                    int2ObjMap.put(level, arr);
                }
                holder.levels = int2ObjMap;
                holders.add(holder);
            }
            map.put(loc, holders);
        }
        Minecraft.getInstance().execute(() -> EnchantmentOverhaul.recipeMap = map);
    }
}
