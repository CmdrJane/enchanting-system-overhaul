package aiefu.eso.client.gui;

import aiefu.eso.data.RecipeHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.Nullable;

public class EnchButtonWithData extends CustomEnchantingButton{

    @Nullable
    protected RecipeHolder recipe;
    protected EnchantmentInstance enchantmentInstance;
    protected int ordinal;

    public EnchButtonWithData(int x, int y, int width, int height, Component message, OnPress onPress, @Nullable RecipeHolder recipe, Enchantment enchantment, int level, int ordinal) {
        super(x, y, width, height, message, onPress);
        this.recipe = recipe;
        this.enchantmentInstance = new EnchantmentInstance(enchantment, level);
        this.ordinal = ordinal;
    }

    public @Nullable RecipeHolder getRecipe() {
        return recipe;
    }

    public Enchantment getEnchantment() {
        return enchantmentInstance.enchantment;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
