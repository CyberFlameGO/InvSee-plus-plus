package com.janboerman.invsee.spigot.impl_1_16_R3;

import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.SpectatorInventory;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import com.janboerman.invsee.utils.TriFunction;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.v1_16_R3.*;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class InvseeImpl extends InvseeAPI {

    static final ItemStack EMPTY_STACK = ItemStack.b;

    public InvseeImpl(Plugin plugin) {
        super(plugin);
        if (lookup.onlineMode(plugin.getServer())) {
            lookup.uuidResolveStrategies.add(new UUIDSearchSaveFilesStrategy(plugin));
        } else {
            // If we are in offline mode, then we should insert this strategy *before* the UUIDOfflineModeStrategy.
            lookup.uuidResolveStrategies.add(lookup.uuidResolveStrategies.size() - 1, new UUIDSearchSaveFilesStrategy(plugin));
        }
        lookup.nameResolveStrategies.add(2, new NameSearchSaveFilesStrategy(plugin));
    }

    @Override
    public void openMainSpectatorInventory(Player spectator, MainSpectatorInventory inv, String title, Mirror<PlayerInventorySlot> mirror) {
        CraftPlayer bukkitPlayer = (CraftPlayer) spectator;
        EntityPlayer nmsPlayer = bukkitPlayer.getHandle();
        MainBukkitInventory bukkitInventory = (MainBukkitInventory) inv;
        MainNmsInventory nmsInventory = bukkitInventory.getInventory();

        //this is what the nms does: nmsPlayer.openContainer(nmsWindow);
        //so let's emulate that!
        int windowId = nmsPlayer.nextContainerCounter();
        PlayerInventory bottom = nmsPlayer.inventory;
        MainNmsContainer nmsWindow = new MainNmsContainer(windowId, nmsInventory, bottom, nmsPlayer, mirror);
        nmsWindow.setTitle(CraftChatMessage.fromString(title != null ? title : inv.getTitle())[0]);
        boolean eventCancelled = CraftEventFactory.callInventoryOpenEvent(nmsPlayer, nmsWindow, false) == null; //closes current open inventory if one is already open
        if (!eventCancelled) {
            nmsPlayer.activeContainer = nmsWindow;
            nmsPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(windowId, nmsWindow.getType(), nmsWindow.getTitle()));
            nmsWindow.addSlotListener(nmsPlayer);
        }
    }

    @Override
    public void openEnderSpectatorInventory(Player spectator, EnderSpectatorInventory inv, String title, Mirror<EnderChestSlot> mirror) {
        CraftPlayer bukkitPlayer = (CraftPlayer) spectator;
        EntityPlayer nmsPlayer = bukkitPlayer.getHandle();
        EnderBukkitInventory bukkitInventory = (EnderBukkitInventory) inv;
        EnderNmsInventory nmsInventory = bukkitInventory.getInventory();

        //this is what the nms does: nmsPlayer.openContainer(nmsWindow);
        //so let's emulate that!
        int windowId = nmsPlayer.nextContainerCounter();
        PlayerInventory bottom = nmsPlayer.inventory;
        EnderNmsContainer nmsWindow = new EnderNmsContainer(windowId, nmsInventory, bottom, nmsPlayer, mirror);
        nmsWindow.setTitle(CraftChatMessage.fromString(title != null ? title : inv.getTitle())[0]);
        boolean eventCancelled = CraftEventFactory.callInventoryOpenEvent(nmsPlayer, nmsWindow, false) == null; //closes current open inventory if one is already open
        if (!eventCancelled) {
            nmsPlayer.activeContainer = nmsWindow;
            nmsPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(windowId, nmsWindow.getType(), nmsWindow.getTitle()));
            nmsWindow.addSlotListener(nmsPlayer);
        }
    }

    @Override
    public MainSpectatorInventory spectateInventory(HumanEntity player, String title, Mirror<PlayerInventorySlot> mirror) {
        MainNmsInventory spectatorInv = new MainNmsInventory(((CraftHumanEntity) player).getHandle(), title, mirror);
        MainBukkitInventory bukkitInventory = new MainBukkitInventory(spectatorInv);
        spectatorInv.bukkit = bukkitInventory;
        InventoryView targetView = player.getOpenInventory();
        bukkitInventory.watch(targetView);
        return bukkitInventory;
    }

    @Override
    public EnderSpectatorInventory spectateEnderChest(HumanEntity player, String title, Mirror<EnderChestSlot> mirror) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        CraftInventory craftInventory = (CraftInventory) player.getEnderChest();
        InventoryEnderChest nmsInventory = (InventoryEnderChest) craftInventory.getInventory();
        EnderNmsInventory spectatorInv = new EnderNmsInventory(uuid, name, nmsInventory.items, title, mirror);
        EnderBukkitInventory bukkitInventory = new EnderBukkitInventory(spectatorInv);
        spectatorInv.bukkit = bukkitInventory;
        return bukkitInventory;
    }

    @Override
    public CompletableFuture<Optional<MainSpectatorInventory>> createOfflineInventory(UUID player, String name, String title, Mirror<PlayerInventorySlot> mirror) {
        return createOffline(player, name, title, mirror, this::spectateInventory);
    }

    @Override
    public CompletableFuture<Optional<EnderSpectatorInventory>> createOfflineEnderChest(UUID player, String name, String title, Mirror<EnderChestSlot> mirror) {
        return createOffline(player, name, title, mirror, this::spectateEnderChest);
    }

    @Override
    public CompletableFuture<Void> saveInventory(MainSpectatorInventory newInventory) {
        return save(newInventory, this::spectateInventory, (currentInv, newInv) -> {
            currentInv.setStorageContents(newInv.getStorageContents());
            currentInv.setArmourContents(newInv.getArmourContents());
            currentInv.setOffHandContents(newInv.getOffHandContents());
            currentInv.setCursorContents(newInv.getCursorContents());
            currentInv.setPersonalContents(newInv.getPersonalContents());
        });
    }

    @Override
    public CompletableFuture<Void> saveEnderChest(EnderSpectatorInventory newInventory) {
        return save(newInventory, this::spectateEnderChest, (currentInv, newInv) -> {
            currentInv.setStorageContents(newInv.getStorageContents());
        });
    }

    private <Slot, IS extends SpectatorInventory<Slot>> CompletableFuture<Optional<IS>> createOffline(UUID player, String name, String title, Mirror<Slot> mirror, TriFunction<? super HumanEntity, String, ? super Mirror<Slot>, IS> invCreator) {
        CraftServer server = (CraftServer) plugin.getServer();
        DedicatedPlayerList playerList = server.getHandle();
        WorldNBTStorage worldNBTStorage = playerList.playerFileData;

        CraftWorld world = (CraftWorld) server.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        float yaw = 0F;
        GameProfile gameProfile = new GameProfile(player, name);

        FakeEntityHuman fakeEntityHuman = new FakeEntityHuman(
                world.getHandle(),
                new BlockPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()),
                yaw,
                gameProfile);

        return CompletableFuture.supplyAsync(() -> {
            NBTTagCompound playerCompound = worldNBTStorage.load(fakeEntityHuman);
            if (playerCompound != null) {
                fakeEntityHuman.loadData(playerCompound);   //only player-specific stuff
            } //else: player save file exists.

            CraftHumanEntity craftHumanEntity = new CraftHumanEntity(server, fakeEntityHuman);
            return Optional.of(invCreator.apply(craftHumanEntity, title, mirror));

        }, serverThreadExecutor);
    }

    private <Slot, SI extends SpectatorInventory<Slot>> CompletableFuture<Void> save(SI newInventory, TriFunction<? super HumanEntity, String, ? super Mirror<Slot>, SI> currentInvProvider, BiConsumer<SI, SI> transfer) {
        CraftServer server = (CraftServer) plugin.getServer();
        DedicatedPlayerList playerList = server.getHandle();
        WorldNBTStorage worldNBTStorage = playerList.playerFileData;

        CraftWorld world = (CraftWorld) server.getWorlds().get(0);
        GameProfile gameProfile = new GameProfile(newInventory.getSpectatedPlayerId(), newInventory.getSpectatedPlayerName());

        FakeEntityPlayer fakeEntityPlayer = new FakeEntityPlayer(
                server.getServer(),
                world.getHandle(),
                gameProfile,
                new PlayerInteractManager(world.getHandle()));

        return CompletableFuture.runAsync(() -> {
            NBTTagCompound playerCompound = worldNBTStorage.load(fakeEntityPlayer);
            if (playerCompound != null) {
                fakeEntityPlayer.load(playerCompound);   //all entity stuff + player stuff
            } //else: no player save file exists

            FakeCraftPlayer craftHumanEntity = fakeEntityPlayer.getBukkitEntity();
            SI currentInv = currentInvProvider.apply(craftHumanEntity, newInventory.getTitle(), newInventory.getMirror());

            transfer.accept(currentInv, newInventory);

            worldNBTStorage.save(fakeEntityPlayer);
        }, serverThreadExecutor);
    }

}
