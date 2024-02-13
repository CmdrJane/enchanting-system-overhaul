package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Set;

@Mixin(EnchantmentTableBlock.class)
public abstract class EnchantingTableMixins extends BaseEntityBlock{

    protected EnchantingTableMixins(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void interceptMenuProviderCall(BlockState state, Level level, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir){
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            if(level.getBlockEntity(pos) instanceof EnchantmentTableBlockEntity ee){
                NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider((id, playerInv, p)
                        -> new OverhauledEnchantmentMenu(id, playerInv, ContainerLevelAccess.create(level, pos), p), ee.getName()), buf -> {
                    if(player.getAbilities().instabuild || ESOCommon.config.disableDiscoverySystem){
                        Set<ResourceLocation> keyset = ForgeRegistries.ENCHANTMENTS.getKeys();
                        buf.writeVarInt(keyset.size());
                        for (ResourceLocation loc : keyset){
                            buf.writeUtf(loc.toString());
                            buf.writeVarInt(ESOCommon.getMaximumPossibleEnchantmentLevel(ForgeRegistries.ENCHANTMENTS.getValue(loc)));
                        }
                    } else {
                        Object2IntOpenHashMap<Enchantment> enchantments = ((IServerPlayerAcc) player).enchantment_overhaul$getUnlockedEnchantments();
                        buf.writeInt(enchantments.size());
                        for (Object2IntMap.Entry<Enchantment> e : enchantments.object2IntEntrySet()) {
                            ResourceLocation loc = ForgeRegistries.ENCHANTMENTS.getKey(e.getKey());
                            Objects.requireNonNull(loc);
                            buf.writeUtf(loc.toString());
                            buf.writeVarInt(e.getIntValue());
                        }
                    }
                });
            }
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
