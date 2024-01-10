package aiefu.eso;

import net.darkhax.enchdesc.DescriptionManager;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchDescCompat {
    public static MutableComponent getEnchantmentDescription(Enchantment enchantment){
        return DescriptionManager.getDescription(enchantment);
    }
}
