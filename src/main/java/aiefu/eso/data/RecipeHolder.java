package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.Utils;
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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.ModList;

import java.io.FileNotFoundException;
import java.io.FileReader;
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
        ItemDataPrepared[] arr = this.levels.get(targetLevel);
        if(arr != null) {
            ArrayList<ItemDataPrepared> list = new ArrayList<>();
            for (ItemDataPrepared prep : arr){
                if(prep != null && !prep.isEmpty()){
                    list.add(prep);
                }
            }

            for (ItemDataPrepared prep : list){
                for (int i = 1; i < 5; i++) {
                    ItemStack stack = container.getItem(i);
                    if(prep.testItemStackMatch(stack)){
                        x++;
                        break;
                    }
                }
            }
            return x >= list.size() && this.checkXPRequirements(player, xpMap.get(targetLevel));
        } else {
            int xp = xpMap.get(targetLevel);
            if(xp > 0){
                return this.checkXPRequirements(player, xpMap.get(targetLevel));
            }
        }
        return false;
    }

    public boolean checkAndConsume(SimpleContainer container, int targetLevel, Player player){
        int x = 0;
        ItemDataPrepared[] arr = this.levels.get(targetLevel);
        if(arr != null) {
            Int2ObjectOpenHashMap<ItemDataPrepared> stacksToConsume = new Int2ObjectOpenHashMap<>();
            ArrayList<ItemDataPrepared> list = new ArrayList<>();
            for (ItemDataPrepared prep : arr){
                if(prep != null && !prep.isEmpty()){
                    list.add(prep);
                }
            }

            for (ItemDataPrepared prep : list){
                for (int i = 1; i < 5; i++) {
                    ItemStack stack = container.getItem(i);
                    if(prep.testItemStackMatch(stack)){
                        stacksToConsume.put(i, prep);
                        x++;
                        break;
                    }
                }
            }

            if(x >= list.size() && this.checkXPRequirementsAndConsume(player, xpMap.get(targetLevel))){
                for (Int2ObjectMap.Entry<ItemDataPrepared> e: stacksToConsume.int2ObjectEntrySet()){
                    ItemStack stack = container.getItem(e.getIntKey());
                    ItemDataPrepared data = e.getValue();
                    ItemStack remainder = data.getRemainderForStack(stack);
                    if(stack.getCount() == 1 && remainder != null){
                        container.setItem(e.getIntKey(), remainder);
                    } else stack.shrink(data.amount);
                }
                return true;
            }
        } else {
            int xp = xpMap.get(targetLevel);
            if(xp > 0){
                return this.checkXPRequirementsAndConsume(player, xp);
            }
        }
        return false;
    }

    public boolean checkXPRequirements(Player player, int cost){
        if(cost < 1){
            return true;
        }
        if(mode){
            return cost <= Utils.getTotalAvailableXPPoints(player);
        } else {
            return player.experienceLevel >= cost;
        }
    }

    public boolean checkXPRequirementsAndConsume(Player player, int cost){
        if(cost < 1){
            return true;
        }
        if(mode){
            if(cost <= Utils.getTotalAvailableXPPoints(player)){
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
                if(ModList.get().isLoaded(s)){
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
                if(e.getValue() != null){
                    ItemDataPrepared[] prepArr = new ItemDataPrepared[e.getValue().length];
                    for (int i = 0; i < e.getValue().length; i++) {
                        ItemData data = e.getValue()[i];
                        prepArr[i] = new ItemDataPrepared(data, location, id, true);
                    }
                    levelsProcessed.put(e.getIntKey(), prepArr);
                }
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

    public static RecipeHolder deserializeDefaultRecipe(ResourceLocation location) throws FileNotFoundException {
        String id = "default";

        Int2ObjectOpenHashMap<ItemData[]> levels = ESOCommon.getGson().fromJson(new FileReader("./config/eso/default-recipe.json"), new TypeToken<Int2ObjectOpenHashMap<ItemData[]>>(){}.getType());
        JsonObject jsonTree = JsonParser.parseReader(new FileReader("./config/eso/default-xp-map.json")).getAsJsonObject();
        boolean useXPPoints = jsonTree.has("useExpPoints") && jsonTree.get("useExpPoints").getAsBoolean();
        JsonElement xp = jsonTree.get("xp");
        Int2IntOpenHashMap xpMap = ESOCommon.getGson().fromJson(xp, new TypeToken<Int2IntOpenHashMap>(){}.getType());

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
        return new RecipeHolder(new ResourceLocation(id), useXPPoints, id, 0, levelsProcessed, xpMap);
    }
}
