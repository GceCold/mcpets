package fr.nocsy.mcpets.listeners;

import fr.nocsy.mcpets.MCPets;
import fr.nocsy.mcpets.PPermission;
import fr.nocsy.mcpets.data.Items;
import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.PetDespawnReason;
import fr.nocsy.mcpets.data.config.GlobalConfig;
import fr.nocsy.mcpets.data.config.Language;
import fr.nocsy.mcpets.data.inventories.PetInteractionMenu;
import fr.nocsy.mcpets.data.livingpets.PetFood;
import fr.nocsy.mcpets.events.EntityMountPetEvent;
import fr.nocsy.mcpets.events.PetSpawnEvent;
import fr.nocsy.mcpets.utils.debug.Debugger;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import ltd.icecold.orangeengine.api.OrangeEngineAPI;
import ltd.icecold.orangeengine.api.model.ModelEntity;
import ltd.icecold.orangeengine.api.model.ModelManager;
import ltd.icecold.orangeengine.core.OrangeEngine;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.HashMap;
import java.util.UUID;

public class PetListener implements Listener {

    private final HashMap<UUID, Pet> reconnectionPets = new HashMap<>();

    @EventHandler
    public void interact(PlayerInteractEntityEvent e) {
        if (!GlobalConfig.getInstance().isRightClickToOpen())
            return;

        Player p = e.getPlayer();

        if (GlobalConfig.getInstance().isSneakMode() && !p.isSneaking())
            return;

        // Do not open the menu if the player has a signal stick
        if (GlobalConfig.getInstance().isDisableInventoryWhileHoldingSignalStick()) {
            ItemStack it = p.getInventory().getItemInMainHand();
            if (Items.isSignalStick(it))
                return;
        }

        //If it's pet food in the main hand then do not open the menu
        if (PetFood.getFromItem(p.getInventory().getItemInMainHand()) != null) {
            return;
        }

        Entity ent = e.getRightClicked();

        Pet pet = Pet.getFromEntity(ent);

        if (pet != null && pet.getOwner() != null &&
                pet.getOwner().equals(p.getUniqueId())) {
            PetInteractionMenu menu = new PetInteractionMenu(pet, p.getUniqueId());
            pet.setLastInteractedWith(p);
            menu.open(p);
        }
        if (pet != null && p.isOp()) {
            PetInteractionMenu menu = new PetInteractionMenu(pet, p.getUniqueId());
            pet.setLastOpInteracted(p);
            menu.open(p);
        }
    }

    @EventHandler
    public void interact(EntityDamageByEntityEvent e) {
        if (!GlobalConfig.getInstance().isLeftClickToOpen())
            return;

        if (!(e.getDamager() instanceof Player))
            return;

        Player p = (Player) e.getDamager();

        if (GlobalConfig.getInstance().isSneakMode() && !p.isSneaking())
            return;

        Entity ent = e.getEntity();

        Pet pet = Pet.getFromEntity(ent);

        if (pet != null && pet.getOwner() != null &&
                pet.getOwner().equals(p.getUniqueId())) {
            PetInteractionMenu menu = new PetInteractionMenu(pet, p.getUniqueId());
            pet.setLastInteractedWith(p);
            menu.open(p);
            e.setCancelled(true);
            e.setDamage(0);
        }
        if (pet != null && p.isOp()) {
            PetInteractionMenu menu = new PetInteractionMenu(pet, p.getUniqueId());
            pet.setLastOpInteracted(p);
            menu.open(p);
            e.setCancelled(true);
            e.setDamage(0);
        }
    }

    @EventHandler
    public void disconnectPlayer(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (Pet.getActivePets().containsKey(p.getUniqueId())) {
            Pet pet = Pet.getActivePets().get(p.getUniqueId());
            pet.despawn(PetDespawnReason.DISCONNECTION);
            reconnectionPets.put(p.getUniqueId(), pet);
        }
    }

    @EventHandler
    public void reconnectionPlayer(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (reconnectionPets.containsKey(p.getUniqueId())) {
            Pet pet = reconnectionPets.get(p.getUniqueId());
            pet.spawn(p.getLocation(), true);
            reconnectionPets.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void teleport(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (Pet.getActivePets().containsKey(p.getUniqueId())) {
            Pet pet = Pet.getActivePets().get(p.getUniqueId());
            if (pet.getTamingProgress() < 1)
                return;
            pet.despawn(PetDespawnReason.TELEPORT);
            new BukkitRunnable() {
                @Override
                public void run() {
                    pet.spawn(p, p.getLocation());
                }
            }.runTaskLater(MCPets.getInstance(), 20L);
        }

    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (Pet.getActivePets().containsKey(p.getUniqueId())) {
            Pet pet = Pet.getActivePets().get(p.getUniqueId());
            pet.dismount(p);
        }

    }

    @EventHandler
    public void riding(EntityDamageEvent e) {
        if (!GlobalConfig.getInstance().isDismountOnDamaged())
            return;

        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (p.isInsideVehicle() && Pet.fromOwner(p.getUniqueId()) != null) {
                Pet pet = Pet.fromOwner(p.getUniqueId());
                pet.dismount(p);
            }
            return;
        }

    }

    /**
     * Wtf is this doing seriously ? Makes no sense.
     *
     * @param e
     */
    @EventHandler
    public void damaged(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Pet pet = Pet.getFromEntity(e.getEntity());
            if (pet != null && pet.isInvulnerable()) {
                e.setDamage(0);
                e.setCancelled(true);
            }
            return;
        }
    }

