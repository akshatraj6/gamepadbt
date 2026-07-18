package com.gamepadbt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepadbt.app.GamepadDescriptor
import com.gamepadbt.app.Layout
import kotlin.math.roundToInt

@Composable
fun GamepadScreen(
    layout: Layout,
    connected: Boolean,
    connectedName: String?,
    vibrationEnabled: Boolean,
    onVibrate: () -> Unit,
    onSendReport: (ByteArray) -> Unit,
    onBack: () -> Unit,
    onChangeLayout: (Layout) -> Unit
) {
    var buttons by remember { mutableStateOf(IntArray(8) { 0 }) }
    var hat by remember { mutableStateOf(GamepadDescriptor.HAT_CENTER) }
    var leftX by remember { mutableStateOf(128) }
    var leftY by remember { mutableStateOf(128) }
    var rightX by remember { mutableStateOf(128) }
    var rightY by remember { mutableStateOf(128) }

    fun push() {
        val builder = GamepadDescriptor.ReportBuilder()
        for (i in buttons.indices) builder.setButton(i, buttons[i] == 1)
        builder.setHat(hat)
        builder.setLeftStick(leftX, leftY)
        builder.setRightStick(rightX, rightY)
        onSendReport(builder.build())
    }

    fun press(index: Int, pressed: Boolean) {
        buttons = buttons.copyOf().also { it[index] = if (pressed) 1 else 0 }
        if (pressed && vibrationEnabled) onVibrate()
        push()
    }

    fun setHat(direction: Int) {
        hat = direction
        if (direction != GamepadDescriptor.HAT_CENTER && vibrationEnabled) onVibrate()
        push()
    }

    fun setLeftStick(x: Int, y: Int) {
        leftX = x; leftY = y
        push()
    }

    fun setRightStick(x: Int, y: Int) {
        rightX = x; rightY = y
        push()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("< Home") }
            Text(if (connected) "\u25CF ${connectedName ?: "Connected"}" else "\u25CB Disconnected")
            LayoutPicker(current = layout, onSelect = onChangeLayout)
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (layout) {
                Layout.STANDARD -> StandardLayout(::press, ::setHat)
                Layout.SIMPLE_REMOTE -> SimpleRemoteLayout(::press, ::setHat)
                Layout.TWIN_STICK -> TwinStickLayout(::press, ::setLeftStick, ::setRightStick)
            }
        }
    }
}

@Composable
fun LayoutPicker(current: Layout, onSelect: (Layout) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(current.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Layout.entries.forEach { l ->
                DropdownMenuItem(text = { Text(l.name) }, onClick = { onSelect(l); expanded = false })
            }
        }
    }
}

@Composable
fun GamepadButton(label: String, onPress: (Boolean) -> Unit, size: Int = 64) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF3A3A3A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun DPadButton(label: String, direction: Int, onHat: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHat(direction)
                        tryAwaitRelease()
                        onHat(GamepadDescriptor.HAT_CENTER)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White)
    }
}

@Composable
fun DPad(onHat: (Int) -> Unit) {
    Column(
        modifier = Modifier.size(160.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            DPadButton("\u2191", GamepadDescriptor.HAT_UP, onHat)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DPadButton("\u2190", GamepadDescriptor.HAT_LEFT, onHat)
            Spacer(Modifier.size(46.dp))
            DPadButton("\u2192", GamepadDescriptor.HAT_RIGHT, onHat)
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            DPadButton("\u2193", GamepadDescriptor.HAT_DOWN, onHat)
        }
    }
}

@Composable
fun FaceButtons(onButton: (Int, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("Y", { p -> onButton(3, p) })
        Row {
            GamepadButton("X", { p -> onButton(2, p) })
            Spacer(Modifier.size(64.dp))
            GamepadButton("B", { p -> onButton(1, p) })
        }
        GamepadButton("A", { p -> onButton(0, p) })
    }
}

@Composable
fun StandardLayout(onButton: (Int, Boolean) -> Unit, onHat: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GamepadButton("L", { p -> onButton(4, p) }, size = 52)
            GamepadButton("R", { p -> onButton(5, p) }, size = 52)
        }
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onHat)
            FaceButtons(onButton)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            GamepadButton("Select", { p -> onButton(6, p) }, size = 56)
            Spacer(Modifier.size(24.dp))
            GamepadButton("Start", { p -> onButton(7, p) }, size = 56)
        }
    }
}

@Composable
fun SimpleRemoteLayout(onButton: (Int, Boolean) -> Unit, onHat: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DPad(onHat)
        Spacer(Modifier.height(24.dp))
        GamepadButton("OK", { p -> onButton(0, p) }, size = 72)
        Spacer(Modifier.height(16.dp))
        Row {
            GamepadButton("Back", { p -> onButton(1, p) }, size = 56)
            Spacer(Modifier.size(24.dp))
            GamepadButton("Home", { p -> onButton(7, p) }, size = 56)
        }
    }
}

@Composable
fun VirtualJoystick(size: Int = 140, onMove: (Int, Int) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val radius = size / 2f

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-radius, radius)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-radius, radius)
                        val x = (128 + (offsetX / radius * 127)).roundToInt().coerceIn(0, 255)
                        val y = (128 + (offsetY / radius * 127)).roundToInt().coerceIn(0, 255)
                        onMove(x, y)
                    },
                    onDragEnd = {
                        offsetX = 0f; offsetY = 0f
                        onMove(128, 128)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFF6A6A6A))
        )
    }
}

@Composable
fun TwinStickLayout(
    onButton: (Int, Boolean) -> Unit,
    onLeftStick: (Int, Int) -> Unit,
    onRightStick: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VirtualJoystick(onMove = onLeftStick)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GamepadButton("A", { p -> onButton(0, p) })
            Spacer(Modifier.height(12.dp))
            GamepadButton("B", { p -> onButton(1, p) })
        }
        VirtualJoystick(onMove = onRightStick)
    }
}
