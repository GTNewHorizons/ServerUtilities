package serverutils.lib.api.config;

import java.io.File;

import com.google.gson.JsonElement;

import latmod.lib.LMFileUtils;
import latmod.lib.LMJsonUtils;

public class ConfigFile extends ConfigGroup {

    private File file;

    public ConfigFile(String id) {
        super(id);
    }

    public ConfigFile getConfigFile() {
        return this;
    }

    public void setFile(File f) {
        file = LMFileUtils.newFile(f);
    }

    public File getFile() {
        return file;
    }

    public void load() {
        JsonElement e = LMJsonUtils.fromJson(file);

        if (e.isJsonObject()) {
            ConfigGroup g = new ConfigGroup(getID());
            g.func_152753_a(e.getAsJsonObject());
            loadFromGroup(g, false);
        }
    }

    public void save() {
        if (file != null) LMJsonUtils.toJson(file, getSerializableElement());
    }

    public void addGroup(String id, Class<?> c) {
        add(new ConfigGroup(id).addAll(c, null, false), false);
    }
}
