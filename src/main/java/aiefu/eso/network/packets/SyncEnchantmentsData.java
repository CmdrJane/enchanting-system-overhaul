package aiefu.eso.network.packets;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.RecipeData;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SyncEnchantmentsData {

    private ConcurrentHashMap<ResourceLocation, List<RecipeData>> recipes;

    public SyncEnchantmentsData(){

    }

    private SyncEnchantmentsData(ConcurrentHashMap<ResourceLocation, List<RecipeData>> map){
        this.recipes = map;
    }

    public static SyncEnchantmentsData decode(FriendlyByteBuf buf){
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
                        arr[l] = new ItemData(id, tag, idsArr, amount, remainder, remainderAmount, remainderTag);
                    }
                    int2ObjMap.put(level, arr);
                }
                RecipeData holder = new RecipeData(eid, maxLevel, int2ObjMap);
                holders.add(holder);
            }
            map.put(loc, holders);
        }
        return new SyncEnchantmentsData(map);
    }

    public void encode(FriendlyByteBuf buf) {
        String n = "null";

        buf.writeVarInt(ESOCommon.recipeMap.size());
        for (Map.Entry<ResourceLocation, List<RecipeHolder>> e : ESOCommon.recipeMap.entrySet()){
            buf.writeUtf(e.getKey().toString());
            List<RecipeHolder> holders = e.getValue();
            buf.writeVarInt(holders.size());
            for (RecipeHolder holder : holders){
                buf.writeUtf(holder.enchantment_id);
                buf.writeVarInt(holder.maxLevel);

                buf.writeVarInt(holder.levels.size());
                for (Int2ObjectMap.Entry<ItemDataPrepared[]> entry : holder.levels.int2ObjectEntrySet()){
                    buf.writeVarInt(entry.getIntKey());
                    ItemDataPrepared[] arr = entry.getValue();
                    buf.writeVarInt(arr.length);
                    for (ItemDataPrepared data : arr){
                        String id = data.data.id == null ? n : data.data.id;
                        buf.writeUtf(id);
                        if(data.data.itemArray != null){
                            buf.writeBoolean(true);
                            buf.writeVarInt(data.data.itemArray.length);
                            for (ItemData ds : data.data.itemArray){
                                buf.writeUtf(ds.id);
                                buf.writeVarInt(ds.amount);
                                String tag = ds.tag == null ? n : ds.tag;
                                buf.writeUtf(tag);
                                String rid = ds.remainderId == null ? n : ds.remainderId;
                                buf.writeUtf(rid);
                                buf.writeVarInt(ds.remainderAmount);
                                String rtag = ds.remainderTag == null ? n : ds.remainderTag;
                                buf.writeUtf(rtag);
                            }
                        } else buf.writeBoolean(false);

                        buf.writeVarInt(data.amount);
                        String tag = data.data.tag == null ? n : data.data.tag;
                        buf.writeUtf(tag);
                        String remainder = data.data.remainderId == null ? n : data.data.remainderId;
                        buf.writeUtf(remainder);
                        buf.writeVarInt(data.remainderAmount);
                        String remainderTag = data.data.remainderTag == null ? n : data.data.remainderTag;
                        buf.writeUtf(remainderTag);
                    }
                }
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if(FMLEnvironment.dist.isClient()){
            ctx.get().enqueueWork(() -> {
                ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> holdersMap = new ConcurrentHashMap<>();
                recipes.forEach((k, v) -> {
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
}
