package aiefu.enchantmentoverhaul;

public class ConfigurationFile {
    public int maxEnchantments;

    public int maxEnchantmentsOnLootBooks;
    public int maxEnchantmentsOnLootItems;

    public boolean enableCursesAmplifier;

    public int maxCurses;

    public int enchantmentLimitIncreasePerCurse;

    public ConfigurationFile() {
    }

    public ConfigurationFile(int maxEnchantments, int maxEnchantmentsOnLootBooks, int maxEnchantmentsOnLootItems, boolean enableCursesAmplifier, int maxCurses, int enchantmentLimitIncreasePerCurse) {
        this.maxEnchantments = maxEnchantments;
        this.maxEnchantmentsOnLootBooks = maxEnchantmentsOnLootBooks;
        this.maxEnchantmentsOnLootItems = maxEnchantmentsOnLootItems;
        this.enableCursesAmplifier = enableCursesAmplifier;
        this.maxCurses = maxCurses;
        this.enchantmentLimitIncreasePerCurse = enchantmentLimitIncreasePerCurse;
    }

    public int getMaxEnchantments() {
        return maxEnchantments;
    }

    public static ConfigurationFile getDefault(){
        return new ConfigurationFile(4, 2, 3, false, 1, 1);
    }
}
