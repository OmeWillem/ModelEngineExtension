package entries.entity

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.utils.point.Vector
import com.typewritermc.engine.paper.entry.entity.DisplayNameProperty
import com.typewritermc.engine.paper.entry.entity.EntityState
import com.typewritermc.engine.paper.entry.entity.FakeEntity
import com.typewritermc.engine.paper.entry.entity.PositionProperty
import com.typewritermc.engine.paper.entry.entries.EntityDefinitionEntry
import com.typewritermc.engine.paper.entry.entries.EntityProperty
import com.typewritermc.engine.paper.entry.entries.LinesProperty
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.Color
import com.typewritermc.engine.paper.utils.isFloodgate
import com.typewritermc.engine.paper.utils.replaceTagPlaceholders
import com.typewritermc.entity.entries.data.minecraft.display.BillboardConstraintProperty
import com.typewritermc.entity.entries.data.minecraft.display.InterpolationDurationProperty
import com.typewritermc.entity.entries.data.minecraft.display.TranslationProperty
import com.typewritermc.entity.entries.data.minecraft.display.text.BackgroundColorProperty
import com.typewritermc.entity.entries.data.minecraft.living.ScaleProperty
import com.typewritermc.entity.entries.entity.custom.InteractionIndicatorEntity
import com.typewritermc.entity.entries.entity.custom.namePlate
import com.typewritermc.entity.entries.entity.custom.namePlateColor
import com.typewritermc.entity.entries.entity.custom.namePlateOffset
import com.typewritermc.entity.entries.entity.minecraft.TextDisplayEntity
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta
import org.bukkit.entity.Player
import java.time.Duration

class NamedModelEngineEntity(
    player: Player,
    var displayName: Var<String>,
    modelId: Var<String>,
    definition: Ref<out EntityDefinitionEntry>,
) : FakeEntity(player) {
    private val baseEntity = ModelEngineEntity(player, modelId)
    private val hologram = TextDisplayEntity(player)
    private val indicatorEntity = InteractionIndicatorEntity(player, definition)

    override val entityId: Int
        get() = baseEntity.entityId

    override val state: EntityState
        get() = baseEntity.state

    init {
        val hologramText = hologram()
        hologram.consumeProperties(
            LinesProperty(hologramText),
            TranslationProperty(Vector(y = namePlateOffset)),
            BillboardConstraintProperty(AbstractDisplayMeta.BillboardConstraints.CENTER),
            BackgroundColorProperty(Color.fromHex(namePlateColor)),
            InterpolationDurationProperty(Duration.ofMillis(100))
        )
        indicatorEntity.consumeProperties(
            TranslationProperty(calculateIndicatorOffset(hologramText)),
            BillboardConstraintProperty(AbstractDisplayMeta.BillboardConstraints.CENTER),
            BackgroundColorProperty(Color.fromHex(namePlateColor)),
            InterpolationDurationProperty(Duration.ofMillis(100))
        )
    }

    override fun applyProperties(properties: List<EntityProperty>) {
        baseEntity.consumeProperties(properties)
        properties.forEach { property ->
            when (property) {
                is DisplayNameProperty -> {
                    displayName = property.displayName
                }

                is PositionProperty -> {
                    val pos = property.add(0.0, baseEntity.height(), 0.0)
                    hologram.consumeProperties(pos)
                    indicatorEntity.consumeProperties(pos)
                }

                is ScaleProperty -> {
                    val pos = baseEntity.property(PositionProperty.type)?.withY { it + baseEntity.height() }
                    if (pos == null) return

                    hologram.consumeProperties(pos)
                    indicatorEntity.consumeProperties(pos)
                }
            }
        }
    }

    override fun tick() {
        baseEntity.tick()
        val hologramText = hologram()
        hologram.consumeProperties(LinesProperty(hologramText))
        hologram.tick()
        indicatorEntity.consumeProperties(TranslationProperty(calculateIndicatorOffset(hologramText)))
        indicatorEntity.tick()
    }

    private fun hologram(): String {
        val other = property(LinesProperty::class)?.lines ?: ""
        val displayName = this.displayName

        return namePlate.parsePlaceholders(player).replaceTagPlaceholders(
            "other" to other,
            "display_name" to displayName.get(player).parsePlaceholders(player),
        ).trim()
    }

    private fun calculateIndicatorOffset(hologramText: String): Vector {
        val lines = hologramText.count { it == '\n' } + 1
        val height = lines * 0.3 + namePlateOffset
        return Vector(y = height)
    }

    override fun spawn(location: PositionProperty) {
        baseEntity.spawn(location)

        val pos = location.add(0.0, baseEntity.height(), 0.0)
        hologram.spawn(pos)

        if (player.isFloodgate) {
            baseEntity.addPassenger(hologram)
        } else {
            indicatorEntity.spawn(pos)
            baseEntity.addPassenger(hologram)
            baseEntity.addPassenger(indicatorEntity)
        }
    }

    /*
      This is essentially useless; I could not find a proper way for ModelEngine to have packet entities as passenger.
      Thus, the unique fix above for the hologram height.
     */
    override fun addPassenger(entity: FakeEntity) {
        baseEntity.addPassenger(entity)
    }

    override fun removePassenger(entity: FakeEntity) {
        baseEntity.removePassenger(entity)
    }

    override fun contains(entityId: Int): Boolean {
        if (baseEntity.contains(entityId)) return true
        if (hologram.contains(entityId)) return true
        if (indicatorEntity.contains(entityId)) return true
        return false
    }

    override fun dispose() {
        baseEntity.dispose()
        hologram.dispose()
        indicatorEntity.dispose()
    }

    fun getEntity(): ModelEngineEntity {
        return baseEntity
    }

}