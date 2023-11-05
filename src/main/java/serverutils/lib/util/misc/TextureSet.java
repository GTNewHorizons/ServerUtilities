package serverutils.lib.util.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Facing;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;

import serverutils.lib.util.StringUtils;

public class TextureSet {

    public static final TextureSet DEFAULT = TextureSet.of("all=blocks/planks_oak");

    public static TextureSet of(String v) {
        TextureSet set = new TextureSet();
        Map<String, String> map = StringUtils.parse(StringUtils.TEMP_MAP, v);

        String s = map.get("all");

        if (s != null) {
            ResourceLocation tex = new ResourceLocation(s);

            for (int i = 0; i < 6; i++) {
                set.textures[i] = tex;
            }
        }

        for (EnumFacing facing : EnumFacing.faceList) {
            s = map.get(Facing.facings[facing.order_a]);

            if (s != null) {
                set.textures[facing.ordinal()] = new ResourceLocation(s);
            }
        }

        return set;
    }

    public static TextureSet of(JsonElement json) {
        return of(json.getAsString());
    }

    public static TextureSet of(int x, int y, int z) {
        return DEFAULT; // FIXME
    }

    public final ResourceLocation[] textures;

    private TextureSet() {
        textures = new ResourceLocation[6];
    }

    @Nullable
    public ResourceLocation getTexture(EnumFacing face) {
        return textures[face.ordinal()];
    }

    public Collection<ResourceLocation> getTextures() {
        List<ResourceLocation> list = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            if (textures[i] != null) {
                list.add(textures[i]);
            }
        }

        return list;
    }
}
