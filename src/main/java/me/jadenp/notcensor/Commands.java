package me.jadenp.notcensor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {
    private final NotCensor notCensor;
    public Commands(NotCensor notCensor) {
        this.notCensor = notCensor;
    }
    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if (command.getName().equalsIgnoreCase("notcensor")) {
            if (sender.hasPermission("notcensor.admin")) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    notCensor.reloadFiles();
                    sender.sendMessage(ChatColor.GREEN + "NotCensor " + notCensor.getDescription().getVersion() + " has been reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown command!");
                }
            } else {
                // no permission
                sender.sendMessage(ChatColor.RED + "Unknown or incomplete command, see below for error");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + Arrays.stream(args).map(str -> " " + str).collect(Collectors.joining("", label, "")) + ChatColor.RED + ChatColor.ITALIC + "<--[HERE]");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        List<String> tab = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("notcensor") && sender.hasPermission("notcensor.admin"))
            tab.add("reload");
        return tab;
    }
}
