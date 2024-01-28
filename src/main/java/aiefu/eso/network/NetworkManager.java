package aiefu.eso.network;

import aiefu.eso.ESOCommon;
import aiefu.eso.network.packets.EnchantItemData;
import aiefu.eso.network.packets.SyncEnchantmentsData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel network = NetworkRegistry.newSimpleChannel(new ResourceLocation(ESOCommon.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int id = 0;

    public static void setup(){
        network.messageBuilder(SyncEnchantmentsData.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEnchantmentsData::encode).decoder(SyncEnchantmentsData::decode).consumerMainThread(SyncEnchantmentsData::handle).add();
        network.messageBuilder(EnchantItemData.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EnchantItemData::encode).decoder(EnchantItemData::decode).consumerMainThread(EnchantItemData::handle).add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player){
        network.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet){
        network.sendToServer(packet);
    }
}
