package entries.cinematic.segments

import com.typewritermc.core.extension.annotations.Default
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import entries.action.AnimationSettings

data class ModelEngineAnimationSegment(
    @Help("The frame to play or stop the animation at.")
    val frame: Var<Int> = ConstVar(0),
    @Help("The animation to play or stop.")
    val animation: Var<String> = ConstVar(""),
    @Help("If true, this event will stop the animation instead of playing it.")
    @Default("false")
    val stop: Var<Boolean> = ConstVar(false),
    @Help("Settings for playing the animation.")
    val animationSettings: AnimationSettings = AnimationSettings(),
)