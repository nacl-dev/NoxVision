package com.noxvision.app

/**
 * Binary frames for the Guide camera `customdata` control channel.
 * Layout: 0x55 0xAA, payload length, command, big-endian value, XOR checksum, 0xF0.
 */
object GuideControlFrames {
    const val DEFAULT_PALETTE = "whitehot"

    private val paletteCommand = byteArrayOf(0x02, 0x00, 0x04)
    private val hotspotCommand = byteArrayOf(0x02, 0x00, 0x09)
    private val brightnessCommand = byteArrayOf(0x02, 0x03, 0x09)
    private val contrastCommand = byteArrayOf(0x02, 0x03, 0x0A)
    private val enhancementCommand = byteArrayOf(0x02, 0x03, 0x0B)

    private val paletteIds = mapOf(
        "whitehot" to 0,
        "blackhot" to 9,
        "redhot" to 11,
        "iron" to 2,
        "bluehot" to 4,
        "greenhot" to 5,
        "darkbrown" to 20
    )

    fun paletteId(name: String): Int? = paletteIds[name]

    fun paletteFrame(paletteId: Int): ByteArray = build(paletteCommand, paletteId)

    fun hotspotFrame(enabled: Boolean): ByteArray = build(hotspotCommand, if (enabled) 1 else 0)

    fun brightnessFrame(level: Int): ByteArray = build(brightnessCommand, level.coerceIn(1, 5))

    fun contrastFrame(level: Int): ByteArray = build(contrastCommand, level.coerceIn(1, 5))

    fun enhancementFrame(enabled: Boolean): ByteArray = build(enhancementCommand, if (enabled) 1 else 0)

    fun build(cmd: ByteArray, value: Int): ByteArray {
        val frameHead = byteArrayOf(0x55.toByte(), 0xAA.toByte())

        val valueBytes = byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )

        val payloadLen: Byte = (cmd.size + valueBytes.size).toByte()
        val payload = ByteArray(1 + cmd.size + valueBytes.size)
        payload[0] = payloadLen
        System.arraycopy(cmd, 0, payload, 1, cmd.size)
        System.arraycopy(valueBytes, 0, payload, 1 + cmd.size, valueBytes.size)

        var checksum = payload[0]
        for (i in 1 until payload.size) {
            checksum = (checksum.toInt() xor payload[i].toInt()).toByte()
        }

        val frame = ByteArray(frameHead.size + payload.size + 2)
        frame[0] = frameHead[0]
        frame[1] = frameHead[1]
        System.arraycopy(payload, 0, frame, 2, payload.size)
        frame[2 + payload.size] = checksum
        frame[3 + payload.size] = 0xF0.toByte()

        return frame
    }
}
