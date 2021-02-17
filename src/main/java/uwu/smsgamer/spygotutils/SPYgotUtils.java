package uwu.smsgamer.spygotutils;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packettype.PacketType;
import org.bukkit.Bukkit;
import uwu.smsgamer.spygotutils.commands.CommandManager;
import uwu.smsgamer.spygotutils.config.ConfigManager;
import uwu.smsgamer.spygotutils.listener.*;
import uwu.smsgamer.spygotutils.managers.*;
import uwu.smsgamer.spygotutils.utils.*;
import uwu.smsgamer.spygotutils.utils.python.spigot.*;

import java.io.File;

public final class SPYgotUtils {
    private static SPYgotUtils INSTANCE;

    public static SPYgotUtils getInstance() {
        return INSTANCE;
    }

    public final SpigotLoader spigotPlugin;
    public final BungeeLoader bungeePlugin;
    public final boolean onSpigot;
    public boolean firstLoad;

    public SPYgotUtils(SpigotLoader spigot, BungeeLoader bungee, boolean onSpigot) {
        INSTANCE = this;
        spigotPlugin = spigot;
        bungeePlugin = bungee;
        this.onSpigot = onSpigot;
    }

    public static Loader getLoader() {
        SPYgotUtils instance = getInstance();
        return instance.onSpigot ? instance.spigotPlugin : instance.bungeePlugin;
    }

    public void onLoad() {
        if (onSpigot) {
            /*
             * PacketEvents likes to maintain compatibility with other plugins shading the same API.
             * If some other API loaded before you, the create method will return it's PacketEvents instance
             * to maintain compatibility, otherwise you will create the instance.
             * Set the settings.
             */
            //When a player injection fails, what should happen?
            PacketEvents.create(spigotPlugin).getSettings().checkForUpdates(true) //We won't check for updates
              .injectionFailureReaction(player -> {
                  /*
                   * NOTE: We are OFF the main thread. Kicking is not thread safe. We will switch over to the main thread to kick.
                   * By default if you don't specify a reaction, PacketEvents will kick them! If you need to modify the kick message or the whole action,
                   * use this feature!
                   * You NOT kicking the player will result in them staying on the server, and PacketEvents won't notice if they are sending any packets.
                   * We also won't notice if the server is sending any packets to that player.
                   */
                  Bukkit.getScheduler().runTask(spigotPlugin, () -> player.kickPlayer("Failed to inject. Please rejoin!"));
              });
            /*
             * Access the stored instance of the PacketEvents class and load PacketEvents.
             * A few settings need to be specified before loading PacketEvents
             * as PacketEvents already uses a few in the load method.
             * To be safe, just set them all before loading.
             */
            PacketEvents.get().load();
        }

        firstLoad = !getDataFolder().exists();

        ConfigManager.setup("messages", "chat-filter", "py-settings");

        ChatFilterManager.getInstance();
        ChatUtils.init();
        EvalUtils.init();
        PythonManager.init();
        PyListener.init();

        if (firstLoad) scriptFiles();

        PythonManager.loadScripts();
    }

    public void onEnable() {

        //Initiate PacketEvents
        PacketEvents.get().init(spigotPlugin);

        //Register our listener (class extending PacketListenerDynamic)
        //By default it is configured to listen to all packets with no filter at all.
        PacketProcessor packetProcessor = new PacketProcessor();
        //We will FILTER all packets. We don't want to listen to anything.
        packetProcessor.filterAll();
        //Now since the filter has been applied, we are no longer listening to all packets. We can now "whitelist" our wanted packets.
        //Using this feature can improve performance.

        packetProcessor.addServerSidedPlayFilter(PacketType.Play.Server.CHAT);

        PacketEvents.get().registerListener(packetProcessor);

        PycketListener pycketListener = PycketListener.getInstance();
        PacketEvents.get().registerListener(pycketListener);

        PythonManager.onEnable();

        Bukkit.getPluginManager().registerEvents(BukkitListener.getInstance(), spigotPlugin);

        CommandManager.setupCommands();

        if (firstLoad) configFiles();
    }

    public void onDisable() {
        PythonManager.onDisable();

        //Terminate PacketEvents
        PacketEvents.get().terminate();

        if (ConfigManager.getConfig("py-settings").getBoolean("remove-classes-on-disable")) {
            for (File file : new File(SPYgotUtils.getInstance().spigotPlugin.getDataFolder(), "scripts")
              .listFiles(pathname -> pathname.getName().endsWith("$py.class")))
                file.delete();
        }
    }

    private void scriptFiles() {
        // Shitty ik but I'm lazy.
        FileUtils.saveResource(spigotPlugin, "event.py", new File(spigotPlugin.getDataFolder(), "scripts/event.py"), false);
        FileUtils.saveResource(spigotPlugin, "command.py", new File(spigotPlugin.getDataFolder(), "scripts/command.py"), false);
        FileUtils.saveResource(spigotPlugin, "packet.py", new File(spigotPlugin.getDataFolder(), "scripts/packet.py"), false);
        FileUtils.saveResource(spigotPlugin, "test.py", new File(spigotPlugin.getDataFolder(), "scripts/test.py"), false);
        FileUtils.saveResource(spigotPlugin, "itest.py", new File(spigotPlugin.getDataFolder(), "scripts/itest.py"), false);
    }

    private void configFiles() {
        ConfigManager.saveConfig("messages");
        ConfigManager.saveConfig("py-settings");
    }

    public File getDataFolder() {
        return onSpigot ? spigotPlugin.getDataFolder() : bungeePlugin.getDataFolder();
    }
}
