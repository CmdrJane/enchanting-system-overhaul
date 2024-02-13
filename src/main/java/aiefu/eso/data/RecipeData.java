package aiefu.eso.data;

import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.exception.ItemDoesNotExistException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;

public class RecipeData {
    public String enchantment_id;
    public int maxLevel;
    public Int2ObjectOpenHashMap<ItemData[]> data;

    public RecipeData(String enchantment_id, int maxLevel, Int2ObjectOpenHashMap<ItemData[]> data) {
        this.enchantment_id = enchantment_id;
        this.maxLevel = maxLevel;
        this.data = data;
    }

    public RecipeHolder getRecipeHolder(){
        Int2ObjectOpenHashMap<ItemDataPrepared[]> levels = new Int2ObjectOpenHashMap<>();
        data.forEach((k, v) -> {
            ItemDataPrepared[] preparedData = new ItemDataPrepared[v.length];
            for (int i = 0; i < v.length; i++) {
                try {
                    preparedData[i] = new ItemDataPrepared(v[i], null, enchantment_id, true);
                } catch (ItemDoesNotExistException e) {
                    throw new RuntimeException(e);
                }
            }
            levels.put(k.intValue(), preparedData);
        });
        return new RecipeHolder(new ResourceLocation(enchantment_id), enchantment_id, maxLevel, levels);
    }
}
