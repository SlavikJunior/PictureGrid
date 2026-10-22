package architecture

/**
 * Отчёт о покрытии архитектурных проверок.
 *
 * Каждая проверка сообщает, сколько файлов она реально увидела. Проверка с
 * нулевой областью не падает — в новом проекте слоя может не быть, — но и не
 * молчит: строка `COVERAGE ... status=SKIPPED` попадает в вывод теста, оттуда
 * в XML, оттуда в отчёт `arch_check`.
 *
 * Зачем: зелёный прогон, в котором половина тестов ничего не проверила,
 * неотличим от настоящего зелёного. Один такой случай уже стоил нам «33 теста,
 * 0 провалов» на проекте, где ни один экран не был проверен.
 */
object Coverage {

    /**
     * @param test имя проверки — совпадает с именем класса теста.
     * @param subject что именно считали: «файлы *Content», «файлы domain».
     * @param count размер области.
     */
    fun report(test: String, subject: String, count: Int) {
        val status = if (count == 0) "SKIPPED" else "APPLIED"
        println("COVERAGE test=$test scope=$count status=$status what=$subject")
    }
}
