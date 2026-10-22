package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.*
import com.lemonappdev.konsist.api.verify.assertNotEmpty
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * INV-08 — один StateFlow<UiState> на экран, мутабельный источник не наружу.
 * INV-09 — только атомарная мутация состояния.
 * INV-15 — на Android только collectAsStateWithLifecycle().
 *
 * Область проверки — держатели состояния экрана из [STATE_HOLDER_SUFFIXES].
 * В проекте это MVIKotlin `Store`, поэтому проверка не ограничивается
 * `*ViewModel`, которых в коде нет: иначе она смотрит в пустоту.
 */
class StateContractTest {

    private val productionScope = Konsist.scopeFromProduction()

    /**
     * Классы и интерфейсы, которые держат состояние экрана.
     *
     * Интерфейсы тоже: контракт состояния в проекте объявлен интерфейсом
     * (`GalleryStore`, `ViewerStore`), а не классом.
     */
    private val stateHolders = productionScope.classesAndInterfaces()
        .withNameEndingWith(STATE_HOLDER_SUFFIXES)

    @Test
    fun `view model does not expose mutable state`() {
        Coverage.report("StateContractTest", "держатели состояния", stateHolders.size)

        // Пустая область — это не «проверок нет ошибок», а «проверок не было».
        stateHolders.assertNotEmpty()

        stateHolders.assertTrue { viewModel ->
            viewModel.properties().none { property ->
                property.hasPublicOrDefaultModifier &&
                    property.text.contains("MutableStateFlow")
            }
        }
    }

    @Test
    fun `mutable state is not assigned directly`() {
        stateHolders.assertTrue { viewModel ->
            val mutableStateNames = viewModel.properties()
                .filter { it.text.contains("MutableStateFlow") }
                .map { it.name }
            mutableStateNames.none { name -> viewModel.text.contains("$name.value =") }
        }
    }

    @Test
    fun `state is not mutated directly`() {
        productionScope.files.assertTrue { file ->
            !file.text.contains(Regex("""_state\.value\s*="""))
        }
    }

    @Test
    fun `state is not mutated through collection mutation`() {
        productionScope.files.assertTrue { file ->
            !file.text.contains(Regex("""_state\.value\.[A-Za-z]+\.(add|remove|clear)\("""))
        }
    }

    @Test
    fun `compose state is not used inside view models`() {
        stateHolders.assertTrue { viewModel ->
            !viewModel.text.contains("mutableStateOf") &&
                !viewModel.text.contains("mutableStateListOf")
        }
    }

    @Test
    fun `android sources use lifecycle aware state collection`() {
        productionScope.files
            .filterNot { it.sourceSetName == "commonMain" }
            .assertTrue { file ->
                !file.text.contains("collectAsState(")
            }
    }

    @Test
    fun `content does not receive view model`() {
        // INV-12: Content — stateless-слой. ViewModel в его параметрах означает,
        // что превью и переиспользование снова требуют DI.
        productionScope.functions()
            .withNameEndingWith("Content")
            .assertTrue { function ->
                function.parameters.none { parameter ->
                    parameter.type.name.contains("ViewModel")
                }
            }
    }
}
