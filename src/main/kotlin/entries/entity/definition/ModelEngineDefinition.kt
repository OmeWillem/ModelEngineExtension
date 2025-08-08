package entries.entity.definition

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.Default
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Icon
import com.typewritermc.core.extension.annotations.OnlyTags
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.entity.FakeEntity
import com.typewritermc.engine.paper.entry.entity.SimpleEntityDefinition
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.EntityData
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.Sound
import entries.entity.ModelEngineEntity
import entries.entity.NamedModelEngineEntity
import org.bukkit.entity.Player
import java.time.Duration

@Entry(
    "modelengine_definition",
    "A simplified premade ModelEngine entity",
    Colors.ORANGE,
    "material-symbols:account-box"
)
@Tags("modelengine_definition")
class ModelEngineDefinition(
    override val id: String = "",
    override val name: String = "",
    override val displayName: Var<String> = ConstVar(""),
    override val sound: Sound = Sound.EMPTY,
    @OnlyTags("generic_entity_data", "living_entity_data", "lines", "player_data")
    override val data: List<Ref<EntityData<*>>> = emptyList(),
    @Help("Set the model of the entity")
    val modelId: Var<String> = ConstVar(""),
    @Help("Whether the entity is named or not, showing a nametag.")
    val named: Var<Boolean> = ConstVar(false),
    @Help("Configure if you want default animations to be used.")
    val defaultAnimationSettings: DefaultAnimationSettings = DefaultAnimationSettings(),

) : SimpleEntityDefinition {

    override fun create(player: Player): FakeEntity {
        if (named.get(player)) return NamedModelEngineEntity(player, displayName, modelId, defaultAnimationSettings, ref())
        return ModelEngineEntity(player, modelId, defaultAnimationSettings)
    }
}

data class DefaultAnimationSettings(
    @Help("Run the default walk animation if the entity is moving") @Default("true")
    val walkAnimation: Boolean = true,
)