package serverutils.lib.lib;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public final class ATHelper {

    // public static List<IContainerListener> getContainerListeners(Container
    // container) {
    // return container.listeners;
    // }

    public static EnumChatFormatting getEnumChatFormattingFromDyeColor(EnumDyeColor color) {
        return color.chatFormatting;
    }

    public static char getEnumChatFormattingChar(EnumChatFormatting formatting) {
        return formatting.getFormattingCode(); // formattingCode
    }

    @SuppressWarnings("unchecked")
    public static Set<ICommand> getCommandSet(CommandHandler handler) {
        return handler.getCommands().keySet();
    }

    // public static boolean areCommandsAllowedForAll(ServerConfigurationManager ServerConfigurationManager) {
    // return ServerUtilitiesLibConfig.general.enable_cheats;
    // //return ServerConfigurationManager.commandsAllowedForAll;
    // }

    @Nullable
    public static Boolean getBold(ChatStyle style) {
        return style.getBold();
    }

    @Nullable
    public static Boolean getItalic(ChatStyle style) {
        return style.getItalic();
    }

    @Nullable
    public static Boolean getStriketrough(ChatStyle style) {
        return style.getStrikethrough();
    }

    @Nullable
    public static Boolean getUnderlined(ChatStyle style) {
        return style.getUnderlined();
    }

    @Nullable
    public static Boolean getObfuscated(ChatStyle style) {
        return style.getObfuscated();
    }

    @Nullable
    public static EnumChatFormatting getColor(ChatStyle style) {
        return style.getColor();
    }

    @Nullable
    public static ClickEvent getClickEvent(ChatStyle style) {
        return style.getChatClickEvent();
    }

    @Nullable
    public static HoverEvent getHoverEvent(ChatStyle style) {
        return style.getChatHoverEvent();
    }

    // @Nullable
    // public static String getInsertion(ChatStyle style) {
    // return style.insertion;
    // }
}
