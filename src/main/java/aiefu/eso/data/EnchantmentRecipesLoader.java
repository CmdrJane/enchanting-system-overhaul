package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EnchantmentRecipesLoader {

    public static final ResourceLocation enchantment_recipe_loader = new ResourceLocation(ESOCommon.MOD_ID,"enchantment_recipe_loader");
    public static void registerReloadListener(){
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<RecipesContainer>() {

            @Override
            public CompletableFuture<RecipesContainer> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.supplyAsync(() -> new RecipesContainer(manager.listResources("ench-recipes", resourceLocation -> resourceLocation.getPath().endsWith(".json")),
                        manager.listResources("ench-recipes/fallbacks", resourceLocation -> resourceLocation.getPath().endsWith(".json"))), executor);
            }

            @Override
            public CompletableFuture<Void> apply(RecipesContainer container, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
                return CompletableFuture.runAsync(() -> {
                    ESOCommon.recipeMap.clear();
                    container.recipes.forEach((key, value) -> {
                        try {
                            RecipeHolder holder = RecipeHolder.deserialize(JsonParser.parseReader(value.openAsReader()).getAsJsonObject(), key, container.fallbacks);
                            if(holder != null){
                                holder.register();
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    try {
                        RecipeHolder defaultHolder = RecipeHolder.deserializeDefaultRecipe(new ResourceLocation("config/eso/default-recipe.json"));
                        defaultHolder.register();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
            }

            @Override
            public ResourceLocation getFabricId() {
                return enchantment_recipe_loader;
            }

        });
    }

    public record RecipesContainer(Map<ResourceLocation, Resource> recipes, Map<ResourceLocation, Resource> fallbacks){

    }
}
