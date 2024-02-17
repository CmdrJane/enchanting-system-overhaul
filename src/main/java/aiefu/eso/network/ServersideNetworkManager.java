package aiefu.eso.network;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.RecipeHolder;
import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServersideNetworkManager {

    public static void registerReceivers(){
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.c2s_enchant_item, (server, player, handler, buf, responseSender) -> {
            String s = buf.readUtf();
            int ordinal = buf.readVarInt();
            ResourceLocation location = new ResourceLocation(s);
            server.execute(() -> {
                if(player.containerMenu instanceof OverhauledEnchantmentMenu m){
                    m.checkRequirementsAndConsume(location, player, ordinal);
                }
            });
        });
    }

    public static void syncMatConfig(ServerPlayer player){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HashMap<Item, MaterialData> tools = ESOCommon.mat_config.toolsMatOverridesCompiled;
        buf.writeVarInt(tools.size());
        tools.forEach((k, v) -> {
            String loc = BuiltInRegistries.ITEM.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
        HashMap<Item, MaterialData> armor = ESOCommon.mat_config.armorMatOverridesCompiled;
        buf.writeVarInt(armor.size());
        armor.forEach((k, v) -> {
            String loc = BuiltInRegistries.ITEM.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
        HashMap<Item, MaterialData> items = ESOCommon.mat_config.hardOverridesCompiled;
        buf.writeVarInt(items.size());
        items.forEach((k, v) -> {
            String loc = BuiltInRegistries.ITEM.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
        ServerPlayNetworking.send(player, PacketIdentifiers.s2c_mat_config_sync, buf);
    }

    public static void syncData(ServerPlayer player){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        String n = "null";

        buf.writeVarInt(ESOCommon.recipeMap.size());
        for (Map.Entry<ResourceLocation, List<RecipeHolder>> e : ESOCommon.recipeMap.entrySet()){
            buf.writeUtf(e.getKey().toString());
            List<RecipeHolder> holders = e.getValue();
            buf.writeVarInt(holders.size());
            for (RecipeHolder holder : holders){
                buf.writeUtf(holder.enchantment_id);
                buf.writeVarInt(holder.maxLevel);
                buf.writeBoolean(holder.mode);

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
                buf.writeVarInt(holder.xpMap.size());
                for (Int2IntMap.Entry entry : holder.xpMap.int2IntEntrySet()){
                    buf.writeVarInt(entry.getIntKey());
                    buf.writeVarInt(entry.getIntValue());
                }
            }
        }
        ServerPlayNetworking.send(player, PacketIdentifiers.s2c_data_sync, buf);
    }
}
