package space.suiten.domain.model

enum class AIStatus(val value: String) {
    PENDING("pending"),
    RUNNING("running"),
    DONE("done"),
    ERROR("error");

    companion object {
        fun fromString(value: String): AIStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
