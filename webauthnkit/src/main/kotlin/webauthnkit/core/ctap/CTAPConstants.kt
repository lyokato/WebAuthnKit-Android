package webauthnkit.core.ctap

enum class CTAPCommandType(val rawValue: Byte) {

   MakeCredential(0x01.toByte()),
   GetAssertion(0x02.toByte()),
   GetInfo(0x04.toByte()),
   ClientPIN(0x06.toByte()),
   Reset(0x07.toByte()),
   GetNextAssertion(0x08.toByte());

   companion object {
       fun fromByte(byte: Byte): CTAPCommandType? {
          return when (byte) {
             0x01.toByte() -> MakeCredential
             0x02.toByte() -> GetAssertion
             0x04.toByte() -> GetInfo
             0x06.toByte() -> ClientPIN
             0x07.toByte() -> Reset
             0x08.toByte() -> GetNextAssertion
             else -> null
          }
       }
   }

   fun toByte(): Byte {
      return rawValue.toByte()
   }
}

enum class CTAPStatusCode(val rawValue: Int) {

   Success(0x00),
   InvalidCommand(0x01),
   InvalidParameter(0x02),
   InvalidLength(0x03),
   InvalidSeq(0x04),
   Timeout(0x05),
   ChannelBusy(0x06),
   LockRequired(0x0A),
   InvalidChannel(0x0B),
   CBORUnexpectedType(0x11),
   InvalidCBOR(0x12),
   MissingParameter(0x14),
   LimitExceeded(0x15),
   UnsupportedExtension(0x16),
   CredentialExcluded(0x19),
   Processing(0x21),
   InvalidCredential(0x22),
   UserActionPending(0x23),
   OperationPending(0x24),
   NoOperations(0x25),
   UnsupportedAlgorithm(0x26),
   OperationDenied(0x27),
   KeyStoreFull(0x28),
   NotBusy(0x29),
   NoOperationPending(0x2A),
   UnsupportedOption(0x2B),
   InvalidOption(0x2C),
   KeepAliveCancel(0x2D),
   NoCredentials(0x2E),
   UserActionTimeout(0x2F),
   NotAllowed(0x30),
   PINInvalid(0x31),
   PINBlocked(0x32),
   PINAuthInvalid(0x33),
   PINAuthBlocked(0x34),
   PINNotSet(0x35),
   PINRequired(0x36),
   PINPolicyViolation(0x37),
   PINTokenExpired(0x38),
   RequestTooLarge(0x39),
   ActionTimeout(0x3A),
   UPRequired(0x3B),
   Other(0x7F);

   fun toByte(): Byte {
      return rawValue.toByte()
   }
}