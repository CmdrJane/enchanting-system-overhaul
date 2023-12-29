package aiefu.enchantmentoverhaul;

public class ConfigurationFile {
    public int maxEnchantments;
    public int lootHandlingTactic;

    public ConfigurationFile() {
    }

    public ConfigurationFile(int maxEnchantments, int lootHandlingTactic) {
        this.maxEnchantments = maxEnchantments;
        this.lootHandlingTactic = lootHandlingTactic;
    }

    public int getMaxEnchantments() {
        return maxEnchantments;
    }

    public static ConfigurationFile getDefault(){
        return new ConfigurationFile(5, 0);
    }
}
