package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.itemdata.ItemData;
import aiefu.eso.data.itemdata.ItemDataPrepared;
import aiefu.eso.exception.ItemDoesNotExistException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeHolder {
    public static final ItemDataPrepared EMPTY = new ItemDataPrepared(null);
    public transient ResourceLocation ench_location;
    public transient boolean mode;

    public String enchantment_id;
    public int maxLevel;
    public Int2ObjectOpenHashMap<ItemDataPrepared[]> levels;
    public Int2IntOpenHashMap xpMap;

    public RecipeHolder(ResourceLocation ench_location, boolean mode, String enchantment_id, int maxLevel, Int2ObjectOpenHashMap<ItemDataPrepared[]> levels, Int2IntOpenHashMap xpMap) {
        this.ench_location = ench_location;
        this.mode = mode;
        this.enchantment_id = enchantment_id;
        this.maxLevel = maxLevel;
        this.levels = levels;
        this.xpMap = xpMap;
    }

    public void register(){
        List<RecipeHolder> list = ESOCommon.recipeMap.get(ench_location);
        if(list != null){
            list.add(this);
        } else {
            list = new ArrayList<>();
            list.add(this);
            ESOCommon.recipeMap.put(ench_location, list);
        }
    }

    public boolean check(SimpleContainer container, int targetLevel, Player player){
        int x = 0;
        ItemDataPrepared[] array = this.levels.get(targetLevel);
        if(array != null) {
            for (int i = 0; i < array.length; i++) {
                ItemStack stack = container.getItem(i + 1);
                ItemDataPrepared data = array[i];
                if (data != null) {
                    if (data.isEmpty() || data.testItemStackMatch(stack) && this.checkXPRequirements(player, xpMap.get(targetLevel))) {
                        x++;
                    }
                } else {
                    x++;
                }
            }
            return x > array.length - 1;
        }
        return false;
    }

    public boolean checkAndConsume(SimpleContainer container, int targetLevel, Player player){
        int x = 0;
        ItemDataPrepared[] arr = this.levels.get(targetLevel);
        if(arr != null) {
            for (int i = 0; i < arr.length; i++) {
                ItemStack stack = container.getItem(i + 1);
                ItemDataPrepared data = arr[i];
                if (data != null) {
                    if (data.isEmpty() || data.testItemStackMatch(stack) && this.checkXPRequirementsAndConsume(player, xpMap.get(targetLevel))) {
                        x++;
                    }
                } else {
                    x++;
                    arr[i] = EMPTY;
                }
            }

            if(x >= arr.length){
                for (int i = 0; i < arr.length; i++) {
                    ItemStack stack = container.getItem(i + 1);
                    ItemDataPrepared data = arr[i];
                    ItemStack remainder = data.getRemainderForStack(stack);
                    if(stack.getCount() == 1 && remainder != null){
                        container.setItem(i + 1, remainder);
                    } else stack.shrink(data.amount);
                }
                return true;
            }
        }
        return false;
    }

    public boolean checkXPRequirements(Player player, int cost){
        if(cost < 1){
            return true;
        }
        if(mode){
            int level = player.experienceLevel;
            float progress = player.experienceProgress - (float) cost / player.getXpNeededForNextLevel();
            while (progress < 0.0F){
                float f = progress * getXpNeededForLevel(level);
                if(level > 0){
                    level--;
                    progress = 1.0F +  f / getXpNeededForLevel(level);
                }
            }
            return progress >= 0.0F;
        } else {
            return player.experienceLevel >= cost;
        }
    }

    public boolean checkXPRequirementsAndConsume(Player player, int cost){
        if(cost < 1){
            return true;
        }
        if(mode){
            int level = player.experienceLevel;
            float progress = player.experienceProgress - (float) cost / player.getXpNeededForNextLevel();
            while (progress < 0.0F){
                float f = progress * getXpNeededForLevel(level);
                if(level > 0){
                    level--;
                    progress = 1.0F +  f / getXpNeededForLevel(level);
                }
            }
            if(progress >= 0.0F){
                player.giveExperiencePoints(-cost);
                return true;
            }
        } else {
            if(player.experienceLevel >= cost){
                player.giveExperienceLevels(-cost);
                return true;
            }
        }
        return false;
    }

    public int getXpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else {
            return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
        }
    }

    public void processTags(){
        for (Int2ObjectMap.Entry<ItemDataPrepared[]> e : levels.int2ObjectEntrySet()){
            for (ItemDataPrepared data : e.getValue()){
                data.processTags();
            }
        }
    }

    public int getMaxLevel(Enchantment enchantment){
        return this.maxLevel < 1 ? enchantment.getMaxLevel() : this.maxLevel;
    }

    public static MutableComponent getFullName(Enchantment e, int level, int maxLevel){
        MutableComponent mutableComponent = Component.translatable(e.getDescriptionId());
        if (level != 1 || maxLevel != 1) {
            mutableComponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }
        return mutableComponent;
    }

    public static RecipeHolder deserialize(JsonObject jsonTree, ResourceLocation location, Map<ResourceLocation, Resource> fallbacks) throws IOException {
        String id = jsonTree.get("enchantment_id").getAsString();
        ResourceLocation fallback = jsonTree.has("fallback") ? new ResourceLocation(jsonTree.get("fallback").getAsString()) : null;
        List<String> dependsOn = new ArrayList<>();
        if(jsonTree.has("dependsOn")){
            jsonTree.get("dependsOn").getAsJsonArray().forEach(jsonElement -> dependsOn.add(jsonElement.getAsString()));
        }

        if(!dependsOn.isEmpty()){
            int i = 0;
            for (String s : dependsOn){
                if(FabricLoaderImpl.INSTANCE.isModLoaded(s)){
                    i++;
                }
            }
            if(i < dependsOn.size()){
                return deserializeFallback(fallback, fallbacks);
            }
        }

        int maxLevel = jsonTree.has("maxLevel") ? jsonTree.get("maxLevel").getAsInt() : 0;
        boolean useXPPoints = jsonTree.has("useExpPoints") && jsonTree.get("useExpPoints").getAsBoolean();
        JsonElement map = jsonTree.get("levels");
        JsonElement map2 = jsonTree.has("xp") ? jsonTree.get("xp") : null;
        Int2ObjectOpenHashMap<ItemData[]> levels = ESOCommon.getGson().fromJson(map, new TypeToken<Int2ObjectOpenHashMap<ItemData[]>>(){}.getType());
        Int2IntOpenHashMap xpMap = map2 == null ? new Int2IntOpenHashMap() : ESOCommon.getGson().fromJson(map2,new TypeToken<Int2IntOpenHashMap>(){}.getType());
        Int2ObjectOpenHashMap<ItemDataPrepared[]> levelsProcessed = new Int2ObjectOpenHashMap<>();
        try {
            for (Int2ObjectMap.Entry<ItemData[]> e : levels.int2ObjectEntrySet()){
                ItemDataPrepared[] prepArr = new ItemDataPrepared[e.getValue().length];
                for (int i = 0; i < e.getValue().length; i++) {
                    ItemData data = e.getValue()[i];
                    prepArr[i] = new ItemDataPrepared(data, location, id, true);
                }
                levelsProcessed.put(e.getIntKey(), prepArr);
            }
        } catch (ItemDoesNotExistException exception){
            exception.printStackTrace();
            ESOCommon.LOGGER.warn("Failed to deserialize recipe " + location.toString() + " trying to load fallback...");
            return deserializeFallback(fallback, fallbacks);
        }
        return new RecipeHolder(new ResourceLocation(id), useXPPoints, id, maxLevel, levelsProcessed, xpMap);
    }

    private static RecipeHolder deserializeFallback(ResourceLocation fallback, Map<ResourceLocation, Resource> fallbacks) throws IOException {
        if(fallback != null){
            Resource resource = fallbacks.get(fallback);
            if(resource != null){
                return deserialize(JsonParser.parseReader(resource.openAsReader()).getAsJsonObject(), fallback, fallbacks);
            }
        }
        return null;
    }

    public static RecipeHolder deserializeDefaultRecipe(Int2ObjectOpenHashMap<ItemData[]> levels, ResourceLocation location){
        String id = "default";
        Int2ObjectOpenHashMap<ItemDataPrepared[]> levelsProcessed = new Int2ObjectOpenHashMap<>();
        levels.forEach((k, v) -> {
            ItemDataPrepared[] prepArr = new ItemDataPrepared[v.length];
            for (int i = 0; i < v.length; i++) {
                ItemData data = v[i];
                try {
                    prepArr[i] = new ItemDataPrepared(data, location, id, true);
                } catch (ItemDoesNotExistException e) {
                    throw new RuntimeException(e);
                }
            }
            levelsProcessed.put(k.intValue(), prepArr);
        });
        return new RecipeHolder(new ResourceLocation(id), false, id, 0, levelsProcessed, new Int2IntOpenHashMap());
    }
}
