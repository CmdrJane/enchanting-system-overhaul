package aiefu.enchantmentoverhaul;

public class ConfigurationFile {
    public int maxEnchantments;
    public int lootHandlingTactic;

    public int maxEnchantmentsOnLootBooks;
    public int maxEnchantmentsOnLootItems;

    public ConfigurationFile() {
    }

    public ConfigurationFile(int maxEnchantments, int lootHandlingTactic, int maxEnchantmentsOnLootBooks, int maxEnchantmentsOnLootItems) {
        this.maxEnchantments = maxEnchantments;
        this.lootHandlingTactic = lootHandlingTactic;
        this.maxEnchantmentsOnLootBooks = maxEnchantmentsOnLootBooks;
        this.maxEnchantmentsOnLootItems = maxEnchantmentsOnLootItems;
    }

    public int getMaxEnchantments() {
        return maxEnchantments;
    }

    public static ConfigurationFile getDefault(){
        return new ConfigurationFile(5, 0, 2, 3);
    }
}
