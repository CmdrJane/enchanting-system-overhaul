package aiefu.enchantmentoverhaul;

import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;

public interface IServerPlayerAcc {
    HashSet<Enchantment> enchantment_overhaul$getUnlockedEnchantments();
}
