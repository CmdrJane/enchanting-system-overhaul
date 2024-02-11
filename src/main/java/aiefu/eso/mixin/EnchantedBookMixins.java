package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
            Object2IntOpenHashMap<Enchantment> set = acc.enchantment_overhaul$getUnlockedEnchantments();
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);

            int originalSize = map.size();
            List<Enchantment> msg = new ArrayList<>();
            Iterator<Map.Entry<Enchantment, Integer>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Enchantment, Integer> entry = iterator.next();
                if(!set.containsKey(entry.getKey())){
                    Enchantment e = entry.getKey();
                    set.put(entry.getKey(), entry.getValue().intValue());
                    msg.add(e);
                    iterator.remove();
                }
            }
            if(map.size() != originalSize) {
                esoApplyEnchantments(map, stack);
                player.displayClientMessage(Component.translatable("eso.absorbingknowledge")
                        .withStyle(ChatFormatting.DARK_PURPLE), false);
                if(map.size() == 0){
                    stack.shrink(1);
                    player.displayClientMessage(Component.translatable("eso.booktoashes")
                            .withStyle(ChatFormatting.GOLD), false);
                }
                for (Enchantment e: msg) {
                    if(e.isCurse() && !ESOCommon.config.enableCursesAmplifier){
                        continue;
                    }
                    MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
                    c.append(Component.translatable(e.getDescriptionId()));
                    c.append(Component.literal("]"));
                    player.displayClientMessage(Component.translatable("eso.youlearned", c).withStyle(ChatFormatting.GOLD), false);
                }
            } else player.displayClientMessage(Component.translatable("eso.allreadylearned")
                    .withStyle(ChatFormatting.DARK_GREEN), false);

        }
        return InteractionResultHolder.pass(stack);
    }

    @Unique
    private void esoApplyEnchantments(Map<Enchantment, Integer> map, ItemStack stack){
        stack.removeTagKey("StoredEnchantments");
        map.forEach((k, v) -> EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(k, v)));
    }

}
