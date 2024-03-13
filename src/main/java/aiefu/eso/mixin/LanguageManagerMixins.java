package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(LanguageManager.class)
public class LanguageManagerMixins {
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void EOVRPatchEnchsDescriptions(ResourceManager resourceManager, CallbackInfo ci){
        List<Resource> resources = resourceManager.getResourceStack(new ResourceLocation(ESOCommon.MOD_ID,"ench-desc/" + Minecraft.getInstance().getLanguageManager().getSelected().getCode() + "_ench_desc.json"));
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
}
