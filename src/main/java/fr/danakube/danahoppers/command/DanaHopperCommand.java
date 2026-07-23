package fr.danakube.danahoppers.command;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.util.ColorUtil;
import fr.danakube.danahoppers.util.HopperItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Commande principale /danahopper (ou /danahoppers) avec sous-commandes give, reload, info et auto-complétion.
 */
public class DanaHopperCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final HopperManager hopperManager;

    public DanaHopperCommand(Plugin plugin, ConfigManager configManager, HopperManager hopperManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            default -> sendHelpMessage(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("danahoppers.admin.give") && !sender.hasPermission("danahoppers.admin")) {
            configManager.sendRawMessage(sender, "no_permission");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Utilisation : /" + args[0] + " give <joueur> <type> [tier]</red>"));
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ColorUtil.parse("<red>Joueur introuvable : " + targetName + "</red>"));
            return;
        }

        String typeId = args[2].toLowerCase();
        HopperTypeConfig typeConfig = configManager.getHopperType(typeId);
        if (typeConfig == null) {
            configManager.sendMessage(sender, "unknown_hopper_type", Map.of("type", typeId));
            return;
        }

        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtil.parse("<red>Le tier doit être un nombre entier valide.</red>"));
                return;
            }
        }

        ItemStack hopperItem = HopperItemUtil.createHopperItem(typeId, tier, configManager, plugin);
        Map<Integer, ItemStack> remaining = target.getInventory().addItem(hopperItem);
        if (!remaining.isEmpty()) {
            for (ItemStack drop : remaining.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
        }

        configManager.sendMessage(target, "hopper_given", Map.of("type", typeConfig.name(), "tier", String.valueOf(tier)));
        if (!sender.equals(target)) {
            configManager.sendMessage(sender, "hopper_given_target", Map.of(
                    "type", typeConfig.name(),
                    "tier", String.valueOf(tier),
                    "player", target.getName()
            ));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("danahoppers.admin.reload") && !sender.hasPermission("danahoppers.admin")) {
            configManager.sendRawMessage(sender, "no_permission");
            return;
        }

        configManager.loadAll();
        configManager.sendRawMessage(sender, "reload_success");
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            configManager.sendRawMessage(sender, "player_only");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !hopperManager.isCustomHopper(targetBlock.getLocation())) {
            player.sendMessage(ColorUtil.parse("<red>Vous ne regardez aucun CustomHopper valide (portée max: 5 blocs).</red>"));
            return;
        }

        CustomHopper hopper = hopperManager.getHopper(targetBlock.getLocation());
        if (hopper == null) {
            player.sendMessage(ColorUtil.parse("<red>Impossible d'obtenir les informations de cet entonnoir.</red>"));
            return;
        }

        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        String typeName = typeConfig != null ? typeConfig.name() : hopper.getTypeId();

        player.sendMessage(ColorUtil.parse("<gradient:#4facfe:#00f2fe><bold>=== Informations CustomHopper ===</bold></gradient>"));
        player.sendMessage(ColorUtil.parse("<gray>UUID: <white>" + hopper.getHopperUuid() + "</white></gray>"));
        player.sendMessage(ColorUtil.parse("<gray>Type: <yellow>" + typeName + "</yellow> (ID: " + hopper.getTypeId() + ")</gray>"));
        player.sendMessage(ColorUtil.parse("<gray>Tier: <green>" + hopper.getTier() + "</green></gray>"));
        player.sendMessage(ColorUtil.parse("<gray>Items aspirés: <aqua>" + hopper.getItemsTransferred() + "</aqua></gray>"));
        player.sendMessage(ColorUtil.parse("<gray>Propriétaire: <gold>" + hopper.getOwnerUuid() + "</gold></gray>"));
        if (hopper.getLinkedLocation() != null) {
            Location l = hopper.getLinkedLocation();
            player.sendMessage(ColorUtil.parse("<gray>Lien: <green>" + l.getWorld().getName() + " (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")</green></gray>"));
        } else {
            player.sendMessage(ColorUtil.parse("<gray>Lien: <red>Aucun conteneur lié</red></gray>"));
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<gradient:#4facfe:#00f2fe><bold>=== DanaHoppers Commandes ===</bold></gradient>"));
        sender.sendMessage(ColorUtil.parse("<yellow>/danahopper give <joueur> <type> [tier]</yellow> <gray>- Donner un entonnoir custom</gray>"));
        sender.sendMessage(ColorUtil.parse("<yellow>/danahopper reload</yellow> <gray>- Recharger les configurations</gray>"));
        sender.sendMessage(ColorUtil.parse("<yellow>/danahopper info</yellow> <gray>- Voir les infos de l'entonnoir visé</gray>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("give", "reload", "info");
            return filterPrefix(subs, args[0]);
        }

        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return filterPrefix(players, args[1]);
        }

        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            List<String> types = new ArrayList<>(configManager.getHopperTypes().keySet());
            return filterPrefix(types, args[2]);
        }

        if (args.length == 4 && "give".equalsIgnoreCase(args[0])) {
            return filterPrefix(List.of("1", "2", "3"), args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
