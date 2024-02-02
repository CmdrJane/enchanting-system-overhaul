package aiefu.eso.network.packets;

import aiefu.eso.OverhauledEnchantmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnchantItemData {
    private ResourceLocation id;
    private String stringId;
    private int ordinal;
    public EnchantItemData(String id, int ordinal){
        this.stringId = id;
        this.ordinal = ordinal;
    }

    private EnchantItemData(ResourceLocation id, int ordinal) {
        this.id = id;
        this.ordinal = ordinal;
    }

    public static EnchantItemData decode(FriendlyByteBuf buf){
        String s = buf.readUtf();
        int ordinal = buf.readVarInt();
        return new EnchantItemData(new ResourceLocation(s), ordinal);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(stringId);
        buf.writeVarInt(ordinal);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player != null && player.containerMenu instanceof OverhauledEnchantmentMenu m){
                m.checkRequirementsAndConsume(this.id, player, this.ordinal);
            }
        });
    }
}
