package aiefu.enchantmentoverhaul.mixin;

import aiefu.enchantmentoverhaul.EnchantmentOverhaul;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(SetEnchantmentsFunction.class)
public class SetEnchantmentsFuncMixins  {
    @Inject(method = "run", at = @At("RETURN"))
    private void patchEnchantmentsToNewSystem(ItemStack itemStack, LootContext lootContext, CallbackInfoReturnable<ItemStack> cir){
        ItemStack stack = cir.getReturnValue();
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
        Map<Enchantment, Integer> enchantments = Maps.newLinkedHashMap();
        if(EnchantmentOverhaul.config.lootHandlingTactic == 0){
            List<Enchantment> list = new ArrayList<>(enchs.keySet());
            Collections.shuffle(list);
            int size = Math.min(list.size(), stack.getItem() == Items.ENCHANTED_BOOK ?
                    EnchantmentOverhaul.config.maxEnchantmentsOnLootBooks : EnchantmentOverhaul.config.maxEnchantmentsOnLootItems);
            for (int i = 0; i < size; i++) {
                Enchantment e = list.get(i);
                enchantments.put(e, enchs.get(e));
            }
        } else if(EnchantmentOverhaul.config.lootHandlingTactic == 1) {
            int size = Math.min(enchs.size(), stack.getItem() == Items.ENCHANTED_BOOK ?
                    EnchantmentOverhaul.config.maxEnchantmentsOnLootBooks : EnchantmentOverhaul.config.maxEnchantmentsOnLootItems);
            int i = 0;
            for (Map.Entry<Enchantment, Integer> e : enchs.entrySet()){
                if(i < size){
                    enchantments.put(e.getKey(), e.getValue());
                    i++;
                }
            }
        }
        CompoundTag compound = stack.getOrCreateTag();
        if(compound.contains("StoredEnchantments", Tag.TAG_LIST)){
            compound.remove("StoredEnchantments");
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }
}
