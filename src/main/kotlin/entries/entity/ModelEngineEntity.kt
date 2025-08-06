package entries.entity

import com.destroystokyo.paper.profile.ProfileProperty
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.entity.Dummy
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.ModeledEntity
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes
import com.typewritermc.engine.paper.entry.entity.EntityState
import com.typewritermc.engine.paper.entry.entity.FakeEntity
import com.typewritermc.engine.paper.entry.entity.PositionProperty
import com.typewritermc.engine.paper.entry.entity.SkinProperty
import com.typewritermc.engine.paper.entry.entries.EntityProperty
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.entity.entries.data.minecraft.living.ScaleProperty
import com.typewritermc.entity.entries.data.minecraft.living.armorstand.InvisibleProperty
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*


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

                is ScaleProperty -> {
                    activeModel.setScale(property.scale)
                    activeModel.setHitboxScale(property.scale)
                }

                is SkinProperty -> {
                    val profile = Bukkit.createProfile(UUID.randomUUID()).apply {
                        setProperty(ProfileProperty("textures", property.texture, property.signature))
                    }

                    activeModel.bones.forEach { _, bone ->
                        bone.getBoneBehavior(BoneBehaviorTypes.PLAYER_LIMB).ifPresent { behavior ->
                            behavior.setTexture(profile)
                        }
                    }
                }

                is InvisibleProperty -> {
                    if (property.invisible) entity.setForceHidden(player, true)
                    else entity.setForceViewing(player, true)
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
        val scale = property(ScaleProperty::class)?.scale ?: 1.0

        return blueprint.mainHitbox.height * scale
    }

}