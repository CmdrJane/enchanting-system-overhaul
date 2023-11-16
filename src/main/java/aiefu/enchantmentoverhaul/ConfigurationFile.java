package aiefu.enchantmentoverhaul;

public class ConfigurationFile {
    protected int maxEnchantments;

    public ConfigurationFile() {
    }
    public ConfigurationFile(int maxEnchantments) {
        this.maxEnchantments = maxEnchantments;
    }

    public int getMaxEnchantments() {
        return maxEnchantments;
    }
}
