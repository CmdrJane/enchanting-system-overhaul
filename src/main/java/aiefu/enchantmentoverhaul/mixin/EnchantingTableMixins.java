package aiefu.enchantmentoverhaul.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentTableBlock.class)
public abstract class EnchantingTableMixins extends BaseEntityBlock{

    protected EnchantingTableMixins(Properties properties) {
        super(properties);
    }

    @Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
    public void interceptMenuProviderCall(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir){
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EnchantmentTableBlockEntity) {
            cir.setReturnValue((MenuProvider) blockEntity);
        } else {
            cir.setReturnValue(null);
        }
    }
}
