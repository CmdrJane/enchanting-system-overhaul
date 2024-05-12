package aiefu.eso;

import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class Utils {
    public static final Component SPACE = Component.literal(" ");

    public static int getTotalAvailableXPPoints(Player player){
        int i = player.experienceLevel;
        int totalXP = 0;
        for (int j = 0; j < i; j++) {
            totalXP += getXpNeededForLevel(j);
        }
        return (int) (totalXP + player.experienceProgress * player.getXpNeededForNextLevel());
    }

    public static float getXPCostInLevels(Player player, int points){
        return getXPCostInLevels(player, points, getTotalAvailableXPPoints(player));
    }

    public static float getXPCostInLevels(Player player, int points, int totalXP){
        int remainingPoints = points;
        int level = player.experienceLevel;
        float result = 0.0F;
        if(totalXP == points){
            return player.experienceLevel + player.experienceProgress;
        } else if(totalXP > points){
            while (remainingPoints > 0){
                int xp = getXpNeededForLevel(level);
                if(remainingPoints >= xp){
                    result += 1.0F;
                } else result += (float) remainingPoints / xp;
                remainingPoints -= xp;
                level--;
            }
        } else {
            level = 0;
            while (remainingPoints > 0){
                int xp = getXpNeededForLevel(level);
                if(remainingPoints >= xp){
                    result  += 1.0F;
                } else result += (float) remainingPoints / xp;
                remainingPoints -= xp;
                level++;
            }

        }
        return result;
    }

    public static int getXpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else {
            return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
        }
    }

    public static int getEnchantmentsLimit(int curses, MaterialData data){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? data.getMaxEnchantments() + Math.min(curses, data.getMaxCurses()) * data.getCurseMultiplier() : data.getMaxEnchantments();
    }

    public static MaterialData getMatData(Item item){
        return ESOCommon.config.enableEnchantability ? ESOCommon.mat_config.getMaterialData(item) : MaterialOverrides.defaultMatData;
    }

    public static int getCurrentLimit(int appliedEnchantments, int curses){
        ConfigurationFile cfg = ESOCommon.config;
        return cfg.enableCursesAmplifier ? appliedEnchantments - curses : appliedEnchantments;
    }

    public static boolean containsEnchantments(ItemStack stack){
        if(stack.hasTag()){
            CompoundTag tag = stack.getTag();
            if(tag.contains("Enchantments", Tag.TAG_LIST)){
                return !tag.getList("Enchantments", Tag.TAG_COMPOUND).isEmpty();
            } else if(tag.contains("StoredEnchantments", Tag.TAG_LIST)){
                return !tag.getList("StoredEnchantments", Tag.TAG_COMPOUND).isEmpty();
            } else return false;
        } else return false;
    }

    /**
     * Use {@link net.minecraft.world.item.enchantment.EnchantmentHelper#getEnchantments} to get enchantment map from stack
     */
    public static boolean containsSameEnchantments(Map<Enchantment, Integer> m1, Map<Enchantment, Integer> m2){
        if(m1.size() != m2.size()){
            return false;
        } else {
            int matches = 0;
            for (Enchantment e : m1.keySet()){
                if(m2.containsKey(e)) matches++;
            }
            return matches == m2.size();
        }
    }

    public static boolean containsSameEnchantmentsOfSameLevel(Map<Enchantment, Integer> m1, Map<Enchantment, Integer> m2){
        if(m1.size() != m2.size()){
            return false;
        } else {
            int matches = 0;
            for (Map.Entry<Enchantment, Integer> e : m1.entrySet()){
                Integer lvl = m2.get(e.getKey());
                if(lvl != null && lvl.intValue() == e.getValue()) matches++;
            }
            return matches == m2.size();
        }
    }

    public static LinkedHashMap<Enchantment, Integer> filterToNewMap(Map<Enchantment, Integer> map, BiPredicate<Enchantment, Integer> predicate){
        LinkedHashMap<Enchantment, Integer>  enchs = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> e : map.entrySet()){
            if(predicate.test(e.getKey(), e.getValue())){
                enchs.put(e.getKey(), e.getValue());
            }
        }
        return enchs;
    }
}
