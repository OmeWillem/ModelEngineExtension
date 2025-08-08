package entries.cinematic

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot.*
import com.google.gson.Gson
import com.typewritermc.core.utils.point.Coordinate
import com.typewritermc.core.utils.point.distanceSqrt
import com.typewritermc.engine.paper.content.modes.Streamer
import com.typewritermc.engine.paper.content.modes.Tape
import com.typewritermc.engine.paper.content.modes.parseTape
import com.typewritermc.engine.paper.entry.AssetManager
import com.typewritermc.engine.paper.entry.entity.*
import com.typewritermc.engine.paper.entry.entries.EntityProperty
import com.typewritermc.engine.paper.entry.entries.PropertyCollector
import com.typewritermc.engine.paper.entry.entries.PropertySupplier
import com.typewritermc.engine.paper.entry.temporal.SimpleCinematicAction
import com.typewritermc.engine.paper.extensions.packetevents.toPacketItem
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.engine.paper.utils.toWorld
import com.typewritermc.entity.entries.cinematic.EntityFrame
import com.typewritermc.entity.entries.data.minecraft.ArmSwingProperty
import com.typewritermc.entity.entries.data.minecraft.PoseProperty
import com.typewritermc.entity.entries.data.minecraft.living.DamagedProperty
import com.typewritermc.entity.entries.data.minecraft.living.EquipmentProperty
import com.typewritermc.entity.entries.data.minecraft.living.UseItemProperty
import com.typewritermc.entity.entries.data.minecraft.toProperty
import entries.cinematic.segments.ModelEngineEntityRecordedSegment
import entries.entity.ModelEngineEntity
import entries.entity.NamedModelEngineEntity
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent
import kotlin.math.abs
import kotlin.reflect.KClass

