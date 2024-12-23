package com.meteor.connector;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Server {
    private String motd = "";
    private int playerCount = 0, playerMaxCount = 0;
    private ServerState state = ServerState.NOT_FOUND;

    private final InetSocketAddress address;
    private final String server;
    private final String name;

    public Server(String address, String server, String name) {
        String[] split = address.split(":");
        this.address = new InetSocketAddress(split[0], split.length > 1 ? Integer.parseInt(split[1]) : 25565);
        this.server = server;
        this.name = ChatColor.stripColor(name);
        if (!this.refreshState()) Main.getPlugin().getLogger().warning("Can't connect to server: " + this.address);
    }

    public boolean refreshState() {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(2000);
            socket.connect(this.address, 2000);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_16BE);
            dataOutputStream.write(new byte[]{ -2, 1 });

            if (inputStream.read() == 255) {
                int length = inputStreamReader.read();
                if (length != 0 && length != -1) {
                    char[] chars = new char[length];
                    if (inputStreamReader.read(chars, 0, length) == length) {
                        String string = new String(chars);
                        String[] data;
                        if (string.startsWith("§")) {
                            data = string.split("\u0000");
                            this.motd = data[3];
                            this.playerCount = Integer.parseInt(data[4]);
                            this.playerMaxCount = Integer.parseInt(data[5]);
                        } else {
                            data = string.split("§");
                            this.motd = data[0];
                            this.playerCount = Integer.parseInt(data[1]);
                            this.playerMaxCount = Integer.parseInt(data[2]);
                        }

                        if (Main.getPlugin().getConfig().contains("waiting-motd") && !this.motd.contains(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("waiting-motd", "")))) this.state = ServerState.OCCUPIED;
                        else if (this.playerCount >= this.playerMaxCount) this.state = ServerState.FILLED;
                        else this.state = ServerState.FOUND;
                        dataOutputStream.close();
                        outputStream.close();
                        inputStreamReader.close();
                        inputStream.close();
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) { }

        this.state = ServerState.NOT_FOUND;
        return false;
    }

    public String getName() {
        return this.name;
    }

    public int getPlayerCount() {
        return this.playerCount;
    }

    public ServerState getState() {
        return this.state;
    }

    public ItemStack getGUIItem() {
        this.refreshState();
        ItemStack is;
        ItemMeta im;
        List<String> lore;
        switch (this.state) {
            case FOUND:
                is = Optional.ofNullable(XMaterial.LIME_WOOL.parseItem()).orElse(new ItemStack(Material.AIR));
                im = is.getItemMeta();
                im.setDisplayName(ChatColor.GREEN + this.name);
                lore = Optional.ofNullable(Main.getPlugin().getConfig().getStringList("message.found-server-lore")).orElse(new ArrayList<>());
                break;
            case NOT_FOUND:
                is = Optional.ofNullable(XMaterial.RED_WOOL.parseItem()).orElse(new ItemStack(Material.AIR));
                im = is.getItemMeta();
                im.setDisplayName(ChatColor.RED + this.name);
                lore = Optional.ofNullable(Main.getPlugin().getConfig().getStringList("message.not-found-server-lore")).orElse(new ArrayList<>());
                break;
            case OCCUPIED:
                is = Optional.ofNullable(XMaterial.ORANGE_WOOL.parseItem()).orElse(new ItemStack(Material.AIR));
                im = is.getItemMeta();
                im.setDisplayName(ChatColor.GOLD + this.name);
                lore = Optional.ofNullable(Main.getPlugin().getConfig().getStringList("message.occupied-server-lore")).orElse(new ArrayList<>());
                break;
            default:
                is = Optional.ofNullable(XMaterial.YELLOW_WOOL.parseItem()).orElse(new ItemStack(Material.AIR));
                im = is.getItemMeta();
                im.setDisplayName(ChatColor.YELLOW + this.name);
                lore = Optional.ofNullable(Main.getPlugin().getConfig().getStringList("message.filled-server-lore")).orElse(new ArrayList<>());
                break;
        }

        String[] split = this.motd.split("\n");
        lore.replaceAll(line -> line.replace("%players%", String.valueOf(this.playerCount)).replace("%maxPlayers%", String.valueOf(this.playerMaxCount)));
        if (split.length > 1) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("%motd%")) {
                    lore.add(i + 1, split[1]);
                    lore.set(i, line.substring(0, line.indexOf("%motd%")) + ChatColor.WHITE + split[0]);
                }
            }
        } else lore.replaceAll(line -> line.replace("%motd%", ChatColor.WHITE + this.motd));
        im.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));
        is.setItemMeta(im);
        is.setAmount(Math.min(64, Math.max(1, this.playerCount)));
        return is;
    }

    public boolean connect(Player player, boolean sendWarning) {
        this.refreshState();
        switch (this.state) {
            case FOUND:
                //noinspection UnstableApiUsage
                ByteArrayDataOutput data = ByteStreams.newDataOutput();
                data.writeUTF("Connect");
                data.writeUTF(this.server);
                player.sendPluginMessage(Main.getPlugin(), "BungeeCord", data.toByteArray());
                return true;
            case NOT_FOUND:
                if (sendWarning) player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("message.not-found-server-message", "")));
                return false;
            case OCCUPIED:
                if (sendWarning) player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("message.occupied-server-message", "")));
                return false;
            default:
                if (sendWarning) player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getPlugin().getConfig().getString("message.filled-server-message", "")));
                return false;
        }
    }

    public enum ServerState {
        FOUND, FILLED, OCCUPIED, NOT_FOUND
    }
}
