package com.gamepadbt.app

/**
 * Defines the Bluetooth HID report descriptor this app registers as, and a
 * small helper to build the raw report bytes sent on every input change.
 *
 * Report layout (6 bytes, after the report ID which Android's HID API
 * handles separately):
 *   byte 0: 8 buttons, one bit each
 *   byte 1: low nibble = hat switch / d-pad (0-7 = direction, 8 = centered)
 *   byte 2: left stick X   (0-255, 128 = center)
 *   byte 3: left stick Y   (0-255, 128 = center)
 *   byte 4: right stick X  (0-255, 128 = center)
 *   byte 5: right stick Y  (0-255, 128 = center)
 *
 * This follows the standard USB HID Gamepad usage page. It's written from
 * the public HID specification, but since it can't be tested against real
 * hardware here, minor tweaks may be needed after the first real pairing.
 */
object GamepadDescriptor {

    const val REPORT_ID: Byte = 0x01

    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x05,                    // Usage (Game Pad)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), REPORT_ID,      //   Report ID
        0x05, 0x09,                    //   Usage Page (Button)
        0x19, 0x01,                    //   Usage Minimum (Button 1)
        0x29, 0x08,                    //   Usage Maximum (Button 8)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x01,                    //   Logical Maximum (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data,Var,Abs)
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x39,                    //   Usage (Hat Switch)
        0x15, 0x00,                    //   Logical Minimum (0)
        0x25, 0x07,                    //   Logical Maximum (7)
        0x35, 0x00,                    //   Physical Minimum (0)
        0x46.toByte(), 0x3B, 0x01,     //   Physical Maximum (315)
        0x65, 0x14,                    //   Unit (Eng Rot: Angular Pos)
        0x75, 0x04,                    //   Report Size (4)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x42,           //   Input (Data,Var,Abs,Null)
        0x65, 0x00,                    //   Unit (None)
        0x75, 0x04,                    //   Report Size (4) - padding
        0x95.toByte(), 0x01,
        0x81.toByte(), 0x01,           //   Input (Const) - padding
        0x05, 0x01,                    //   Usage Page (Generic Desktop)
        0x09, 0x01,                    //   Usage (Pointer)
        0xA1.toByte(), 0x00,           //   Collection (Physical)
        0x09, 0x30,                    //     Usage (X)
        0x09, 0x31,                    //     Usage (Y)
        0x09, 0x33,                    //     Usage (Rx)
        0x09, 0x34,                    //     Usage (Ry)
        0x15, 0x00,                    //     Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00,     //     Logical Maximum (255)
        0x75, 0x08,                    //     Report Size (8)
        0x95.toByte(), 0x04,           //     Report Count (4)
        0x81.toByte(), 0x02,           //     Input (Data,Var,Abs)
        0xC0.toByte(),                 //   End Collection
        0xC0.toByte()                  // End Collection
    )

    const val HAT_UP = 0
    const val HAT_UP_RIGHT = 1
    const val HAT_RIGHT = 2
    const val HAT_DOWN_RIGHT = 3
    const val HAT_DOWN = 4
    const val HAT_DOWN_LEFT = 5
    const val HAT_LEFT = 6
    const val HAT_UP_LEFT = 7
    const val HAT_CENTER = 8

    fun emptyReport(): ByteArray = ReportBuilder().build()

    class ReportBuilder {
        private var buttons: Int = 0
        private var hat: Int = HAT_CENTER
        private var lx: Int = 128
        private var ly: Int = 128
        private var rx: Int = 128
        private var ry: Int = 128

        fun setButton(index: Int, pressed: Boolean): ReportBuilder {
            buttons = if (pressed) buttons or (1 shl index) else buttons and (1 shl index).inv()
            return this
        }

        fun setHat(direction: Int): ReportBuilder {
            hat = direction
            return this
        }

        fun setLeftStick(x: Int, y: Int): ReportBuilder {
            lx = x.coerceIn(0, 255)
            ly = y.coerceIn(0, 255)
            return this
        }

        fun setRightStick(x: Int, y: Int): ReportBuilder {
            rx = x.coerceIn(0, 255)
            ry = y.coerceIn(0, 255)
            return this
        }

        fun build(): ByteArray = byteArrayOf(
            buttons.toByte(),
            hat.toByte(),
            lx.toByte(),
            ly.toByte(),
            rx.toByte(),
            ry.toByte()
        )
    }
}
