package webauthnkit.core.error

import java.lang.RuntimeException

class BadOperationException(msg: String? = null) : RuntimeException(msg)
class InvalidStateException(msg: String? = null) : RuntimeException(msg)
class ConstraintException(msg: String? = null) : RuntimeException(msg)
class CancelledException(msg: String? = null) : RuntimeException(msg)
class TimeoutException(msg: String? = null) : RuntimeException(msg)
class NotAllowedException(msg: String? = null) : RuntimeException(msg)
class UnsupportedException(msg: String? = null) : RuntimeException(msg)
class UnknownException(msg: String? = null) : RuntimeException(msg)

enum class ErrorReason(val rawValue: RuntimeException) {
    BadOperation(BadOperationException()),
    InvalidState(InvalidStateException()),
    Constraint(ConstraintException()),
    Cancelled(CancelledException()),
    Timeout(TimeoutException()),
    NotAllowed(NotAllowedException()),
    Unsupported(UnsupportedException()),
    Unknown(UnknownException())
}

