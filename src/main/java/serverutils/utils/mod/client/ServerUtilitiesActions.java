package serverutils.utils.mod.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.*;
import serverutils.lib.*;
import serverutils.lib.api.PlayerAction;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.friends.ILMPlayer;
import serverutils.lib.api.gui.*;
import serverutils.utils.mod.client.gui.claims.GuiClaimChunks;
import serverutils.utils.mod.client.gui.friends.GuideFriendsGUI;
import serverutils.utils.mod.client.gui.guide.GuiGuide;
import serverutils.utils.net.ClientAction;

public class ServerUtilitiesActions {

    @SideOnly(Side.CLIENT)
    public static void init() {
        PlayerActionRegistry.add(friends_gui);
        PlayerActionRegistry.add(guide);
        PlayerActionRegistry.add(info);
        PlayerActionRegistry.add(claims);

        PlayerActionRegistry.add(friend_add);
        PlayerActionRegistry.add(friend_remove);
        PlayerActionRegistry.add(friend_deny);

        if (ServerUtilitiesLib.DEV_ENV) {
            PlayerActionRegistry.add(mail);
            PlayerActionRegistry.add(trade);
        }

        GuiScreenRegistry.register("friends_gui", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                return new GuiGuide(null, new GuideFriendsGUI());
            }
        });

        GuiScreenRegistry.register("claimed_chunks", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                return new GuiClaimChunks(0L);
            }
        });

        GuiScreenRegistry.register("guide", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                return GuiGuide.openClientGui(false);
            }
        });

        GuiScreenRegistry.register("server_info", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                ClientAction.REQUEST_SERVER_INFO.send(0);
                return null;
            }
        });

        /*
         * GuiScreenRegistry.register("trade", new GuiScreenRegistry.Entry() { public GuiScreen openGui(EntityPlayer ep)
         * { return ServerUtilitiesLibraryClient.mc.currentScreen; } });
         */
    }

    // Self //

    public static final PlayerAction friends_gui = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverutils.friends_gui",
            950,
            TextureCoords.getSquareIcon(new ResourceLocation("serverutils", "textures/gui/friendsbutton.png"), 256)) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ServerUtilitiesLibraryClient.openGui(new GuiGuide(null, new GuideFriendsGUI()));
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities();
        }

        public String getDisplayName() {
            return "FriendsGUI";
        }
    };

    public static final PlayerAction guide = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverutils.guide",
            0,
            GuiIcons.book) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ServerUtilitiesLibraryClient.playClickSound();
            GuiGuide.openClientGui(true);
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities();
        }

        public Boolean configDefault() {
            return Boolean.TRUE;
        }
    };

    public static final PlayerAction info = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverutils.server_info",
            0,
            GuiIcons.book_red) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ClientAction.REQUEST_SERVER_INFO.send(0);
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities();
        }

        public Boolean configDefault() {
            return Boolean.TRUE;
        }
    };

    public static final PlayerAction claims = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverutils.claimed_chunks",
            0,
            GuiIcons.map) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ServerUtilitiesLibraryClient.openGui(new GuiClaimChunks(0L));
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities();
        }

        public Boolean configDefault() {
            return Boolean.TRUE;
        }
    };

    public static final PlayerAction trade = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverutils.trade",
            0,
            GuiIcons.money_bag) {

        public void onClicked(ILMPlayer owner, ILMPlayer player) {}

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLib.DEV_ENV;
        }

        public Boolean configDefault() {
            return Boolean.TRUE;
        }
    };

    // Other //

    public static final PlayerAction friend_add = new PlayerAction(
            PlayerAction.Type.OTHER,
            "serverutils.add_friend",
            1,
            GuiIcons.add) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ClientAction.ADD_FRIEND.send(other.getPlayerID());
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities() && !self.isFriendRaw(other);
        }
    };

    public static final PlayerAction friend_remove = new PlayerAction(
            PlayerAction.Type.OTHER,
            "serverutils.rem_friend",
            -1,
            GuiIcons.remove) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ClientAction.REM_FRIEND.send(other.getPlayerID());
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities() && self.isFriendRaw(other);
        }
    };

    public static final PlayerAction friend_deny = new PlayerAction(
            PlayerAction.Type.OTHER,
            "serverutils.deny_friend",
            -1,
            GuiIcons.remove) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ClientAction.DENY_FRIEND.send(other.getPlayerID());
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLibraryClient.isIngameWithServerUtilities() && !self.isFriendRaw(other)
                    && other.isFriendRaw(self);
        }
    };

    public static final PlayerAction mail = new PlayerAction(
            PlayerAction.Type.OTHER,
            "serverutils.mail",
            0,
            GuiIcons.feather) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {}

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return ServerUtilitiesLib.DEV_ENV;
        }
    };
}
