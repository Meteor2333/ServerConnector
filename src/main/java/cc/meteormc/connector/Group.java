package cc.meteormc.connector;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Group extends BukkitRunnable {
    private final GUI gui;
    private final String name;
    private final List<Server> servers = new ArrayList<>();

    private static final List<Integer> GUI_SLOTS = Arrays.asList(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    );

    public Group(String name) {
        this.name = name;
        ItemStack rjItem = Optional.ofNullable(XMaterial.CYAN_WOOL.parseItem()).orElse(new ItemStack(Material.AIR));
        ItemMeta im = rjItem.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("message.random-join", "")));
        rjItem.setItemMeta(im);
        this.gui = GUI.create(ChatColor.translateAlternateColorCodes('&', name), 6).setSlot(4, rjItem, new GUI.Action() {
            @Override
            public void onClick(InventoryClickEvent e) {
                Bukkit.dispatchCommand(e.getWhoClicked(), "connector random_join " + Group.this.name);
            }
        });
        this.runTaskTimer(Main.getPlugin(), 0L, 60L);
    }

    public void addServer(Server server) {
        this.servers.add(server);
    }

    public List<Server> getServers() {
        return this.servers;
    }

    public void openGUI(Player player) {
        this.gui.open(player);
    }

    public void run() {
        this.servers.sort((a, b) -> {
            if (a.getState() != b.getState()) return Integer.compare(a.getState().ordinal(), b.getState().ordinal());
            else return a.getName().compareTo(b.getName());
        });
        //this.servers.sort(Comparator.comparingInt(server -> server.getState().ordinal()));
        for (int i = 0; i < Math.min(21, this.servers.size()); i++) {
            Server server = this.servers.get(i);
            this.gui.setSlot(GUI_SLOTS.get(i), server.getGUIItem(), new GUI.Action() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    server.connect((Player) e.getWhoClicked(), true);
                }
            });
        }
    }
}
