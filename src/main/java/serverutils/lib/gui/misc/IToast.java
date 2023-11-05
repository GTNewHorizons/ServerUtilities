package serverutils.lib.gui.misc;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public interface IToast {

    ResourceLocation TEXTURE_TOASTS = new ResourceLocation("textures/gui/toasts.png");
    Object NO_TOKEN = new Object();

    Visibility draw(GuiToast toastGui, long delta);

    default Object getType() {
        return NO_TOKEN;
    }

    public static enum Visibility {

        SHOW(new ResourceLocation("ui.toast.in")),
        HIDE(new ResourceLocation("ui.toast.out"));

        private final ResourceLocation sound;

        private Visibility(ResourceLocation soundIn) {
            this.sound = soundIn;
        }

        public void playSound(SoundHandler handler) {
            handler.playSound(PositionedSoundRecord.func_147673_a(this.sound));
        }
    }
}
