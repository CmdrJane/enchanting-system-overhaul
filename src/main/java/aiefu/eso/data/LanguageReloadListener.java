package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.mixin.IClientLanguageAcc;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.*;

public class LanguageReloadListener {
    private static final ResourceLocation language_reload_listener = new ResourceLocation(ESOCommon.MOD_ID, "language_reload_listener");
    public static void registerListener(){
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return language_reload_listener;
            }

            @Override
            public Collection<ResourceLocation> getFabricDependencies() {
                return Collections.singletonList(ResourceReloadListenerKeys.LANGUAGES);
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                List<Resource> resources = resourceManager.getResourceStack(new ResourceLocation(ESOCommon.MOD_ID,"ench-desc/" + Minecraft.getInstance().getLanguageManager().getSelected() + "_ench_desc.json"));
                if(Language.getInstance() instanceof IClientLanguageAcc lacc){
                    Map<String, String> lmap = lacc.getLanguageMap();
                    HashMap<String, String> languageMap = new HashMap<>();
                    for (Resource resource : resources){
                        try {
                            languageMap.putAll(ESOCommon.getGson().fromJson(resource.openAsReader(), new TypeToken<HashMap<String, String>>(){}.getType()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    languageMap.putAll(lmap);
                    lacc.setLanguageMap(languageMap);
                }
            }
        });
    }
}
