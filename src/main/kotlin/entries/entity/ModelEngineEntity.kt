package entries.entity

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.entity.Dummy
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.ModeledEntity
import com.typewritermc.engine.paper.entry.entity.EntityState
import com.typewritermc.engine.paper.entry.entity.FakeEntity
import com.typewritermc.engine.paper.entry.entity.PositionProperty
import com.typewritermc.engine.paper.entry.entries.EntityProperty
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.toBukkitLocation
import org.bukkit.entity.Player


class ModelEngineEntity(
    player: Player,
    modelId: Var<String>
) : FakeEntity(player) {

    private var entity: Dummy<*> = Dummy<Any?>().apply {
        isDetectingPlayers = false
        bodyRotationController.rotationDuration = 0
        bodyRotationController.rotationDelay = 0
    }
    private lateinit var modeledEntity: ModeledEntity
    private lateinit var activeModel: ActiveModel

    override val entityId: Int
        get() = entity.entityId

    override val state: EntityState
        get() = EntityState(height(), 0.2085f)

    val modelId = modelId.get(player)

    override fun applyProperties(properties: List<EntityProperty>) {
        properties.forEach { property ->
            when (property) {
                is PositionProperty -> {
                    entity.syncLocation(property.toBukkitLocation())
                    entity.yHeadRot = property.yaw
                    entity.xHeadRot = property.pitch
                }

                else -> {
                }
            }
        }
    }

    override fun tick() {}

    override fun spawn(location: PositionProperty) {
        if (modelId.isEmpty() || ModelEngineAPI.getBlueprint(modelId) == null) return
        entity.syncLocation(location.toBukkitLocation())

        modeledEntity = ModelEngineAPI.createModeledEntity(entity).apply {
            isModelRotationLocked = false
        }
        activeModel = ModelEngineAPI.createActiveModel(modelId)
        modeledEntity.addModel(activeModel, true)
        entity.setForceViewing(player, true)

        super.spawn(location)
    }

    override fun dispose() {
        if (!::activeModel.isInitialized) return

        activeModel.isRemoved = true
        entity.isRemoved = true
    }

    override fun addPassenger(entity: FakeEntity) {
        if (entity.entityId == entityId) return

    }

    override fun removePassenger(entity: FakeEntity) {
        if (entity.entityId == entityId) return
    }

    override fun contains(entityId: Int): Boolean {
        return this.entityId == entityId
    }

    fun height(): Double {
        val blueprint = ModelEngineAPI.getBlueprint(modelId) ?: return 0.0
        return blueprint.mainHitbox.height
    }

}