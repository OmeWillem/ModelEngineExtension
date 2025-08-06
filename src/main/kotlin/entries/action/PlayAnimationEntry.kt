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
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import entries.entity.instance.ModelEngineInstance
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

@Entry("modelengine_play_animation", "Play a ModelEngine animation.", Colors.RED, "material-symbols:touch-app-rounded")
class PlayAnimationEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("The ModelEngine entity to play the animation on.")
    val entity: Ref<ModelEngineInstance> = emptyRef(),
    @Help("The name of the animation")
    val animation: Var<String> = ConstVar("idle"),
    val animationSettings: AnimationSettings = AnimationSettings(),
) : ActionEntry, KoinComponent {

    private val audienceManager: AudienceManager by inject()

    override fun ActionTrigger.execute() {
        val display = audienceManager[entity] as? AudienceEntityDisplay ?: return
        val entityId = display.entityId(player.uniqueId)
        val entity = ModelEngineAPI.getModeledEntity(entityId) ?: return

        entity.models.forEach { model ->
            model.value.animationHandler.playAnimation(
                animation.get(player),
                animationSettings.lerpIn.toMillis() / 1000.0,
                animationSettings.lerpOut.toMillis() / 1000.0,
                animationSettings.speed, animationSettings.force
            )
        }
    }
}

data class AnimationSettings(
    @Help("The duration of the lerp in effect.") @Default("250")
    val lerpIn: Duration = Duration.ofMillis(250),
    @Help("The duration of the lerp out effect.") @Default("250")
    val lerpOut: Duration = Duration.ofMillis(250),
    @Help("The speed of the animation.") @Default("1")
    val speed: Double = 1.0,
    @Help("Force the animation.")
    val force: Boolean = false
)