package cc.meteormc.connector;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin {
    private static Main PLUGIN;
    private static final Map<String, Group> GROUPS = new HashMap<>();

    @Override
    public void onEnable() {
        PLUGIN = this;
        getCommand("connector").setExecutor(this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        saveDefaultConfig();
        reloadConfig();

        for (String group : getConfig().getConfigurationSection("groups").getKeys(false)) {
            Group gp = new Group(group);
            for (String server : getConfig().getConfigurationSection("groups." + group).getKeys(false)) {
                gp.addServer(new Server(getConfig().getString("groups." + group + "." + server + ".address", ""), server, ChatColor.translateAlternateColorCodes('&', getConfig().getString("groups." + group + "." + server + ".name", ""))));
            }

            GROUPS.put(group, gp);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return true;
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        switch (args[0]) {
            case "join":
                if (!player.hasPermission("connector.join")) break;
                if (args.length > 1) {
                    //noinspection UnstableApiUsage
                    ByteArrayDataOutput data = ByteStreams.newDataOutput();
                    data.writeUTF("Connect");
                    data.writeUTF(args[1]);
                    player.sendPluginMessage(Main.getPlugin(), "BungeeCord", data.toByteArray());
                }

                break;
            case "random":
                if (!player.hasPermission("connector.random-join")) break;
                if (args.length > 1 && GROUPS.containsKey(args[1])) {
                    List<Server> servers = new ArrayList<>(GROUPS.get(args[1]).getServers());
                    servers.sort(Comparator.comparingInt(Server::getPlayerCount));
                    for (Server server : GROUPS.get(args[1]).getServers()) {
                        if (server.connect(player, false)) return true;
                    } player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("message.no-available-servers", "")));
                }

                break;
            default:
                if (!player.hasPermission("connector.open-gui")) break;
                if (GROUPS.containsKey(args[0])) GROUPS.get(args[0]).openGUI(player);
        }

        return true;
    }

    public static Plugin getPlugin() {
        return PLUGIN;
    }
}
