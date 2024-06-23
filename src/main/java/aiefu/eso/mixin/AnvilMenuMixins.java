package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.Utils;
import aiefu.eso.data.materialoverrides.MaterialData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixins extends ItemCombinerMenu {

    @Shadow @Final private DataSlot cost;

    public AnvilMenuMixins(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult",at = @At(value = "INVOKE", target = "net/minecraft/world/inventory/AnvilMenu.broadcastChanges()V", shift = At.Shift.BEFORE))
    public void patchResultStack(CallbackInfo ci){
        if(!this.player.getAbilities().instabuild){
            ItemStack stack = this.resultSlots.getItem(0);
            if(!stack.isEmpty() && Utils.containsEnchantments(stack)){
                ItemStack input = this.inputSlots.getItem(0);

                Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
                Map<Enchantment, Integer> inputE = EnchantmentHelper.getEnchantments(input);

                if(!Utils.containsSameEnchantmentsOfSameLevel(enchs, inputE)){
                    if(ESOCommon.config.disableAnvilEnchanting){
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                    } else if (ESOCommon.config.disableBookCombining && stack.is(Items.ENCHANTED_BOOK)) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                    } else {
                        MaterialData data = Utils.getMatData(stack.getItem());
                        Map<Enchantment, Integer> curses = Utils.filterToNewMap(enchs, (e, i) -> e.isCurse());
                        int limit = Utils.getEnchantmentsLimit(curses.size(), data);

                        if(stack.isDamageableItem() && input.isDamageableItem() && stack.getDamageValue() != input.getDamageValue()){
                            EnchantmentHelper.setEnchantments(inputE, stack);
                        } else if(enchs.size() > limit + curses.size()){
                            this.resultSlots.setItem(0, ItemStack.EMPTY);
                            this.cost.set(0);
                        }
                    }
                }
            }
        }
    }

}
