package aiefu.enchantingoverhaul.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixins extends ItemCombinerMenu {

    public AnvilMenuMixins(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    /* name = bl4 in dev, $$22 1.20.1 obfuscated */
    @SuppressWarnings("all")
    @ModifyVariable(method = "createResult", name = "$$22", at = @At(value = "LOAD"))
    private boolean disableEnchantCombinerFuncEOVR(boolean bl){
        return this.player.getAbilities().instabuild;
    }

}
