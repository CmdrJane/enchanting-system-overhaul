package aiefu.eso.mixin;

import aiefu.eso.IServerPlayerAcc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixins implements IServerPlayerAcc {
    @Unique
    private HashSet<Enchantment> unlockedEnchantments = new HashSet<>();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveUnlockedEnchantmentsDataEOVR(CompoundTag compound, CallbackInfo ci){
        ListTag enchantments = new ListTag();
        for (Enchantment e : unlockedEnchantments){
            ResourceLocation key = BuiltInRegistries.ENCHANTMENT.getKey(e);
            if(key != null){
                enchantments.add(StringTag.valueOf(key.toString()));
            }
        }
        compound.put("UnlockedEnchs", enchantments);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readUnlockedEnchantmentsDataEOVR(CompoundTag compound, CallbackInfo ci){
        this.unlockedEnchantments.clear();
        if(compound.contains("UnlockedEnchs", Tag.TAG_LIST)){
            ListTag enchantments = compound.getList("UnlockedEnchs", Tag.TAG_STRING);
            for (Tag t : enchantments){
                String id = t.getAsString();
                Enchantment e = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(id));
                if(e != null){
                    this.unlockedEnchantments.add(e);
                }
            }
        }
    }


    @Override
    public HashSet<Enchantment> enchantment_overhaul$getUnlockedEnchantments() {
        return unlockedEnchantments;
    }

    @Override
    public void enchantment_overhaul$setUnlockedEnchantments(HashSet<Enchantment> map) {
        this.unlockedEnchantments = map;
    }
}
