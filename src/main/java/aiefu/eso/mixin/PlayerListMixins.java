package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.network.ServersideNetworkManager;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(PlayerList.class)
public class PlayerListMixins {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void sendEOVRDataSyncPacket(Connection connection, ServerPlayer player, CommonListenerCookie commonListenerCookie, CallbackInfo ci){
        if(!player.server.isSingleplayerOwner(player.getGameProfile())){
            ServersideNetworkManager.syncData(player);
            ServersideNetworkManager.syncMatConfig(player);
            ServersideNetworkManager.syncConfig(player);
            ESOCommon.LOGGER.info("All data sent to " + player.getDisplayName().getString());
        } else {
            try {
                ESOCommon.readConfig();
                ESOCommon.LOGGER.info("Configuration file reloaded from disk");
            } catch (IOException e) {
                ESOCommon.LOGGER.error("Unable to read config file from disk");
                e.printStackTrace();
            }
        }
    }
}
