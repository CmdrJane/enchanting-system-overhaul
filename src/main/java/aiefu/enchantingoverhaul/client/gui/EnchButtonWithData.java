package aiefu.enchantingoverhaul.client.gui;

import aiefu.enchantingoverhaul.RecipeHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

public class EnchButtonWithData extends CustomEnchantingButton{

    @Nullable
    protected RecipeHolder recipe;
    protected Enchantment enchantment;
    protected int ordinal;

    public EnchButtonWithData(int x, int y, int width, int height, Component message, OnPress onPress, RecipeHolder recipe, Enchantment enchantment, int ordinal) {
        super(x, y, width, height, message, onPress);
        this.recipe = recipe;
        this.enchantment = enchantment;
        this.ordinal = ordinal;
    }

    public @Nullable RecipeHolder getRecipe() {
        return recipe;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
