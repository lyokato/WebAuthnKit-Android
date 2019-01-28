package webauthnkit.core.client.operation

enum class OperationType {
    Create,
    Get
}

interface OperationListener {
    fun onFinish(opType: OperationType, opId: String)
}