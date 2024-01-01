Download the latest JAR from [releases](https://github.com/GTNewHorizons/ServerUtilities/releases) - you want the one named ServerUtilities-number.jar, not the `dev` or `sources`. Place it in the `/mods` folder on your server AND client (if single player, on client only).

Once loaded, you will find new UI options in your inventory screen:

![ServerUtilities GUI](claimed_chunks.png)

For single player, loading chunks is the most important, and you can click that icon and then shift-click (and drag) to designate chunks to load. Right click to unload and unclaim.

The ServerUtilities configuration files are found in `.minecraft/serverutilities` and it will migrate from FTBU for you. The most likely thing you want to change is found in `.minecraft/serverutilties/server/ranks.txt` where you can control how many chunks can be loaded. `.minecraft/serverutilities/serverutlities.cfg` is where you can disable backups if you have another backup solution.

For server admins, there is much more that ServerUtilities can do, refer to the FTBU documentation for now.
