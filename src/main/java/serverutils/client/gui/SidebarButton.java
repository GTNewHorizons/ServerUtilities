package serverutils.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.client.EnumSidebarLocation;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.OtherMods;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.misc.GuiLoading;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.ChainedBooleanSupplier;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.SidedUtils;

public class SidebarButton implements Comparable<SidebarButton> {

    public static final BooleanSupplier NEI_NOT_LOADED = () -> !OtherMods.isNEILoaded();

    public final ResourceLocation id;
    public final SidebarButtonGroup group;
    private Icon icon = Icon.EMPTY;
    private int x = 0;
    private boolean defaultConfig = true;
    private boolean configValue = true;
    private final List<String> clickEvents = new ArrayList<>();
    private final List<String> shiftClickEvents = new ArrayList<>();
    private final boolean loadingScreen;
    private ChainedBooleanSupplier visible = ChainedBooleanSupplier.TRUE;
    private ChainedBooleanSupplier disabled = ChainedBooleanSupplier.FALSE;
    private Supplier<String> customTextHandler = null;
    private Consumer<List<String>> tooltipHandler = null;

    public SidebarButton(ResourceLocation _id, SidebarButtonGroup g, JsonObject json) {
        group = g;
        id = _id;

        if (json.has("icon")) {
            icon = Icon.getIcon(json.get("icon"));
        }

        if (icon.isEmpty()) {
            icon = GuiIcons.ACCEPT_GRAY;
        }

        if (json.has("click")) {
            for (JsonElement e : JsonUtils.toArray(json.get("click"))) {
                if (e.isJsonPrimitive()) {
                    clickEvents.add(e.getAsString());
                } else {
                    clickEvents.add(GuiHelper.clickEventToString(JsonUtils.deserializeClickEvent(e)));
                }
            }
        }
        if (json.has("shift_click")) {
            for (JsonElement e : JsonUtils.toArray(json.get("shift_click"))) {
                if (e.isJsonPrimitive()) {
                    shiftClickEvents.add(e.getAsString());
                } else {
                    shiftClickEvents.add(GuiHelper.clickEventToString(JsonUtils.deserializeClickEvent(e)));
                }
            }
        }
        if (json.has("config")) {
            defaultConfig = configValue = json.get("config").getAsBoolean();
        }

        if (json.has("x")) {
            x = json.get("x").getAsInt();
        }

        if (json.has("requires_op") && json.get("requires_op").getAsBoolean()) {
            addVisibilityCondition(ClientUtils.IS_CLIENT_OP);
        }

        if (json.has("hide_with_nei") && json.get("hide_with_nei").getAsBoolean()) {
            addVisibilityCondition(NEI_NOT_LOADED);
        }

        if (json.has("hide_if_server_disabled") && json.get("hide_if_server_disabled").getAsBoolean()) {
            addDisabledCondition(() -> !SidedUtils.isButtonEnabledOnServer(id));
        }

        if (json.has("required_server_mods")) {
            LinkedHashSet<String> requiredServerMods = new LinkedHashSet<>();

            for (JsonElement e : JsonUtils.toArray(json.get("required_server_mods"))) {
                requiredServerMods.add(e.getAsString());
            }

            addVisibilityCondition(() -> SidedUtils.areAllModsLoadedOnServer(requiredServerMods));
        }

        loadingScreen = json.has("loading_screen") && json.get("loading_screen").getAsBoolean();
    }

    public void addVisibilityCondition(BooleanSupplier supplier) {
        visible = visible.and(supplier);
    }

    public void addDisabledCondition(BooleanSupplier supplier) {
        disabled = disabled.or(supplier);
    }

    public String getLangKey() {
        return "sidebar_button." + id.getResourceDomain() + '.' + id.getResourcePath();
    }

    public String getTooltipLangKey() {
        return getLangKey() + ".tooltip";
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        return o == this || o instanceof SidebarButton button && id.equals(button.id);
    }

    public Icon getIcon() {
        return isDisabled() ? icon.withColor(Color4I.DARK_GRAY.addBrightness(0.1f)) : icon;
    }

    public int getX() {
        return x;
    }

    public boolean getDefaultConfig() {
        return defaultConfig;
    }

    public void onClicked(boolean shift) {
        if (isDisabled()) {
            return;
        }

        if (loadingScreen) {
            new GuiLoading(I18n.format(getLangKey())).openGui();
        }

        for (String event : (shift && !shiftClickEvents.isEmpty() ? shiftClickEvents : clickEvents)) {
            GuiHelper.BLANK_GUI.handleClick(event);
        }
    }

    public boolean isActuallyVisible() {
        return configValue && ServerUtilitiesClientConfig.sidebar_buttons != EnumSidebarLocation.DISABLED
                && isVisible();
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public boolean isDisabled() {
        return disabled.getAsBoolean();
    }

    public boolean getConfig() {
        return configValue;
    }

    public void setConfig(boolean value) {
        configValue = value;
    }

    @Nullable
    public Supplier<String> getCustomTextHandler() {
        return customTextHandler;
    }

    public void setCustomTextHandler(Supplier<String> text) {
        customTextHandler = text;
    }

    @Nullable
    public Consumer<List<String>> getTooltipHandler() {
        return tooltipHandler;
    }

    public void setTooltipHandler(Consumer<List<String>> text) {
        tooltipHandler = text;
    }

    @Override
    public int compareTo(SidebarButton button) {
        return getX() - button.getX();
    }
}
