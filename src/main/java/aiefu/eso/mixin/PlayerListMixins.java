package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.RecipeHolder;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixins {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void sendEOVRDataSyncPacket(Connection p_11262_, ServerPlayer serverPlayer, CallbackInfo ci){
        ESOCommon.syncData(serverPlayer);
    }

    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void sendEOVRDataSyncPacketOnReload(CallbackInfo ci){
        ESOCommon.recipeMap.values().forEach(l -> l.forEach(RecipeHolder::processTags));
        if(this.server.isSingleplayer()){
            this.server.getPlayerList().getPlayers().forEach(player -> {
                if(!this.server.isSingleplayerOwner(player.getGameProfile())){
                    ESOCommon.syncData(player);
                }
            });
        } else {
            this.server.getPlayerList().getPlayers().forEach(ESOCommon::syncData);
        }
    }
}
