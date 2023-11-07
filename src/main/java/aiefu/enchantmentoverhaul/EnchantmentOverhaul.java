package aiefu.enchantmentoverhaul;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class EnchantmentOverhaul implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "enchantment-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ConfigurationFile config;

	public static ConcurrentHashMap<ResourceLocation, RecipeHolder> recipeMap = new ConcurrentHashMap<>();


	public static final ExtendedScreenHandlerType<OverhauledEnchantmentMenu> enchantment_menu_ovr =
			Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, "enchs_menu_ovr"), new ExtendedScreenHandlerType<>(OverhauledEnchantmentMenu::new));


	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Map<ResourceLocation, Resource>>() {


			@Override
			public CompletableFuture<Map<ResourceLocation, Resource>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
				return CompletableFuture.supplyAsync(() -> manager.listResources("ench-recipes", resourceLocation -> resourceLocation.getPath().endsWith(".json")), executor);
			}

			@Override
			public CompletableFuture<Void> apply(Map<ResourceLocation, Resource> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
				return CompletableFuture.runAsync(() -> {
					EnchantmentOverhaul.recipeMap.clear();
					data.forEach((key, value) -> {
						try {
							new Gson().fromJson(value.openAsReader(), RecipeHolder.class).processData();
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					});
				}, executor);
			}

			@Override
			public ResourceLocation getFabricId() {
				return new ResourceLocation(MOD_ID, "enchantment_recipe_loader");
			}

		});
		LOGGER.info("Hello Fabric world!");
	}
}