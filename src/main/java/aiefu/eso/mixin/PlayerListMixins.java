package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixins {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void sendEOVRDataSyncPacket(Connection netManager, ServerPlayer player, CallbackInfo ci){
        ESOCommon.syncData(player);
    }
}
