package serverutils.utils.badges;

import net.minecraftforge.client.event.RenderPlayerEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.*;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.utils.mod.client.ServerUtilitiesClient;
import serverutils.utils.world.*;

@SideOnly(Side.CLIENT)
public class BadgeRenderer {

    public static final BadgeRenderer instance = new BadgeRenderer();

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Specials.Post e) {
        if (ServerUtilitiesLibraryClient.isIngameWithServerUtilities()
                && ServerUtilitiesClient.render_badges.getAsBoolean()
                && !e.entityPlayer.isInvisible()) {
            Badge b = ClientBadges.getClientBadge(e.entityPlayer.getGameProfile().getId());

            if (b != null && b != Badge.emptyBadge) {
                LMPlayerClient pc = LMWorldClient.inst.getPlayer(e.entityPlayer);

                if (pc != null && pc.renderBadge) {
                    b.onPlayerRender(e.entityPlayer);
                }
            }
        }
    }
}
