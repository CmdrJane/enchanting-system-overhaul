package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.IServerPlayerAcc;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixins implements IServerPlayerAcc {
    @Unique
    private Object2IntOpenHashMap<Enchantment> unlockedEnchantments = new Object2IntOpenHashMap<>();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveUnlockedEnchantmentsDataEOVR(CompoundTag compound, CallbackInfo ci){
        ListTag enchantments = new ListTag();
        unlockedEnchantments.forEach((k, v) -> {
            ResourceLocation key = Registry.ENCHANTMENT.getKey(k);
            if(key != null){
                CompoundTag enchantmentData = new CompoundTag();
                enchantmentData.putString("identifier", key.toString());
                enchantmentData.putInt("level", v);
                enchantments.add(enchantmentData);
            }
        });
        CompoundTag tag = new CompoundTag();
        tag.put("LearnedEnchantments", enchantments);
        compound.put("esodata", tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readUnlockedEnchantmentsDataEOVR(CompoundTag compound, CallbackInfo ci){
        this.unlockedEnchantments.clear();
        if(compound.contains("UnlockedEnchs", Tag.TAG_LIST)){
            ListTag enchantments = compound.getList("UnlockedEnchs", Tag.TAG_STRING);
            for (Tag t : enchantments){
                String id = t.getAsString();
                Enchantment e = Registry.ENCHANTMENT.get(new ResourceLocation(id));
                if(e != null){
                    this.unlockedEnchantments.put(e, ESOCommon.getMaximumPossibleEnchantmentLevel(e));
                }
            }
            compound.remove("UnlockedEnchs");
        }
        if(compound.contains("esodata", Tag.TAG_COMPOUND)){
            CompoundTag esoData = compound.getCompound("esodata");
            if(esoData.contains("LearnedEnchantments", Tag.TAG_LIST)){
                ListTag enchantments = esoData.getList("LearnedEnchantments", Tag.TAG_COMPOUND);
                for (Tag t : enchantments){
                    CompoundTag ct = (CompoundTag) t;
                    String id = ct.getString("identifier");
                    int level = ct.getInt("level");
                    Enchantment e = Registry.ENCHANTMENT.get(new ResourceLocation(id));
                    if(e != null){
                        this.unlockedEnchantments.put(e, level);
                    }
                }
            }
        }
    }


    @Override
    public Object2IntOpenHashMap<Enchantment> enchantment_overhaul$getUnlockedEnchantments() {
        return unlockedEnchantments;
    }

    @Override
    public void enchantment_overhaul$setUnlockedEnchantments(Object2IntOpenHashMap<Enchantment> map) {
        this.unlockedEnchantments = map;
    }
}