class ModelEngineEntityCinematicAction(
    private val player: Player,
    private val entry: ModelEngineEntityCinematicEntry,
) :
    SimpleCinematicAction<ModelEngineEntityRecordedSegment>() {
    private val assetManager: AssetManager by KoinJavaComponent.inject(AssetManager::class.java)
    private val gson: Gson by KoinJavaComponent.inject(Gson::class.java, named("bukkitDataParser"))

    override val segments: List<ModelEngineEntityRecordedSegment>
        get() = entry.segments

    private var entity: FakeEntity? = null
    private var collectors: List<PropertyCollector<EntityProperty>> = emptyList()
    private var recordings: Map<String, Tape<EntityFrame>> = emptyMap()

    private var streamer: Streamer<EntityFrame>? = null
    private var lastRelativeFrame = 0

    override suspend fun setup() {
        recordings = entry.segments
            .associate { it.artifact.id to it.artifact.get() }
            .filterValues { it != null }
            .mapValues { assetManager.fetchStringAsset(it.value!!) }
            .filterValues { it != null }
            .mapValues { parseTape(gson, EntityFrame::class, it.value!!) }

        super.setup()
    }

    override suspend fun startSegment(segment: ModelEngineEntityRecordedSegment) {
        super.startSegment(segment)

        val recording = (recordings[segment.artifact.id] ?: return).toMutableMap()
        if (recording.isEmpty()) return
        recording.keys.sorted().zipWithNext { a, b ->
            if (b - a == 2) {
                val prev = recording[a]?.location
                val next = recording[b]?.location
                if (prev != null && next != null) {
                    recording[a + 1] = recording[a]!!.copy(
                        location = Coordinate(
                            x = (prev.x + next.x) / 2,
                            y = (prev.y + next.y) / 2,
                            z = (prev.z + next.z) / 2,
                            yaw = (prev.yaw + next.yaw) / 2,
                            pitch = (prev.pitch + next.pitch) / 2
                        )
                    )
                }
            }
        }

        val definition = entry.definition.get() ?: return
        this.streamer = Streamer(recording)
        this.lastRelativeFrame = 0

        val prioritizedPropertySuppliers = definition.data.withPriority() +
                (FakeProvider(PositionProperty::class) { streamer?.currentFrame()?.location?.toProperty(player.world.toWorld()) } to Int.MAX_VALUE) +
                (FakeProvider(PoseProperty::class) { streamer?.currentFrame()?.pose?.toProperty() } to Int.MAX_VALUE) +
                (FakeProvider(ArmSwingProperty::class) { streamer?.currentFrame()?.swing?.toProperty() } to Int.MAX_VALUE) +
                (FakeProvider(DamagedProperty::class) { DamagedProperty(streamer?.currentFrame()?.damaged == true) } to Int.MAX_VALUE) +
                (FakeProvider(UseItemProperty::class) { UseItemProperty(streamer?.currentFrame()?.useItem == true) } to Int.MAX_VALUE) +
                (FakeProvider(EquipmentProperty::class) {
                    val equipment =
                        mutableMapOf<com.github.retrooper.packetevents.protocol.player.EquipmentSlot, com.github.retrooper.packetevents.protocol.item.ItemStack>()
                    streamer?.currentFrame()?.mainHand?.let { equipment[MAIN_HAND] = it.toPacketItem() }
                    streamer?.currentFrame()?.offHand?.let { equipment[OFF_HAND] = it.toPacketItem() }
                    streamer?.currentFrame()?.helmet?.let { equipment[HELMET] = it.toPacketItem() }
                    streamer?.currentFrame()?.chestplate?.let { equipment[CHEST_PLATE] = it.toPacketItem() }
                    streamer?.currentFrame()?.leggings?.let { equipment[LEGGINGS] = it.toPacketItem() }
                    streamer?.currentFrame()?.boots?.let { equipment[BOOTS] = it.toPacketItem() }

                    EquipmentProperty(equipment)
                } to Int.MAX_VALUE)

        this.collectors = prioritizedPropertySuppliers.toCollectors()
        spawn()
    }

    private fun spawn() {
        val definition = entry.definition.get() ?: return
        this.entity = definition.create(player)
        val startLocation = streamer?.currentFrame()?.location ?: return
        val collectedProperties = collectors.mapNotNull { it.collect(player) }

        this.entity?.spawn(startLocation.toProperty(player.world.toWorld()))
        this.entity?.consumeProperties(collectedProperties)
    }

    private fun despawn() {
        lastSoundLocation = null
        this.entity?.dispose()
        this.entity = null
    }

    override suspend fun tickSegment(segment: ModelEngineEntityRecordedSegment, frame: Int) {
        super.tickSegment(segment, frame)
        val relativeFrame = frame - segment.startFrame
        streamer?.frame(relativeFrame)

        if (abs(relativeFrame - lastRelativeFrame) > 5) {
            despawn()
            spawn()
            lastRelativeFrame = relativeFrame
            return
        }

        val collectedProperties = collectors.mapNotNull { it.collect(player) }
        this.entity?.consumeProperties(collectedProperties)
        this.entity?.tick()
        lastRelativeFrame = relativeFrame

        segment.animations.forEach {
            if (frame != it.frame.get(player)) return@forEach
            val animation = it.animation.get(player)
            val animationSettings = it.animationSettings
            val stop = it.stop.get(player)
            val animationHandler = entity()?.activeModel?.animationHandler ?: return

            if (stop) {
                if (animation.isEmpty()) {
                    animationHandler.forceStopAllAnimations()
                    return
                }

                if (animationSettings.force) {
                    animationHandler.forceStopAnimation(animation)
                    return
                }

                animationHandler.stopAnimation(animation)
            } else {
                animationHandler.playAnimation(
                    animation,
                    animationSettings.lerpIn.toMillis() / 1000.0,
                    animationSettings.lerpOut.toMillis() / 1000.0,
                    animationSettings.speed, animationSettings.force
                )
            }
        }

        trackStepSound()
    }

    private var lastSoundLocation: PositionProperty? = null
    private fun trackStepSound() {
        val location = entity?.property<PositionProperty>() ?: return
        val lastLocation = lastSoundLocation
        if (lastLocation == null) {
            lastSoundLocation = location
            return
        }

        val distance = location.distanceSqrt(lastLocation) ?: 0.0
        if (distance < 1.7) return
        playStepSound()
        lastSoundLocation = location
    }

    private fun playStepSound() {
        val location = entity?.property<PositionProperty>() ?: return
        val sound = location.toBukkitLocation().block.blockData.soundGroup.stepSound
        player.playSound(location.toBukkitLocation(), sound, SoundCategory.NEUTRAL, 0.4f, 1.0f)
    }

    private fun entity(): ModelEngineEntity? {
        if (entity is NamedModelEngineEntity)
            return (entity as NamedModelEngineEntity).getEntity()
        if (entity is ModelEngineEntity)
            return entity as ModelEngineEntity
        return null
    }

    override suspend fun stopSegment(segment: ModelEngineEntityRecordedSegment) {
        super.stopSegment(segment)
        despawn()
    }
}


class FakeProvider<P : EntityProperty>(private val klass: KClass<P>, private val supplier: () -> P?) :
    PropertySupplier<P> {
    override fun type(): KClass<P> = klass

    override fun build(player: Player): P {
        return supplier() ?: throw IllegalStateException("Could not build property $klass")
    }

    override fun canApply(player: Player): Boolean {
        return supplier() != null
    }
}