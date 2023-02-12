package fr.nocsy.mcpets.listeners;

import java.util.UUID;

import ltd.icecold.orangeengine.api.OrangeEngineAPI;
import ltd.icecold.orangeengine.api.model.ModelEntity;
import ltd.icecold.orangeengine.api.model.ModelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;

public class VanillaDismountListener implements Listener {
	
	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		var entity = e.getEntity();
		if (!(entity instanceof Player)) {
			return;
		}

		UUID petUUID = e.getDismounted().getUniqueId();

		ModelManager modelManager = OrangeEngineAPI.getModelManager();
		if (modelManager == null)
			return;

		ModelEntity model = modelManager.getModelEntity(petUUID);
		if (model == null) {
			return;
		}

		model.dismountEntity(entity.getUniqueId());
//		var mountManager = localModeledEntity.getMountManager();
//		var driver = mountManager.getDriver();
//		if (driver == null) {
//			mountManager.removePassenger(entity);
//			return;
//		}
//
//		if (driver.getUniqueId().equals(entity.getUniqueId())) {
//			mountManager.dismountAll();
//		} else {
//			mountManager.removePassenger(entity);
//		}
	}
}
