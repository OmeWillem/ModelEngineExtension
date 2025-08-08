package entries.cinematic

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Segments
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.entries.CinematicAction
import com.typewritermc.engine.paper.entry.entries.CinematicEntry
import entries.cinematic.segments.ModelEngineEntityRecordedSegment
import entries.entity.definition.ModelEngineDefinition
import org.bukkit.entity.Player

@Entry(
    "modelengine_entity_cinematic",
    "Use an animated ModelEngine entity in a cinematic",
    Colors.PINK,
    "material-symbols:identity-platform"
)
class ModelEngineEntityCinematicEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    @Help("The entity that will be used in the cinematic")
    val definition: Ref<ModelEngineDefinition> = emptyRef(),
    @Segments(Colors.PINK, "fa6-solid:person-walking")
    val segments: List<ModelEngineEntityRecordedSegment> = emptyList(),
) : CinematicEntry {
    override fun create(player: Player): CinematicAction = ModelEngineEntityCinematicAction(player, this)
    override fun createRecording(player: Player): CinematicAction? = null
}
