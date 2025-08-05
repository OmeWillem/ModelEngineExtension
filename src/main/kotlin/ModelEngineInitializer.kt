import com.ticxo.modelengine.api.events.BaseEntityInteractEvent
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.TypewriterPaperPlugin
import com.typewritermc.engine.paper.plugin
import listeners.BaseEntityInteractListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Singleton
object ModelEngineInitializer : Initializable {

    private lateinit var baseEntityInteractEvent : BaseEntityInteractListener

    override suspend fun initialize() {
        baseEntityInteractEvent = BaseEntityInteractListener()
        Bukkit.getPluginManager().registerEvents(baseEntityInteractEvent, plugin)
    }

    override suspend fun shutdown() {
        BaseEntityInteractEvent.getHandlerList().unregister(baseEntityInteractEvent)
    }

}