package aiefu.enchantmentoverhaul;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import org.spongepowered.include.com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class EnchantmentOverhaul implements ModInitializer {

	public static final String MOD_ID = "enchantment-overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation c2s_enchant_item = new ResourceLocation(MOD_ID, "c2s_enchant_item");

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
					Gson gson = new Gson();
					data.forEach((key, value) -> {
						try {
							gson.fromJson(value.openAsReader(), RecipeHolder.class).processData();
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
		ServerPlayNetworking.registerGlobalReceiver(c2s_enchant_item, (server, player, handler, buf, responseSender) -> {
			String s = buf.readUtf();
			ResourceLocation location = new ResourceLocation(s);
			server.execute(() -> {
				if(player.containerMenu instanceof OverhauledEnchantmentMenu m){
					m.checkRequirementsAndConsume(location, player);
				}
			});
		});
		try {
			this.genConfig();
			this.readConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Hello Fabric world!");
	}

	public void genConfig() throws IOException {
		Path p = Paths.get("./config/enchantment-overhaul");
		if(!Files.exists(p)){
			Files.createDirectory(p);
		}
		String p2 = "./config/enchantment-overhaul/config.json";
		if(!Files.exists(Paths.get(p2))){
			try(FileWriter writer = new FileWriter(p2)){
				gson.toJson(ConfigurationFile.getDefault(), writer);
			}
		}
	}

	public void readConfig() throws FileNotFoundException {
		EnchantmentOverhaul.config = gson.fromJson(new FileReader("./config/enchantment-overhaul/config.json"), ConfigurationFile.class);
	}
}