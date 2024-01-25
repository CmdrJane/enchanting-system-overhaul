package aiefu.eso.mixin;

import aiefu.eso.IServerPlayerAcc;
import aiefu.eso.OverhauledEnchantmentMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mixin(EnchantmentTableBlockEntity.class)
public abstract class EnchTableBlockEntityMixins extends BlockEntity implements ExtendedScreenHandlerFactory {


    public EnchTableBlockEntityMixins(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Shadow public abstract Component getName();

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        if(player.getAbilities().instabuild){
            Set<ResourceLocation> keyset = BuiltInRegistries.ENCHANTMENT.keySet();
            buf.writeInt(keyset.size());
            for (ResourceLocation loc : keyset){
                buf.writeUtf(loc.toString());
            }
        } else {
            HashSet<Enchantment> enchantments = ((IServerPlayerAcc) player).enchantment_overhaul$getUnlockedEnchantments();
            buf.writeInt(enchantments.size());
            for (Enchantment e : enchantments) {
                ResourceLocation loc = BuiltInRegistries.ENCHANTMENT.getKey(e);
                Objects.requireNonNull(loc);
                buf.writeUtf(loc.toString());
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new OverhauledEnchantmentMenu(i, inventory, ContainerLevelAccess.create(this.getLevel(), this.getBlockPos()), player);
    }
}
