package aiefu.eso;

public class ConfigurationFile {
    public int maxEnchantments;
    public boolean enableEnchantability;
    public boolean enableDefaultRecipe;
    public int maxEnchantmentsOnLootBooks;
    public int maxEnchantmentsOnLootItems;
    public boolean enableCursesAmplifier;
    public int maxCurses;
    public int enchantmentLimitIncreasePerCurse;

    public ConfigurationFile() {
    }

    public ConfigurationFile(int maxEnchantments, boolean enableEnchantability, boolean enableDefaultRecipe, int maxEnchantmentsOnLootBooks, int maxEnchantmentsOnLootItems, boolean enableCursesAmplifier, int maxCurses, int enchantmentLimitIncreasePerCurse) {
        this.maxEnchantments = maxEnchantments;
        this.enableEnchantability = enableEnchantability;
        this.enableDefaultRecipe = enableDefaultRecipe;
        this.maxEnchantmentsOnLootBooks = maxEnchantmentsOnLootBooks;
        this.maxEnchantmentsOnLootItems = maxEnchantmentsOnLootItems;
        this.enableCursesAmplifier = enableCursesAmplifier;
        this.maxCurses = maxCurses;
        this.enchantmentLimitIncreasePerCurse = enchantmentLimitIncreasePerCurse;
    }

    public static ConfigurationFile getDefault(){
        return new ConfigurationFile(4, true, true,10, 3, true, 1, 1);
    }
}
