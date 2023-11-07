package aiefu.enchantmentoverhaul.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashSet;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixins {
    private HashSet<String> unlockedEnchantments = new HashSet<>();
}
