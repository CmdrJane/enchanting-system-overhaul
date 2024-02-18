package aiefu.eso;

import net.minecraft.world.entity.player.Player;

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
}
