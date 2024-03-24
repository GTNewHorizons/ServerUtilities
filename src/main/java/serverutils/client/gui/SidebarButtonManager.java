package serverutils.client.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.ServerUtilitiesConfig;
import serverutils.client.ServerUtilitiesClient;
import serverutils.client.ServerUtilitiesResourceType;
import serverutils.client.resource.IResourceType;
import serverutils.client.resource.ISelectiveResourceReloadListener;
import serverutils.events.SidebarButtonCreatedEvent;
import serverutils.lib.io.DataReader;
import serverutils.lib.util.JsonUtils;

public enum SidebarButtonManager implements ISelectiveResourceReloadListener {

    INSTANCE;

    public final List<SidebarButtonGroup> groups = new ArrayList<>();
    private static final String SIDEBAR_BUTTONS_FILE = "sidebar_buttons.json";
    private static final String POSITION = "position";

    @Override
    public void onResourceManagerReload(IResourceManager manager, Predicate<IResourceType> resourcePredicate) {
        if (!resourcePredicate.test(ServerUtilitiesResourceType.SERVERUTILS_CONFIG)) {
            return;
        }

        groups.clear();

        JsonElement element = DataReader.get(
                new File(
                        Minecraft.getMinecraft().mcDataDir,
                        ServerUtilitiesClient.CLIENT_FOLDER + SIDEBAR_BUTTONS_FILE))
                .safeJson();
        JsonObject sidebarButtonConfig;

        if (element.isJsonObject()) {
            sidebarButtonConfig = element.getAsJsonObject();
            if (sidebarButtonConfig.has(POSITION)) {
                JsonObject o = sidebarButtonConfig.get(POSITION).getAsJsonObject();
                if (o.has("posX") && o.has("posY")) {
                    GuiSidebar.dragOffsetX = o.get("posX").getAsInt();
                    GuiSidebar.dragOffsetY = o.get("posY").getAsInt();
                }
            }
        } else {
            sidebarButtonConfig = new JsonObject();
        }

        Map<ResourceLocation, SidebarButtonGroup> groupMap = new HashMap<>();

        for (String domain : manager.getResourceDomains()) {
            try {
                for (IResource resource : manager
                        .getAllResources(new ResourceLocation(domain, "sidebar_button_groups.json"))) {
                    JsonElement json = DataReader.get(resource).json();

                    for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            JsonObject groupJson = entry.getValue().getAsJsonObject();
                            int y = 0;

                            if (groupJson.has("y")) {
                                y = groupJson.get("y").getAsInt();
                            }

                            SidebarButtonGroup group = new SidebarButtonGroup(
                                    new ResourceLocation(domain, entry.getKey()),
                                    y);
                            groupMap.put(group.getId(), group);
                        }
                    }
                }
            } catch (Exception ex) {
                if (!(ex instanceof FileNotFoundException)) {
                    ex.printStackTrace();
                }
            }
        }

        for (String domain : manager.getResourceDomains()) {
            try {
                for (IResource resource : manager.getAllResources(new ResourceLocation(domain, SIDEBAR_BUTTONS_FILE))) {
                    JsonElement json = DataReader.get(resource).json();

                    if (json.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                            if (entry.getValue().isJsonObject()) {
                                JsonObject buttonJson = entry.getValue().getAsJsonObject();

                                if (!buttonJson.has("group")) {
                                    continue;
                                }

                                if (!ServerUtilitiesConfig.debugging.dev_sidebar_buttons && buttonJson.has("dev_only")
                                        && buttonJson.get("dev_only").getAsBoolean()) {
                                    continue;
                                }

                                SidebarButtonGroup group = groupMap
                                        .get(new ResourceLocation(buttonJson.get("group").getAsString()));

                                if (group == null) {
                                    continue;
                                }

                                SidebarButton button = new SidebarButton(
                                        new ResourceLocation(domain, entry.getKey()),
                                        group,
                                        buttonJson);

                                group.getButtons().add(button);

                                if (sidebarButtonConfig.has(button.id.getResourceDomain())) {
                                    JsonElement e = sidebarButtonConfig.get(button.id.getResourceDomain());

                                    if (e.isJsonObject() && e.getAsJsonObject().has(button.id.getResourcePath())) {
                                        button.setConfig(
                                                e.getAsJsonObject().get(button.id.getResourcePath()).getAsBoolean());
                                    }
                                } else if (sidebarButtonConfig.has(button.id.toString())) {
                                    button.setConfig(sidebarButtonConfig.get(button.id.toString()).getAsBoolean());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                if (!(ex instanceof FileNotFoundException)) {
                    ex.printStackTrace();
                }
            }
        }

        for (SidebarButtonGroup group : groupMap.values()) {
            if (!group.getButtons().isEmpty()) {
                group.getButtons().sort(null);
                groups.add(group);
            }
        }

        groups.sort(null);

        for (SidebarButtonGroup group : groups) {
            for (SidebarButton button : group.getButtons()) {
                new SidebarButtonCreatedEvent(button).post();
            }
        }

        saveConfig();
    }

    public void saveConfig() {
        JsonObject o = new JsonObject();

        for (SidebarButtonGroup group : groups) {
            for (SidebarButton button : group.getButtons()) {
                JsonObject o1 = o.getAsJsonObject(button.id.getResourceDomain());

                if (o1 == null) {
                    o1 = new JsonObject();
                    o.add(button.id.getResourceDomain(), o1);
                }

                o1.addProperty(button.id.getResourcePath(), button.getConfig());
            }
        }
        JsonObject o2 = o.getAsJsonObject(POSITION);

        if (o2 == null) {
            o2 = new JsonObject();
            o.add(POSITION, o2);
        }

        o2.addProperty("posX", String.valueOf(GuiSidebar.dragOffsetX));
        o2.addProperty("posY", String.valueOf(GuiSidebar.dragOffsetY));

        JsonUtils.toJsonSafe(
                new File(
                        Minecraft.getMinecraft().mcDataDir,
                        ServerUtilitiesClient.CLIENT_FOLDER + SIDEBAR_BUTTONS_FILE),
                o);
    }
}
