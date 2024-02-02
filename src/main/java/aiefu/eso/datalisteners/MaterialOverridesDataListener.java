package aiefu.eso.datalisteners;

import aiefu.eso.ESOCommon;
import aiefu.eso.MaterialData;
import aiefu.eso.MaterialOverrides;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MaterialOverridesDataListener {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor){
        return CompletableFuture.supplyAsync(() -> resourceManager.listResources("mat-overrides", resourceLocation -> resourceLocation.getPath().endsWith(".json")), backgroundExecutor)
                .thenCompose(preparationBarrier::wait).thenAcceptAsync(resourceLocationResourceMap -> {
                    HashMap<String, MaterialData> armor = new HashMap<>();
                    HashMap<String, MaterialData> tools = new HashMap<>();
                    HashMap<String, MaterialData> items = new HashMap<>();
                    resourceLocationResourceMap.forEach((l, r) -> {
                        try {
                            JsonElement jsonTree = JsonParser.parseReader(r.openAsReader());
                            JsonObject obj = jsonTree.getAsJsonObject();
                            String id = obj.get("material_id").getAsString();
                            String type = obj.get("type").getAsString();
                            MaterialData md = ESOCommon.getGson().fromJson(jsonTree, MaterialData.class);
                            if(type.equalsIgnoreCase("armor")){
                                armor.put(id, md);
                            } else if(type.equalsIgnoreCase("tool")){
                                tools.put(id, md);
                            } else if(type.equalsIgnoreCase("item")){
                                items.put(id, md);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    try {
                        ESOCommon.mat_config = MaterialOverrides.readWithAttachments(tools, armor, items);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }, gameExecutor);
    }
}
