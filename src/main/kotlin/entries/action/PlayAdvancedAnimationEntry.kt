package entries.action

import com.ticxo.modelengine.api.ModelEngineAPI
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Default
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.AudienceManager
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entity.AudienceEntityDisplay
import com.typewritermc.engine.paper.entry.entity.IndividualAdvancedEntityInstance
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.EntityInstanceEntry
import com.typewritermc.engine.paper.entry.entries.Var
import entries.entity.instance.ModelEngineInstance
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

@Entry("play_advanced_modelengine_animation", "Play a ModelEngine animation on any entity.", Colors.RED, "material-symbols:touch-app-rounded")
class PlayAdvancedAnimationEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("The entity to play the animation on.")
    val entity: Ref<EntityInstanceEntry> = emptyRef(),
    @Help("The name of the animation")
    val animation: Var<String> = ConstVar("idle"),
    val animationSettings: AnimationSettings = AnimationSettings(),
) : ActionEntry, KoinComponent {

    private val audienceManager: AudienceManager by inject()

    override fun ActionTrigger.execute() {
        val display = audienceManager[entity] as? AudienceEntityDisplay ?: return
        val entityId = display.entityId(player.uniqueId)
        val entity = ModelEngineAPI.getModeledEntity(entityId) ?: return

        val animationName = animation.get(player)
        entity.models.forEach { model ->
            model.value.animationHandler.playAnimationWithPriority(animationName, animationSettings)
        }
    }
}