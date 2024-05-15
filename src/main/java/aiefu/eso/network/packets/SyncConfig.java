package aiefu.eso.network.packets;

import aiefu.eso.ConfigurationFile;
import aiefu.eso.ESOCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncConfig {
    protected ConfigurationFile cfg;

    public SyncConfig(ConfigurationFile cfg) {
        this.cfg = cfg;
    }

    public static SyncConfig decode(FriendlyByteBuf buf){
        return new SyncConfig(new ConfigurationFile(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(cfg.maxEnchantments);
        buf.writeBoolean(cfg.enableEnchantability);
        buf.writeBoolean(cfg.enableDefaultRecipe);
        buf.writeBoolean(cfg.disableDiscoverySystem);
        buf.writeBoolean(cfg.enableEnchantmentsLeveling);
        buf.writeVarInt(cfg.maxEnchantmentsOnLootBooks);
        buf.writeVarInt(cfg.maxEnchantmentsOnLootItems);
        buf.writeBoolean(cfg.enableCursesAmplifier);
        buf.writeVarInt(cfg.maxCurses);
        buf.writeVarInt(cfg.enchantmentLimitIncreasePerCurse);
        buf.writeBoolean(cfg.hideEnchantmentsWithoutRecipe);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if(FMLEnvironment.dist.isClient()){
            ctx.get().enqueueWork(() -> {
                ESOCommon.config = cfg;
            });
        }
    }
}
