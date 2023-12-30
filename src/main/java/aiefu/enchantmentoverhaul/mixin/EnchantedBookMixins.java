package aiefu.enchantmentoverhaul.mixin;

import aiefu.enchantmentoverhaul.IServerPlayerAcc;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

@Mixin(EnchantedBookItem.class)
public abstract class EnchantedBookMixins extends Item {

    public EnchantedBookMixins(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if(!level.isClientSide() && player instanceof IServerPlayerAcc acc){
            HashSet<Enchantment> set = acc.enchantment_overhaul$getUnlockedEnchantments();
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);

            int originalSize = map.size();

            Iterator<Map.Entry<Enchantment, Integer>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Enchantment, Integer> entry = iterator.next();
                if(!set.contains(entry.getKey())){
                    set.add(entry.getKey());
                    iterator.remove();
                }
            }
            if(map.size() != originalSize) {
                EnchantmentHelper.setEnchantments(map, stack);
                player.displayClientMessage(Component.translatable("enchantmentoverhaul.absorbingknowledge"), false);
                if(map.size() == 0){
                    stack.shrink(1);
                    player.displayClientMessage(Component.translatable("enchantmentoverhaul.booktoashes"), false);
                }
            } else player.displayClientMessage(Component.translatable("enchantmentoverhaul.allreadylearned"), false);

        }
        return InteractionResultHolder.pass(stack);
    }

}
