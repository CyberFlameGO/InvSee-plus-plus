package com.janboerman.invsee.spigot.api.resolve;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Deprecated
public class UUIDOfflinePlayerStrategy implements UUIDResolveStrategy {

    private final Server server;

    public UUIDOfflinePlayerStrategy(Server server) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public CompletableFuture<Optional<UUID>> resolveUniqueId(String userName) {
        return CompletableFuture.completedFuture(Optional.of(server.getOfflinePlayer(userName)).map(OfflinePlayer::getUniqueId));
    }

}