    @EventHandler
    public void gamemode(PlayerGameModeChangeEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (Pet.getActivePets().containsKey(uuid) && e.getNewGameMode() == GameMode.SPECTATOR) {
            Pet pet = Pet.getActivePets().get(uuid);
            pet.despawn(PetDespawnReason.GAMEMODE);
        }
    }

    /**
     * Handle random despawn
     *
     * @param e
     */
    @EventHandler
    public void despawn(MythicMobDespawnEvent e) {
        if (e.getEntity() != null) {
            Pet pet = Pet.getFromEntity(e.getEntity());
            if (pet != null) {
                if (!pet.isRemoved()) {
                    pet.despawn(PetDespawnReason.MYTHICMOBS);
                    Player owner = Bukkit.getPlayer(pet.getOwner());
                    if (owner != null) {
                        Language.REVOKED_UNKNOWN.sendMessage(owner);
                    }
                }
            }
        }
    }

    /**
     * Handle death of the pet
     *
     * @param e
     */
    @EventHandler
    public void death(MythicMobDeathEvent e) {
        if (e.getEntity() != null) {
            Pet pet = Pet.getFromEntity(e.getEntity());
            if (pet != null) {
                if (!pet.isRemoved()) {
                    pet.despawn(PetDespawnReason.DEATH);
                    Player owner = Bukkit.getPlayer(pet.getOwner());
                    if (owner != null) {
                        Language.REVOKED.sendMessage(owner);
                    }
                }
            }
        }
    }

    /**
     * Blacklisted world system
     *
     * @param e
     */
    @EventHandler
    public void blacklistedWorld(PetSpawnEvent e) {
        if (GlobalConfig.getInstance().hasBlackListedWorld(e.getWhere().getWorld().getName())) {
            e.setCancelled(true);
            Player p = Bukkit.getPlayer(e.getPet().getOwner());
            if (p != null) {
                Language.BLACKLISTED_WORLD.sendMessage(p);
            }
        }
    }

    @EventHandler
    public void despawnOnDismount(EntityDismountEvent e) {
        Entity entity = e.getEntity();
        UUID petUUID = e.getDismounted().getUniqueId();

        ModelManager modelManager = OrangeEngineAPI.getModelManager();
        if (modelManager == null)
            return;

        ModelEntity modelEntity = modelManager.getModelEntity(petUUID);
        if (modelEntity == null)
            return;

        // Running this as sync coz we fetch an entity
        new BukkitRunnable() {
            @Override
            public void run() {
                Pet pet = Pet.getFromEntity(Bukkit.getEntity(petUUID));
                if (pet != null && pet.isDespawnOnDismount()) {
                    pet.despawn(PetDespawnReason.DISMOUNT);
                }
            }
        }.runTask(MCPets.getInstance());

    }

    @EventHandler
    public void cancelDefaultTaming(EntityTameEvent e) {
        if (Pet.getFromEntity(e.getEntity()) != null) {
            // Cancel the event, so it doesn't give other type of item by default to the anchor
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void mountingPet(EntityMountPetEvent e) {
        if (e.getEntity() == null)
            return;

        // if it's not the owner or an admin mounting the pet, then we cancel it
        if (!e.getPet().getOwner().equals(e.getEntity().getUniqueId()) &&
                !e.getEntity().hasPermission(PPermission.ADMIN.getPermission())) {
            e.setCancelled(true);
            Debugger.send("§c" + e.getEntity().getName() + " can not mount " + e.getPet().getId() + " as he's not the owner, nor an admin.");
        }
        // If user doesn't have the perm to mount the pet, cancel the event
        if (e.getPet().getMountPermission() != null && !e.getEntity().hasPermission(e.getPet().getMountPermission())) {
            e.setCancelled(true);
            Language.CANT_MOUNT_PET_YET.sendMessage(e.getEntity());
        }
    }

    @EventHandler
    public void mountingPet(EntityMountEvent e) {
        Entity player = e.getEntity();
        UUID petUUID = e.getMount().getUniqueId();

        ModelManager modelManager = OrangeEngineAPI.getModelManager();
        if (modelManager == null)
            return;

        ModelEntity modelEntity = modelManager.getModelEntity(petUUID);
        if (modelEntity == null)
            return;

        if (!e.getMount().hasMetadata("orange_driver"))
            e.getMount().setMetadata("orange_driver", new FixedMetadataValue(OrangeEngine.getInstance(), "walk"));

        Pet pet = Pet.getFromEntity(e.getMount());

        if (pet == null)
            return;

        // if it's not the owner or an admin mounting the pet, then we cancel it
        if (!pet.getOwner().equals(player.getUniqueId()) &&
                !player.hasPermission(PPermission.ADMIN.getPermission())) {
            e.setCancelled(true);
            Debugger.send("§c" + player.getName() + " can not mount model of " + pet.getId() + " as he's not the owner, nor an admin.");
        }

        // If user doesn't have the perm to mount the pet, cancel the event
        if (pet.getMountPermission() != null && !player.hasPermission(pet.getMountPermission())) {
            e.setCancelled(true);
            Language.CANT_MOUNT_PET_YET.sendMessage(player);
        }
    }
}
