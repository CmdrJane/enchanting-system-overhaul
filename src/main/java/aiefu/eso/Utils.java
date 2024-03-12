package aiefu.eso;

import aiefu.eso.data.materialoverrides.MaterialData;
import aiefu.eso.data.materialoverrides.MaterialOverrides;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class Utils {
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

}
