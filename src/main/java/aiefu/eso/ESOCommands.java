package aiefu.eso;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;

public class ESOCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("learn")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enchantment-id", StringArgumentType.greedyString())
                                .executes(ctx -> learnEnchantmentById(ctx, EntityArgument.getPlayer(ctx,"player"), StringArgumentType.getString(ctx,"enchantment-id")))))));
        dispatcher.register(Commands.literal("eso").requires(stack -> stack.hasPermission(4)).then(Commands.literal("forget")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enchantment-id", StringArgumentType.greedyString())
                                .executes(ctx -> forgetEnchantment(ctx, EntityArgument.getPlayer(ctx,"player"), StringArgumentType.getString(ctx,"enchantment-id")))))));
    }

    public static int forgetEnchantment(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id){
        if(id.equalsIgnoreCase("all")){
            revokeAll(ctx, player);
        } else {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(id));
            if(enchantment != null && player instanceof IServerPlayerAcc acc){
                MutableComponent discId = Component.translatable(enchantment.getDescriptionId());
                MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
                c.append(discId);
                c.append(Component.literal("]"));
                if(acc.enchantment_overhaul$getUnlockedEnchantments().remove(enchantment)){
                    player.sendSystemMessage(Component.translatable("eso.youforgot", c), true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.removedEnchantment", discId, player.getDisplayName()), true);
                } else ctx.getSource().sendFailure(Component.translatable("eso.command.feedback.doesnotknow", player.getDisplayName(), discId));
            } else ctx.getSource().sendFailure(Component.translatable("eso.command.encantmentnotfound", id));
        }
        return 0;
    }

    public static int learnEnchantmentById(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id){
        if(id.equalsIgnoreCase("all")){
            grantAll(ctx, player);
        } else {
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(id));
            if(enchantment != null && player instanceof IServerPlayerAcc acc){
                MutableComponent discId = Component.translatable(enchantment.getDescriptionId());
                MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
                c.append(discId);
                c.append(Component.literal("]"));
                if(acc.enchantment_overhaul$getUnlockedEnchantments().add(enchantment)) {
                    player.sendSystemMessage(Component.translatable("eso.youlearned", c).withStyle(ChatFormatting.GOLD), true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.addedEnchantment", discId, player.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
                } else {
                    ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.playerknows", player.getDisplayName(), discId).withStyle(ChatFormatting.DARK_GREEN), true);
                }
            } else ctx.getSource().sendFailure(Component.translatable("eso.command.encantmentnotfound", id));
        }
        return 0;
    }

    public static void grantAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player){
        if(player instanceof IServerPlayerAcc acc){
            HashSet<Enchantment> enchantments = acc.enchantment_overhaul$getUnlockedEnchantments();
            for (Enchantment e: BuiltInRegistries.ENCHANTMENT){
                enchantments.add(e);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.grantall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.allknowledge").withStyle(ChatFormatting.GOLD));
        }
    }

    public static void revokeAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player){
        if(player instanceof IServerPlayerAcc acc){
            acc.enchantment_overhaul$getUnlockedEnchantments().clear();
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.revokeall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.lostallknowledge").withStyle(ChatFormatting.GOLD));
        }
    }
}
