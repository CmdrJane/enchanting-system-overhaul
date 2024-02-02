package aiefu.eso.network.packets;

import aiefu.eso.client.ESOClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CopyToClipboardData {

    private final String string;

    public CopyToClipboardData(String string) {
        this.string = string;
    }

    public static CopyToClipboardData decode(FriendlyByteBuf buf){
        String s = buf.readUtf();
        return new CopyToClipboardData(s);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(string);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ESOClient.copyToClipboard(string);
        });
    }
}
