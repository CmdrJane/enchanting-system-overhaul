package aiefu.enchantmentoverhaul.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SetEnchantmentsFunction.class)
public class SetEnchantmentsFuncMixins  {
    @Inject(method = "run", at = @At("RETURN"))
    private void patchEnchantmentsToNewSystem(ItemStack itemStack, LootContext lootContext, CallbackInfoReturnable<ItemStack> cir){
        ItemStack stack = cir.getReturnValue();
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
        //TODO: Handling strategy
    }
}
