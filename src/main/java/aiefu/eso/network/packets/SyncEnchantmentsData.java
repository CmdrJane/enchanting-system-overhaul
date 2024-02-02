package aiefu.eso.network.packets;

import aiefu.eso.ESOCommon;
import aiefu.eso.RecipeHolder;
import aiefu.eso.exception.ItemDoesNotExistException;
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

    private ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> recipes;

    public SyncEnchantmentsData(){

    }

    private SyncEnchantmentsData(ConcurrentHashMap<ResourceLocation, List<RecipeHolder>> map){
        this.recipes = map;
    }

    public static SyncEnchantmentsData decode(FriendlyByteBuf buf){
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
                for (Int2ObjectMap.Entry<RecipeHolder.ItemData[]> entry : holder.levels.int2ObjectEntrySet()){
                    buf.writeVarInt(entry.getIntKey());
                    RecipeHolder.ItemData[] arr = entry.getValue();
                    buf.writeVarInt(arr.length);
                    for (RecipeHolder.ItemData data : arr){
                        String id = data.id == null ? n : data.id;
                        buf.writeUtf(id);
                        if(data.itemArray != null){
                            buf.writeBoolean(true);
                            buf.writeVarInt(data.itemArray.length);
                            for (RecipeHolder.ItemDataSimple ds : data.itemArray){
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
                        String tag = data.tag == null ? n : data.tag;
                        buf.writeUtf(tag);
                        String remainder = data.remainderId == null ? n : data.remainderId;
                        buf.writeUtf(remainder);
                        buf.writeVarInt(data.remainderAmount);
                        String remainderTag = data.remainderTag == null ? n : data.remainderTag;
                        buf.writeUtf(remainderTag);
                    }
                }
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if(FMLEnvironment.dist.isClient()){
            ctx.get().enqueueWork(() -> ESOCommon.recipeMap = this.recipes);
        }
    }
}
