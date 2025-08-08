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
import com.typewritermc.entity.entries.data.minecraft.GlowingEffectProperty
import com.typewritermc.entity.entries.data.minecraft.living.ScaleProperty
import com.typewritermc.entity.entries.data.minecraft.living.armorstand.InvisibleProperty
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

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
    lateinit var activeModel: ActiveModel

    override val entityId: Int
        get() = entity.entityId

    override val state: EntityState
        get() = EntityState(height(), 0.2085f)

    val modelId = modelId.get(player)
    private val location: Queue<Location> = ConcurrentLinkedQueue()

    private var subscribeId: UUID? = null
    private var tickInThread: Boolean = false

    override fun applyProperties(properties: List<EntityProperty>) {
        properties.forEach { property ->
            when (property) {
                is PositionProperty -> {
                    location.add(property.toBukkitLocation())
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

                is GlowingEffectProperty -> {
                    entity.isGlowing = property.glowing
                    entity.glowColor = property.color.color
                }

                else -> {
                }
            }
        }
    }

    override fun tick() {
        if (!tickInThread) return
        this.location.poll()?.let { entity.syncLocation(it) }
    }

    override fun spawn(location: PositionProperty) {
        if (modelId.isEmpty() || ModelEngineAPI.getBlueprint(modelId) == null) return
        entity.syncLocation(location.toBukkitLocation())
        this.location.add(location.toBukkitLocation())

        modeledEntity = ModelEngineAPI.createModeledEntity(entity).apply {
            isModelRotationLocked = false
        }
        activeModel = ModelEngineAPI.createActiveModel(modelId)
        modeledEntity.addModel(activeModel, true)
        entity.setForceViewing(player, true)

        // This only exists in the newer developer builds, so there's a little fallback
        val callbackField = entity.data::class.members
            .find { it.name == "syncUpdateCallback" }

        if (callbackField != null) {
            subscribeId = entity.data.syncUpdateCallback.subscribe {
                this.location.poll()?.let { entity.syncLocation(it) }
            }
        } else tickInThread = true

        super.spawn(location)
    }

    override fun dispose() {
        if (!::activeModel.isInitialized) return
        if (subscribeId != null) {
            entity.data.syncUpdateCallback.unsubscribe(subscribeId!!)
        }

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