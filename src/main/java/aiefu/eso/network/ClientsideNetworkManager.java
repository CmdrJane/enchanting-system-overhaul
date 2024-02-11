package aiefu.eso.network;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.itemdata.RecipeData;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientsideNetworkManager {
    public static void registerGlobalReceivers(){
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.s2c_data_sync, (client, handler, buf, responseSender) -> readRecipes(buf));
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.s2c_string_to_clipboard, (client, handler, buf, responseSender) -> {
            String s = buf.readUtf();
            client.execute(() -> client.keyboardHandler.setClipboard(s));
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.s2c_mat_config_sync, (client, handler, buf, responseSender) -> readMatConfigData(buf));
    }

    public static void readMatConfigData(FriendlyByteBuf buf){
        int ts = buf.readVarInt();
        HashMap<String, MaterialData> tools = new HashMap<>();
        for (int i = 0; i < ts; i++) {
            String id = buf.readUtf();
            MaterialData data = new MaterialData(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            tools.put(id, data);
        }
        int as = buf.readVarInt();
        HashMap<String, MaterialData> armor = new HashMap<>();
        for (int i = 0; i < as; i++) {
            String id = buf.readUtf();
            MaterialData data = new MaterialData(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            armor.put(id, data);
        }
        int hs = buf.readVarInt();
        HashMap<String, MaterialData> items = new HashMap<>();
        for (int i = 0; i < hs; i++) {
            String id = buf.readUtf();
            MaterialData data = new MaterialData(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
            items.put(id, data);
        }
        Minecraft.getInstance().execute(() -> ESOCommon.mat_config = MaterialOverrides.reconstructFromPacket(tools, armor, items));
    }

    public static void readRecipes(FriendlyByteBuf buf){
        Interner<String> interner = Interners.newWeakInterner();
        String n = "null";
        interner.intern(n);

        int i = buf.readVarInt();
        ConcurrentHashMap<ResourceLocation, List<RecipeData>> map = new ConcurrentHashMap<>();
        for (int j = 0; j < i; j++) {
            String s = buf.readUtf();
            ResourceLocation loc = new ResourceLocation(s);
            List<RecipeData> holders = new ArrayList<>();
            int jk = buf.readVarInt();
            for (int b = 0; b < jk; b++) {
                String eid = buf.readUtf();
                int maxLevel = buf.readVarInt();
                int r = buf.readVarInt();
                Int2ObjectOpenHashMap<ItemData[]> int2ObjMap = new Int2ObjectOpenHashMap<>();
                for (int k = 0; k < r; k++) {
                    int level = buf.readVarInt();
                    int q = buf.readVarInt();
                    ItemData[] arr = new ItemData[q];
                    for (int l = 0; l < q; l++) {
                        String id = interner.intern(buf.readUtf());
                        id = n == id ? null : id;
                        boolean bl = buf.readBoolean();
                        ItemData[] idsArr = null;
                        if(bl){
                            int lk = buf.readVarInt();
                            idsArr = new ItemData[lk];
                            for (int m = 0; m < lk; m++) {
                                String itemId = buf.readUtf();
                                int itemAmount = buf.readVarInt();
                                String tag = interner.intern(buf.readUtf());
                                tag = tag == n ? null : tag;
                                String rid = interner.intern(buf.readUtf());
                                rid = rid == n ? null : rid;
                                int ridAmount = buf.readVarInt();
                                String rtag = interner.intern(buf.readUtf());
                                rtag = rtag == n ? null : rtag;
                                idsArr[m] = new ItemData(itemId, itemAmount, tag, rid, ridAmount, rtag);
                            }
                        }
                        int amount = buf.readVarInt();
                        String tag = interner.intern(buf.readUtf());
                        tag = n == tag ? null : tag;
                        String remainder = interner.intern(buf.readUtf());
                        remainder = n == remainder ? null : remainder;
                        int remainderAmount = buf.readVarInt();
                        String remainderTag = interner.intern(buf.readUtf());
                        remainderTag = n == remainderTag ? null : remainderTag;
                        arr[l] = idsArr == null ? new ItemData(id, amount, tag, remainder, remainderAmount, remainderTag) :
                                new ItemData(id, tag, idsArr, amount, remainder, remainderAmount, remainderTag);
                    }
                    int2ObjMap.put(level, arr);
                }
                RecipeData holder = new RecipeData(eid, maxLevel, int2ObjMap);
                holders.add(holder);
            }
            map.put(loc, holders);
        }
        Minecraft.getInstance().execute(() -> {
            ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> holdersMap = new ConcurrentHashMap<>();
            map.forEach((k, v) -> {
                List<RecipeHolder> holders = new ArrayList<>();
                v.forEach(data -> {
                    RecipeHolder holder = data.getRecipeHolder();
                    holder.processTags();
                    holders.add(holder);
                });
                holdersMap.put(k, holders);
            });
            ESOCommon.recipeMap = holdersMap;
        });
    }
}
