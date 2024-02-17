package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EnchantmentRecipesLoader {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager manager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor){
        return CompletableFuture.supplyAsync(() ->  new RecipesContainer(manager.listResources("ench-recipes", resourceLocation -> resourceLocation.getPath().endsWith(".json")),
                        manager.listResources("ench-recipes/fallbacks", resourceLocation -> resourceLocation.getPath().endsWith(".json"))), backgroundExecutor)
                .thenCompose(preparationBarrier::wait).thenAcceptAsync(container -> {
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
                }, gameExecutor);
    }

    public record RecipesContainer(Map<ResourceLocation, Resource> recipes, Map<ResourceLocation, Resource> fallbacks){

    }
}
