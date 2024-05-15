package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.ESOClient;
import aiefu.eso.data.client.BackgroundColorData;
import aiefu.eso.data.client.ColorDataHolder;
import aiefu.eso.data.client.SliderColorData;
import aiefu.eso.data.client.TextSettings;
import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ColorsDataLoader {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager manager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor){
        Gson gson = ESOCommon.getGson();
        return CompletableFuture.supplyAsync(() -> {
            Optional<Resource> background = manager.getResource(new ResourceLocation(ESOCommon.MOD_ID, "color-settings/background-color.json"));
            Optional<Resource> slider = manager.getResource(new ResourceLocation(ESOCommon.MOD_ID, "color-settings/slider-colors.json"));
            Optional<Resource> textColor = manager.getResource(new ResourceLocation(ESOCommon.MOD_ID, "color-settings/text-colors.json"));
            try {
                BackgroundColorData bcd = background.isPresent() ? gson.fromJson(background.get().openAsReader(), BackgroundColorData.class) : BackgroundColorData.getDefault();
                SliderColorData scd = slider.isPresent() ? gson.fromJson(slider.get().openAsReader(), SliderColorData.class) : SliderColorData.getDefault();
                TextSettings ts = textColor.isPresent() ? gson.fromJson(textColor.get().openAsReader(), TextSettings.class) : TextSettings.getDefault();
                return new ColorDataHolder(bcd, scd, ts);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new ColorDataHolder(BackgroundColorData.getDefault(), SliderColorData.getDefault(), TextSettings.getDefault());
        }, backgroundExecutor).thenCompose(preparationBarrier::wait).thenAcceptAsync(colorDataHolder -> ESOClient.colorData = colorDataHolder, gameExecutor);
    }
}
