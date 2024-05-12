package aiefu.eso;

public class ConfigurationFile {
    public int maxEnchantments;
    public boolean enableEnchantability;
    public boolean enableDefaultRecipe;
    public boolean disableDiscoverySystem;
    public boolean enableEnchantmentsLeveling;
    public int maxEnchantmentsOnLootBooks;
    public int maxEnchantmentsOnLootItems;
    public boolean enableCursesAmplifier;
    public int maxCurses;
    public int enchantmentLimitIncreasePerCurse;

    public boolean hideEnchantmentsWithoutRecipe;

    public ConfigurationFile() {
    }

    public ConfigurationFile(int maxEnchantments, boolean enableEnchantability, boolean enableDefaultRecipe, boolean disableDiscoverySystem, boolean enableEnchantmentsLeveling, int maxEnchantmentsOnLootBooks, int maxEnchantmentsOnLootItems, boolean enableCursesAmplifier, int maxCurses, int enchantmentLimitIncreasePerCurse, boolean hideEnchantmentsWithoutRecipe) {
        this.maxEnchantments = maxEnchantments;
        this.enableEnchantability = enableEnchantability;
        this.enableDefaultRecipe = enableDefaultRecipe;
        this.disableDiscoverySystem = disableDiscoverySystem;
        this.enableEnchantmentsLeveling = enableEnchantmentsLeveling;
        this.maxEnchantmentsOnLootBooks = maxEnchantmentsOnLootBooks;
        this.maxEnchantmentsOnLootItems = maxEnchantmentsOnLootItems;
        this.enableCursesAmplifier = enableCursesAmplifier;
        this.maxCurses = maxCurses;
        this.enchantmentLimitIncreasePerCurse = enchantmentLimitIncreasePerCurse;
        this.hideEnchantmentsWithoutRecipe = hideEnchantmentsWithoutRecipe;
    }

    public static ConfigurationFile getDefault(){
        return new ConfigurationFile(4, true, true, false, false,10, 3, true, 1, 1, false);
    }
}
