Download the latest JAR from [releases](https://github.com/GTNewHorizons/ServerUtilities/releases) - you want the one named ServerUtilities-number.jar, not the `dev` or `sources`. Place it in the `/mods` folder on your server AND client (if single player, on client only).

Once loaded, you will find new UI options in your inventory screen:

![image](https://github.com/user-attachments/assets/e6bfcf21-4489-4210-bee1-b33c3a054611)

For single player, loading chunks is the most important, and you can click that icon and then shift-click (and drag) to designate chunks to load. Right click to unload and unclaim.

The ServerUtilities configuration files are found in `.minecraft/serverutilities` and it will migrate from FTBU for you. The most likely thing you want to change is found in `.minecraft/serverutilties/server/ranks.txt` where you can control how many chunks can be loaded. `.minecraft/serverutilities/serverutilities.cfg` is where you can disable backups if you have another backup solution.

For server admins, there is much more that ServerUtilities can do, refer to the FTBU documentation for now.

For an example of how the `ranks.txt` could be formatted on a server, with different roles for different permissions look [Here](https://github.com/user-attachments/files/18085428/ranks.txt).

Warning: The example file is made in version 2.7.0 of GTNH and should not be expected to work straight out of the box with different modpacks or versions. 

Some editing IS required. 

But it should give you some idea on how the permission system works.
