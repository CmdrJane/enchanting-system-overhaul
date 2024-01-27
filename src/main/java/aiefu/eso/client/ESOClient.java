package aiefu.eso.client;

import aiefu.eso.ESOCommon;
import aiefu.eso.EnchDescCompat;
import aiefu.eso.RecipeHolder;
import aiefu.eso.client.gui.EnchantingTableScreen;
import aiefu.eso.exception.ItemDoesNotExistException;
import aiefu.eso.mixin.IClientLanguageAcc;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ESOClient implements ClientModInitializer {
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();

    private static final ResourceLocation language_reload_listener = new ResourceLocation(ESOCommon.MOD_ID, "language_reload_listener");

    private static boolean ench_desc_loaded = false;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ESOCommon.enchantment_menu_ovr, EnchantingTableScreen::new);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if(FabricLoaderImpl.INSTANCE.isModLoaded("enchdesc")){
                ench_desc_loaded = true;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ESOCommon.s2c_data_sync, (client, handler, buf, responseSender) -> {
            this.readData(buf);
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
               public ResourceLocation getFabricId() {
                return language_reload_listener;
            }

            @Override
            public Collection<ResourceLocation> getFabricDependencies() {
                return Collections.singletonList(ResourceReloadListenerKeys.LANGUAGES);
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                List<Resource> resources = resourceManager.getResourceStack(new ResourceLocation(ESOCommon.MOD_ID,"ench-desc/" +Minecraft.getInstance().getLanguageManager().getSelected() + "_ench_desc.json"));
                if(Language.getInstance() instanceof IClientLanguageAcc lacc){
                    Map<String, String> lmap = lacc.getLanguageMap();
                    HashMap<String, String> languageMap = new HashMap<>();
                    for (Resource resource : resources){
                        try {
                            languageMap.putAll(ESOCommon.getGson().fromJson(resource.openAsReader(), new TypeToken<HashMap<String, String>>(){}.getType()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    languageMap.putAll(lmap);
                    lacc.setLanguageMap(languageMap);
                }
            }
        });
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
        Minecraft.getInstance().execute(() -> ESOCommon.recipeMap = map);
    }
}
