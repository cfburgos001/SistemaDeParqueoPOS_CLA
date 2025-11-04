package com.parking.system.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import com.parking.system.ReceiptData
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager para impresión Bluetooth en impresoras térmicas compatibles con ESC/POS
 * Optimizado para Terminal ZKH300 y sistema de parqueo
 */
@SuppressLint("MissingPermission")
object PrinterManager {

    // ⚠️ IMPORTANTE: Reemplaza con la dirección MAC real de tu impresora
    // Formato: "AA:BB:CC:DD:EE:FF"
    // Puedes encontrarla en: Configuración > Bluetooth > Dispositivos emparejados
    private const val PRINTER_MAC = "66:11:22:33:44:55"

    // UUID estándar para comunicación serial Bluetooth (SPP)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Obtiene el dispositivo Bluetooth de la impresora
     */
    private fun getDevice(): BluetoothDevice? {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                return null
            }
            adapter.getRemoteDevice(PRINTER_MAC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Imprime el recibo de entrada de vehículo
     */
    fun printReceipt(context: Context, data: ReceiptData) {
        val fallbackText = buildReceiptText(data)

        try {
            val device = getDevice()
            if (device == null) {
                showFallback(context, fallbackText, "Impresora no encontrada")
                return
            }

            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.cancelDiscovery() // Detener búsqueda para mejorar conexión

            // Intentar conexión segura primero
            val socket = try {
                val socketSafe = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socketSafe.connect()
                socketSafe
            } catch (e1: Exception) {
                // Si falla, intentar conexión insegura (común en térmicas antiguas)
                try {
                    val insecure = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    insecure.connect()
                    insecure
                } catch (e2: Exception) {
                    showFallback(context, fallbackText, "No se pudo conectar")
                    return
                }
            }

            val outputStream: OutputStream = socket.outputStream

            // Enviar comandos ESC/POS a la impresora
            sendEscPosPrint(outputStream, data)

            outputStream.flush()
            socket.close()

            Toast.makeText(context, "✓ Recibo impreso correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            showFallback(context, fallbackText, "Error al imprimir: ${e.message}")
        }
    }

    /**
     * Envía comandos ESC/POS para imprimir el ticket
     */
    private fun sendEscPosPrint(out: OutputStream, data: ReceiptData) {
        // Inicializar impresora
        out.write(byteArrayOf(0x1B, 0x40))

        // ===== ENCABEZADO =====
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar texto
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita ON
        out.write("================================\n".utf8())
        out.write("   RECIBO DE INGRESO\n".utf8())
        out.write("   SISTEMA DE PARQUEO\n".utf8())
        out.write("================================\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Negrita OFF
        out.write("\n".utf8())

        // ===== CÓDIGO QR SIMULADO =====
        out.write(simulatedQR(data.uniqueId).utf8())
        out.write("\n".utf8())

        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita ON
        out.write("ID: ${data.uniqueId}\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Negrita OFF
        out.write("\n".utf8())

        // ===== INFORMACIÓN DEL VEHÍCULO =====
        out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Alinear a la izquierda
        out.write("--------------------------------\n".utf8())

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita ON
        out.write("PLACA:  ${data.plate}\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Negrita OFF

        out.write("\n".utf8())
        out.write("FECHA:  ${dateFormatter.format(data.entryTime)}\n".utf8())
        out.write("HORA:   ${timeFormatter.format(data.entryTime)}\n".utf8())
        out.write("\n".utf8())

        // ===== FOOTER =====
        out.write("--------------------------------\n".utf8())
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita ON
        out.write("CONSERVE ESTE TICKET\n".utf8())
        out.write("PARA SU SALIDA\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Negrita OFF
        out.write("================================\n".utf8())

        // Saltos de línea finales
        out.write("\n\n\n".utf8())

        // Corte de papel (si la impresora lo soporta)
        out.write(byteArrayOf(0x1D, 0x56, 0x01)) // Corte parcial
    }

    /**
     * Genera un código QR ASCII simulado
     */
    private fun simulatedQR(seed: String, size: Int = 17): String {
        val random = Random(seed.hashCode().toLong())
        val sb = StringBuilder()

        // Borde superior
        sb.appendLine("  ╔" + "█".repeat(size) + "╗")

        // Contenido del QR
        repeat(size) {
            sb.append("  █")
            repeat(size) {
                sb.append(if (random.nextBoolean()) "█" else " ")
            }
            sb.appendLine("█")
        }

        // Borde inferior
        sb.append("  ╚" + "█".repeat(size) + "╝")

        return sb.toString()
    }

    /**
     * Construye el texto del recibo para mostrar en pantalla
     */
    private fun buildReceiptText(data: ReceiptData): String {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        return buildString {
            appendLine("================================")
            appendLine("   RECIBO DE INGRESO")
            appendLine("   SISTEMA DE PARQUEO")
            appendLine("================================")
            appendLine()
            appendLine(simulatedQR(data.uniqueId, 13))
            appendLine()
            appendLine("ID: ${data.uniqueId}")
            appendLine()
            appendLine("--------------------------------")
            appendLine("PLACA:  ${data.plate}")
            appendLine()
            appendLine("FECHA:  ${dateFormatter.format(data.entryTime)}")
            appendLine("HORA:   ${timeFormatter.format(data.entryTime)}")
            appendLine()
            appendLine("--------------------------------")
            appendLine("   CONSERVE ESTE TICKET")
            appendLine("      PARA SU SALIDA")
            appendLine("================================")
        }
    }

    /**
     * Muestra el recibo en un Toast cuando no se puede imprimir
     */
    private fun showFallback(context: Context, text: String, reason: String) {
        Toast.makeText(
            context,
            "$reason\n\nRecibo generado:\n\n$text",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Verifica si el Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }

    /**
     * Verifica si la impresora está emparejada
     */
    fun isPrinterPaired(context: Context): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            val pairedDevices = adapter.bondedDevices
            pairedDevices.any { it.address == PRINTER_MAC }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extension para convertir String a ByteArray UTF-8
     */
    private fun String.utf8(): ByteArray = toByteArray(Charset.forName("UTF-8"))
}