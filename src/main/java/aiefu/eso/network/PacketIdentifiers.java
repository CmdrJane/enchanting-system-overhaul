package aiefu.eso.network;

import aiefu.eso.ESOCommon;
import net.minecraft.resources.ResourceLocation;

public class PacketIdentifiers {
    //Client to server
    public static final ResourceLocation c2s_enchant_item = new ResourceLocation(ESOCommon.MOD_ID, "c2s_enchant_item");
    //Server to client
    public static final ResourceLocation s2c_data_sync = new ResourceLocation(ESOCommon.MOD_ID, "s2c_data_sync");
    public static final ResourceLocation s2c_mat_config_sync = new ResourceLocation(ESOCommon.MOD_ID, "s2c_mat_config_sync");
    public static final ResourceLocation s2c_string_to_clipboard = new ResourceLocation(ESOCommon.MOD_ID, "s2c_string_to_clipboard");
    public static final ResourceLocation s2c_sync_config = new ResourceLocation(ESOCommon.MOD_ID, "s2c_sync_config");
}
