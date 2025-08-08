package entries.cinematic.segments

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.ContentEditor
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.Segment
import com.typewritermc.entity.entries.cinematic.EntityCinematicArtifact
import com.typewritermc.entity.entries.cinematic.EntityCinematicViewing

data class ModelEngineEntityRecordedSegment(
    override val startFrame: Int = 0,
    override val endFrame: Int = 0,
    @Help("The artifact for the recorded interactions data")
    @ContentEditor(EntityCinematicViewing::class)
    val artifact: Ref<EntityCinematicArtifact> = emptyRef(),
    //@Segments(Colors.PURPLE, "fa6-solid:bolt-lightning")
    val animations: List<ModelEngineAnimationSegment> = emptyList(),
) : Segment