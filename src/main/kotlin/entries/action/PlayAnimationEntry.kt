package entries.action

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.animation.handler.AnimationHandler
import com.ticxo.modelengine.api.animation.handler.IStateMachineHandler
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

@Entry("play_modelengine_animation", "Play a ModelEngine animation.", Colors.RED, "material-symbols:touch-app-rounded")
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

        val animationName = animation.get(player)
        entity.models.forEach { model ->
            model.value.animationHandler.playAnimationWithPriority(animationName, animationSettings)
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
    val force: Boolean = false,
    @Help("State machine priority slot. Leave at -1 to auto-assign a stable slot per animation name (so distinct animations stack). Set a positive value to pin a specific slot.")
    @Default("-1")
    val priority: Int = -1,
)

// Offset from 0/1 so we never clobber ModelEngine's built-in state machine
// (slot 0, walk/idle/jump) or the default "plain" playAnimation slot (1).
private fun resolvePriority(explicit: Int, animation: String): Int {
    if (explicit >= 0) return explicit
    return 10 + ((animation.hashCode() and 0x7FFFFFFF) % 1000)
}

internal fun AnimationHandler.playAnimationWithPriority(
    animation: String,
    settings: AnimationSettings,
) {
    val priority = resolvePriority(settings.priority, animation)
    val lerpIn = settings.lerpIn.toMillis() / 1000.0
    val lerpOut = settings.lerpOut.toMillis() / 1000.0
    if (this is IStateMachineHandler) {
        playAnimation(priority, animation, lerpIn, lerpOut, settings.speed, settings.force)
    } else {
        playAnimation(animation, lerpIn, lerpOut, settings.speed, settings.force)
    }
}

internal fun AnimationHandler.stopAnimationWithPriority(
    animation: String,
    explicitPriority: Int,
    force: Boolean,
) {
    val priority = resolvePriority(explicitPriority, animation)
    if (this is IStateMachineHandler) {
        if (force) forceStopAnimation(priority, animation) else stopAnimation(priority, animation)
    } else {
        if (force) forceStopAnimation(animation) else stopAnimation(animation)
    }
}