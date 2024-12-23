package com.meteor.connector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUI implements InventoryHolder, Listener {
    private final Inventory inventory;
    private final Map<Integer, Action> actions = new HashMap<>();

    private static final Map<UUID, Long> cooldownMap = new HashMap<>();

    GUI(String title, int size) {
        this.inventory = Bukkit.createInventory(this, size, title);
        Bukkit.getPluginManager().registerEvents(this, Main.getPlugin());
    }

    public static GUI create(String title, int rows) {
        return new GUI(title, rows * 9);
    }

    public GUI setItem(int slot, ItemStack item) {
        this.inventory.setItem(slot, item);
        return this;
    }

    public GUI setAction(int slot, Action action) {
        this.actions.put(slot, action);
        return this;
    }

    public GUI setSlot(int slot, ItemStack item, Action action) {
        this.setItem(slot, item);
        this.setAction(slot, action);
        return this;
    }

    public GUI open(Player player) {
        player.openInventory(this.inventory);
        return this;
    }

    @EventHandler
    public void handleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getInventory().getHolder() instanceof GUI) {
            e.setCancelled(true);
            if (System.currentTimeMillis() - cooldownMap.getOrDefault(e.getWhoClicked().getUniqueId(), 0L) <= 1000) return;
            int slot = e.getSlot();
            GUI gui = (GUI) e.getInventory().getHolder();
            if (gui.actions.containsKey(slot)) {
                gui.actions.get(slot).onClick(e);
                cooldownMap.put(e.getWhoClicked().getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public abstract static class Action {
        public abstract void onClick(InventoryClickEvent e);
    }
}
