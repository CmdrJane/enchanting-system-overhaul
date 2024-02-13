package aiefu.eso.network.packets;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.function.Supplier;

public class SyncMatData {

    HashMap<String, MaterialData> tools;
    HashMap<String, MaterialData> armor;
    HashMap<String, MaterialData> items;

    public SyncMatData() {
    }

    public SyncMatData(HashMap<String, MaterialData> tools, HashMap<String, MaterialData> armor, HashMap<String, MaterialData> items) {
        this.tools = tools;
        this.armor = armor;
        this.items = items;
    }

    public static SyncMatData decode(FriendlyByteBuf buf){
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
        return new SyncMatData(tools, armor, items);
    }

    public void encode(FriendlyByteBuf buf) {
        HashMap<Item, MaterialData> tools = ESOCommon.mat_config.toolsMatOverridesCompiled;
        buf.writeVarInt(tools.size());
        tools.forEach((k, v) -> {
            String loc = ForgeRegistries.ITEMS.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
        HashMap<Item, MaterialData> armor = ESOCommon.mat_config.armorMatOverridesCompiled;
        buf.writeVarInt(armor.size());
        armor.forEach((k, v) -> {
            String loc = ForgeRegistries.ITEMS.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
        HashMap<Item, MaterialData> items = ESOCommon.mat_config.hardOverridesCompiled;
        buf.writeVarInt(items.size());
        items.forEach((k, v) -> {
            String loc = ForgeRegistries.ITEMS.getKey(k).toString();
            buf.writeUtf(loc);
            buf.writeVarInt(v.getMaxEnchantments());
            buf.writeVarInt(v.getMaxCurses());
            buf.writeVarInt(v.getCurseMultiplier());
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ESOCommon.mat_config = MaterialOverrides.reconstructFromPacket(tools, armor, items);
        });
    }
}
