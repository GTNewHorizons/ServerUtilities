package serverutils.utils.integration.kubejs;

// import serverutils.utils.chunks.events.ChunkModifiedEvent;
// import dev.latvian.kubejs.documentation.DocumentationEvent;
// import dev.latvian.kubejs.event.EventsJS;
// import dev.latvian.kubejs.player.AttachPlayerDataEvent;
// import dev.latvian.kubejs.script.BindingsEvent;
// import dev.latvian.kubejs.script.DataType;
// import net.minecraftforge.common.MinecraftForge;
// import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KubeJSIntegration {
    // public static void init()
    // {
    // MinecraftForge.EVENT_BUS.register(KubeJSIntegration.class);
    // }
    //
    // @SubscribeEvent
    // public static void registerBindings(BindingsEvent event)
    // {
    // event.add("serverutilities", new KubeJSServerUtilitiesWrapper());
    // }
    //
    // @SubscribeEvent
    // public static void attachPlayerData(AttachPlayerDataEvent event)
    // {
    // event.add("serverutilities", new KubeJSServerUtilitiesPlayerData(event.getParent()));
    // }
    //
    // @SubscribeEvent
    // public static void registerDocumentation(DocumentationEvent event)
    // {
    // event.registerAttachedData(DataType.PLAYER, "serverutilities", KubeJSServerUtilitiesPlayerData.class);
    // event.registerEvent("serverutilities.chunk.claimed", ChunkModifiedEventJS.class).serverOnly();
    // event.registerEvent("serverutilities.chunk.unclaimed", ChunkModifiedEventJS.class).serverOnly();
    // event.registerEvent("serverutilities.chunk.loaded", ChunkModifiedEventJS.class).serverOnly();
    // event.registerEvent("serverutilities.chunk.unloaded", ChunkModifiedEventJS.class).serverOnly();
    // }
    //
    // @SubscribeEvent
    // public static void onChunkClaimed(ChunkModifiedEvent.Claimed event)
    // {
    // EventsJS.post("serverutilities.chunk.claimed", new ChunkModifiedEventJS(event));
    // }
    //
    // @SubscribeEvent
    // public static void onChunkUnclaimed(ChunkModifiedEvent.Unclaimed event)
    // {
    // EventsJS.post("serverutilities.chunk.unclaimed", new ChunkModifiedEventJS(event));
    // }
    //
    // @SubscribeEvent
    // public static void onChunkLoaded(ChunkModifiedEvent.Loaded event)
    // {
    // EventsJS.post("serverutilities.chunk.loaded", new ChunkModifiedEventJS(event));
    // }
    //
    // @SubscribeEvent
    // public static void onChunkUnloaded(ChunkModifiedEvent.Unloaded event)
    // {
    // EventsJS.post("serverutilities.chunk.unloaded", new ChunkModifiedEventJS(event));
    // }
}
