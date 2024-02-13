package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(LootPool.class)
public class LootPoolMixins {
    @Inject(method = "addRandomItems", at = @At(value = "INVOKE", target = "net/minecraft/util/Mth.floor (F)I",
    shift = At.Shift.AFTER))
    private void ESOPatchEnchantmentsInLoot(Consumer<ItemStack> stackConsumer, LootContext lootContext, CallbackInfo ci, @Local(ordinal = 1) LocalRef<Consumer<ItemStack>> consumer){
        Consumer<ItemStack> c = consumer.get();
        consumer.set(stack -> {
            c.accept(stack);
            if(stack.isEnchanted()){
                this.ESOPatchItemStackEnchantments(stack);
            }
        });
    }

    @Unique
    private void ESOPatchItemStackEnchantments(ItemStack stack){
        CompoundTag compound = stack.getOrCreateTag();
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
        if(compound.contains("Enchantments", Tag.TAG_LIST)){
            compound.remove("Enchantments");
        }
        Map<Enchantment, Integer> enchantments = Maps.newLinkedHashMap();

        List<Enchantment> list = new ArrayList<>(enchs.keySet());
        Collections.shuffle(list);
        int size = Math.min(list.size(), stack.getItem() == Items.ENCHANTED_BOOK ?
                ESOCommon.config.maxEnchantmentsOnLootBooks : ESOCommon.config.maxEnchantmentsOnLootItems);
        for (int i = 0; i < size; i++) {
            Enchantment e = list.get(i);
            enchantments.put(e, enchs.get(e));
        }

        if(compound.contains("StoredEnchantments", Tag.TAG_LIST)){
            compound.remove("StoredEnchantments");
            enchantments.forEach((k, v) -> EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(k, v)));
        } else {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }
}
