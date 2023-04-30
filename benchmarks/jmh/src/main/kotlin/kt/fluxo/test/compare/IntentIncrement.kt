package kt.fluxo.test.compare

internal enum class IntentIncrement {
    Increment,
}

internal sealed interface IntentAdd {
    data class Add(val value: Int = 1) : IntentAdd
}
