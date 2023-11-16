package aiefu.enchantmentoverhaul.client.gui;

import aiefu.enchantmentoverhaul.RecipeHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchButtonWithData extends CustomEnchantingButton{
    protected RecipeHolder recipe;
    protected Enchantment enchantment;
    protected boolean prevState;

    public EnchButtonWithData(int x, int y, int width, int height, Component message, OnPress onPress, RecipeHolder recipe, Enchantment enchantment) {
        super(x, y, width, height, message, onPress);
        this.recipe = recipe;
        this.enchantment = enchantment;
    }

    public RecipeHolder getRecipe() {
        return recipe;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }
}
