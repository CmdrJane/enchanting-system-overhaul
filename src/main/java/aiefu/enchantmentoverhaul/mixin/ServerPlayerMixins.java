package aiefu.enchantmentoverhaul.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixins {
    @Unique
    private HashSet<String> unlockedEnchantments = new HashSet<>();
}
