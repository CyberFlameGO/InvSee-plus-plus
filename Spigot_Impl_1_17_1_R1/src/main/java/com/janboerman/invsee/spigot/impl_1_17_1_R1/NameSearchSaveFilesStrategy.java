package com.janboerman.invsee.spigot.impl_1_17_1_R1;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.janboerman.invsee.spigot.internal.CompletedEmpty;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.plugin.Plugin;

import com.janboerman.invsee.spigot.api.resolve.NameResolveStrategy;
import static com.janboerman.invsee.spigot.internal.NBTConstants.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.PlayerDataStorage;

public class NameSearchSaveFilesStrategy implements NameResolveStrategy {

    private final Plugin plugin;

    public NameSearchSaveFilesStrategy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Optional<String>> resolveUserName(UUID uniqueId) {
        CraftServer craftServer = (CraftServer) plugin.getServer();
        PlayerDataStorage worldNBTStorage = craftServer.getHandle().playerIo;

        File playerDirectory = worldNBTStorage.getPlayerDir();
        if (!playerDirectory.exists() || !playerDirectory.isDirectory())
            return CompletedEmpty.the();

        return CompletableFuture.supplyAsync(() -> {
            CompoundTag compound = worldNBTStorage.getPlayerData(uniqueId.toString());
            if (compound.contains("bukkit", TAG_COMPOUND)) {
                CompoundTag bukkit = compound.getCompound("bukkit");
                if (bukkit.contains("lastKnownName", TAG_STRING)) {
                    String lastKnownName = bukkit.getString("lastKnownName");
                    return Optional.of(lastKnownName);
                }
            }
            return Optional.empty();
        }, runnable -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable));
    }

}
