package aiefu.eso;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EnchantmentRecipeDataListener {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor){
        return CompletableFuture.supplyAsync(() -> resourceManager.listResources("ench-recipes", resourceLocation -> resourceLocation.getPath().endsWith(".json")), backgroundExecutor)
                .thenCompose(preparationBarrier::wait).thenAcceptAsync(resourceLocationResourceMap -> {
                    ESOCommon.recipeMap.clear();
                    resourceLocationResourceMap.forEach((k, v) ->{
                        try {
                            ESOCommon.getGson().fromJson(v.openAsReader(), RecipeHolder.class).processData();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    }, gameExecutor);
    }
}
