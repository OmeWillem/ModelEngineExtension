package listeners

import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.ticxo.modelengine.api.events.BaseEntityInteractEvent
import com.typewritermc.engine.paper.entry.AudienceManager
import com.typewritermc.engine.paper.entry.entity.AudienceEntityDisplay
import com.typewritermc.engine.paper.events.AsyncEntityDefinitionInteract
import com.typewritermc.engine.paper.events.AsyncFakeEntityInteract
import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BaseEntityInteractListener : Listener, KoinComponent {

    private val audienceManager: AudienceManager by inject()

    @EventHandler
    fun baseEntityInteract(event: BaseEntityInteractEvent) {
        val player = event.player
        val entity = event.baseEntity

        val hand = when (event.slot) {
            EquipmentSlot.HAND -> InteractionHand.MAIN_HAND
            EquipmentSlot.OFF_HAND -> InteractionHand.OFF_HAND
            else -> InteractionHand.MAIN_HAND
        }

        val action = when (event.action) {
            BaseEntityInteractEvent.Action.INTERACT -> WrapperPlayClientInteractEntity.InteractAction.INTERACT
            BaseEntityInteractEvent.Action.INTERACT_ON -> WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT
            BaseEntityInteractEvent.Action.ATTACK -> WrapperPlayClientInteractEntity.InteractAction.ATTACK
            else ->  WrapperPlayClientInteractEntity.InteractAction.INTERACT
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            AsyncFakeEntityInteract(player, entity.entityId, hand, action).callEvent()
        })

        val display = audienceManager.findDisplays(AudienceEntityDisplay::class)
            .firstOrNull { it.playerSeesEntity(player.uniqueId, entity.entityId) } ?: return

        val definition = display.definition ?: return
        val instance = display.instanceEntryRef.get() ?: return

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            AsyncEntityDefinitionInteract(
                player,
                entity.entityId,
                definition,
                instance,
                hand,
                action,
                event.player.isSneaking
            ).callEvent()
        })
    }

}