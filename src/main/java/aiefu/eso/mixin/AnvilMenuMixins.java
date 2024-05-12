package aiefu.eso.mixin;

import aiefu.eso.Utils;
import aiefu.eso.data.materialoverrides.MaterialData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixins extends ItemCombinerMenu {

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

                if(!Utils.containsSameEnchantments(enchs, inputE)){
                    MaterialData data = Utils.getMatData(stack.getItem());
                    Map<Enchantment, Integer> nc = Utils.filterToNewMap(enchs, (e, i) -> e.isCurse());
                    Map<Enchantment, Integer> ic = Utils.filterToNewMap(inputE, (e, i) -> e.isCurse());
                    Map<Enchantment, Integer> nm = new LinkedHashMap<>();

                    int f = Utils.getEnchantmentsLimit(nc.size(), data);
                    int r = 0;
                    int k = data.getMaxCurses();
                    int cr = 0;
                    for (Map.Entry<Enchantment, Integer> e : enchs.entrySet()){
                        Enchantment en = e.getKey();
                        if(r < f){
                            if(!en.isCurse()){
                                nm.put(en, e.getValue());
                                r++;
                            }
                        } else break;;
                    }
                    if(Utils.containsSameEnchantments(nc, ic)){
                        nm.putAll(nc);
                    } else {
                        for (Map.Entry<Enchantment, Integer> e : nc.entrySet()){
                            if(cr < k){
                                nm.put(e.getKey(), e.getValue());
                                cr++;
                            } else break;
                        }
                    }
                    if(stack.is(Items.ENCHANTED_BOOK)){
                        stack.getOrCreateTag().remove("StoredEnchantments");
                        for (Map.Entry<Enchantment, Integer> e : enchs.entrySet()){
                            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(e.getKey(), e.getValue()));
                        }
                    } else {
                        EnchantmentHelper.setEnchantments(nm, stack);
                    }
                }
            }
        }
    }

}
