package com.janboerman.invsee.spigot.addon.clear;

import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.response.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

class InvClearExecutor implements CommandExecutor {

    private final ClearPlugin plugin;
    private final InvseeAPI api;

    InvClearExecutor(ClearPlugin plugin, InvseeAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String inputPlayer = args[0];

        var eitherPlayer = Convert.convertPlayer(inputPlayer);
        CompletableFuture<Optional<UUID>> uuidFuture;
        CompletableFuture<Optional<String>> userNameFuture;
        if (eitherPlayer.isLeft()) {
            UUID uuid = eitherPlayer.getLeft();
            uuidFuture = CompletableFuture.completedFuture(Optional.of(uuid));
            userNameFuture = api.fetchUserName(uuid);
        } else {
            assert eitherPlayer.isRight();
            String userName = eitherPlayer.getRight();
            userNameFuture = CompletableFuture.completedFuture(Optional.of(userName));
            uuidFuture = api.fetchUniqueId(userName);
        }

        Material itemType = null;
        int maxCount = -1;

        if (args.length >= 2) {
            String inputItemType = args[1];
            var eitherMaterial = Convert.convertItemType(inputItemType);
            if (eitherMaterial.isRight()) {
                itemType = eitherMaterial.getRight();
            } else {
                assert eitherMaterial.isLeft();
                sender.sendMessage(ChatColor.RED + eitherMaterial.getLeft());
                return true;
            }
        }

        if (args.length >= 3) {
            String inputMaxCount = args[2];
            var eitherMaxCount = Convert.convertAmount(inputMaxCount);
            if (eitherMaxCount.isRight()) {
                maxCount = eitherMaxCount.getRight();
            } else {
                assert eitherMaxCount.isLeft();
                sender.sendMessage(ChatColor.RED + eitherMaxCount.getLeft());
                return true;
            }
        }

        final Material finalItemType = itemType;
        final int finalMaxCount = maxCount;

        uuidFuture.<Optional<String>, Void>thenCombineAsync(userNameFuture, (optUuid, optName) -> {
            if (optName.isEmpty() || optUuid.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Unknown player: " + inputPlayer);
            } else {
                String userName = optName.get();
                UUID uuid = optUuid.get();

                var responseFuture = api.mainSpectatorInventory(uuid, userName, userName + "s inventory");
                responseFuture.thenAcceptAsync(response -> {
                    if (response.isSuccess()) {
                        MainSpectatorInventory inventory = response.getInventory();
                        if (finalItemType == null) {
                            inventory.clear();
                            sender.sendMessage(ChatColor.GREEN + "Cleared " + userName + "'s inventory.");
                        } else {
                            if (finalMaxCount == -1) {
                                inventory.remove(finalItemType);
                                sender.sendMessage(ChatColor.GREEN + "Removed all " + finalItemType + " from " + userName + "'s inventory.");
                            } else {
                                int removed = RemoveUtil.removeAtMost(inventory, finalItemType, finalMaxCount);
                                sender.sendMessage( ChatColor.GREEN + "Removed " + removed + " " + finalItemType + " from " + userName + "'s inventory.");
                            }
                        }
                        api.saveInventory(inventory).whenComplete((v, e) -> {
                            if (e != null) plugin.getLogger().log(Level.SEVERE, "Could not save inventory", e);
                        });
                    } else {
                        NotCreatedReason reason = response.getReason();
                        if (reason instanceof TargetDoesNotExist) {
                            var targetDoesNotExist = (TargetDoesNotExist) reason;
                            sender.sendMessage(ChatColor.RED + "Player " + targetDoesNotExist.getTarget() + " does not exist.");
                        } else if (reason instanceof TargetHasExemptPermission) {
                            var targetHasExemptPermission = (TargetHasExemptPermission) reason;
                            sender.sendMessage(ChatColor.RED + "Player " + targetHasExemptPermission.getTarget() + " is exempted from being spectated.");
                        } else if (reason instanceof ImplementationFault) {
                            var implementationFault = (ImplementationFault) reason;
                            sender.sendMessage(ChatColor.RED + "An internal fault occurred when trying to load " + implementationFault.getTarget() + "'s inventory.");
                        } else if (reason instanceof OfflineSupportDisabled) {
                            sender.sendMessage(ChatColor.RED + "Spectating offline players' inventories is disabled.");
                        }
                    }
                }, api.serverThreadExecutor);
            }

            return null;
        }, api.serverThreadExecutor);
        return true;
    }

}
