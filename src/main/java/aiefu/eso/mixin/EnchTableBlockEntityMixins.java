package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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

import java.util.Objects;
import java.util.Set;

@Mixin(EnchantmentTableBlockEntity.class)
public abstract class EnchTableBlockEntityMixins extends BlockEntity implements ExtendedScreenHandlerFactory {
    public EnchTableBlockEntityMixins(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Shadow public abstract Component getName();

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        if(player.getAbilities().instabuild || ESOCommon.config.disableDiscoverySystem){
            Set<ResourceLocation> keyset = BuiltInRegistries.ENCHANTMENT.keySet();
            buf.writeVarInt(keyset.size());
            for (ResourceLocation loc : keyset){
                buf.writeUtf(loc.toString());
                buf.writeVarInt(ESOCommon.getMaximumPossibleEnchantmentLevel(BuiltInRegistries.ENCHANTMENT.get(loc)));
            }
        } else {
            Object2IntOpenHashMap<Enchantment> enchantments = ((IServerPlayerAcc) player).enchantment_overhaul$getUnlockedEnchantments();
            buf.writeVarInt(enchantments.size());
            for (Object2IntMap.Entry<Enchantment> e : enchantments.object2IntEntrySet()) {
                ResourceLocation loc = BuiltInRegistries.ENCHANTMENT.getKey(e.getKey());
                Objects.requireNonNull(loc);
                buf.writeUtf(loc.toString());
                buf.writeVarInt(e.getIntValue());
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
