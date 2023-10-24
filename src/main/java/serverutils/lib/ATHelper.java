package serverutils.lib;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public final class ATHelper {

    public static List<ICrafting> getContainerListeners(Container container) {
        return container.crafters;
    }

    public static EnumChatFormatting getEnumChatFormattingFromDyeColor(EnumDyeColor color) {
        return color.chatFormatting;
    }

    public static char getEnumChatFormattingChar(EnumChatFormatting formatting) {
        return formatting.formattingCode;
    }

    @SuppressWarnings("unchecked")
    public static Set<ICommand> getCommandSet(CommandHandler handler) {
        return handler.commandSet;
    }

    public static boolean areCommandsAllowedForAll(ServerConfigurationManager ServerConfigurationManager) {
        return ServerConfigurationManager.commandsAllowedForAll;
    }

    @Nullable
    public static Boolean getBold(ChatStyle style) {
        return style.bold;
    }

    @Nullable
    public static Boolean getItalic(ChatStyle style) {
        return style.italic;
    }

    @Nullable
    public static Boolean getStriketrough(ChatStyle style) {
        return style.strikethrough;
    }

    @Nullable
    public static Boolean getUnderlined(ChatStyle style) {
        return style.underlined;
    }

    @Nullable
    public static Boolean getObfuscated(ChatStyle style) {
        return style.obfuscated;
    }

    @Nullable
    public static EnumChatFormatting getColor(ChatStyle style) {
        return style.color;
    }

    @Nullable
    public static ClickEvent getClickEvent(ChatStyle style) {
        return style.chatClickEvent;
    }

    @Nullable
    public static HoverEvent getHoverEvent(ChatStyle style) {
        return style.chatHoverEvent;
    }
}
