package webauthnkit.core.ctap.ble.frame

import webauthnkit.core.ctap.ble.BLECommandType
import java.util.*

class FrameSplitter(
    maxPacketDataSize: Int
) {
    private val firstFragmentMaxDataSize = maxPacketDataSize - 3
    private val restFragmentsMaxDataSize = maxPacketDataSize - 1

    fun split(
        command: BLECommandType,
        data:    ByteArray
    ): Pair<Frame, List<ContinuationFrame>> {

        val len = data.size

        if (len > firstFragmentMaxDataSize) {

            val firstData = Arrays.copyOfRange(data, 0, firstFragmentMaxDataSize)
            var pos = firstFragmentMaxDataSize
            val first = Frame(command, len, firstData)

            val fragments =
                mutableListOf<ContinuationFrame>()

            var seq = 0

            while (pos < len) {

                val restSize = len - pos

                if (restSize < restFragmentsMaxDataSize) {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restSize)
                    fragments.add(ContinuationFrame(seq, fragment))
                    break

                } else {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restFragmentsMaxDataSize)
                    pos += restFragmentsMaxDataSize
                    fragments.add(ContinuationFrame(seq, fragment))
                    seq += 1

                }

            }

            return Pair(first, fragments)

        } else {

            return Pair(Frame(command, len, data), listOf())

        }
    }

}

