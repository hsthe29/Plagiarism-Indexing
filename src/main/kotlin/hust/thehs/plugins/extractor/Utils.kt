package hust.thehs.plugins.extractor


data class CM(val value: Double) {
    val toDpiSize: DpiSize
        get() {
            val dpiValue = value/2.54*72.0
            return DpiSize(dpiValue)
        }
}
data class DpiSize(val value: Double) {
    val toCM: CM
        get() {
            val cmValue = value/72.0*2.54
            return CM(cmValue)
        }
}

val Double.cm
    get() = CM(this)

val Double.dpiSize
    get() = DpiSize(this)