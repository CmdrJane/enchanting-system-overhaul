package aiefu.eso.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientLanguage.class)
public interface IClientLanguageAcc {

    @Accessor("storage")
    Map<String, String> getLanguageMap();

    @Mutable
    @Accessor("storage")
    void setLanguageMap(Map<String, String> map);
}
