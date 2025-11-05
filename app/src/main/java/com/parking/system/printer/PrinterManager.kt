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
 * Manager para impresión Bluetooth - Versión compatible con todas las térmicas
 * Ticket compacto con información clara
 */
@SuppressLint("MissingPermission")
object PrinterManager {

    private const val PRINTER_MAC = "66:11:22:33:44:55"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private fun getDevice(): BluetoothDevice? {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) return null
            adapter.getRemoteDevice(PRINTER_MAC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun printReceipt(context: Context, data: ReceiptData) {
        val fallbackText = buildReceiptText(data)

        try {
            val device = getDevice()
            if (device == null) {
                showFallback(context, fallbackText, "Impresora no encontrada")
                return
            }

            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.cancelDiscovery()

            val socket = try {
                val socketSafe = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socketSafe.connect()
                socketSafe
            } catch (e1: Exception) {
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
            sendEscPosPrint(outputStream, data)
            outputStream.flush()
            socket.close()

            Toast.makeText(context, "✓ Ticket impreso", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            showFallback(context, fallbackText, "Error: ${e.message}")
        }
    }

    /**
     * Imprime ticket compacto solo con texto
     */
    private fun sendEscPosPrint(out: OutputStream, data: ReceiptData) {
        // Inicializar
        out.write(byteArrayOf(0x1B, 0x40))

        // Encabezado
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita
        out.write("SISTEMA DE PARQUEO\n".utf8())
        out.write("TICKET DE INGRESO\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("========================\n".utf8())
        out.write("\n".utf8())

        // Código de barras visual (más confiable que QR para estas impresoras)
        out.write(generateBarcodeText(data.plate).utf8())
        out.write("\n".utf8())

        // Información principal
        out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Izquierda

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Negrita
        out.write("PLACA: ${data.plate}\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00))

        out.write("\n".utf8())
        out.write("FECHA: ${dateFormatter.format(data.entryTime)}\n".utf8())
        out.write("HORA:  ${timeFormatter.format(data.entryTime)}\n".utf8())
        out.write("\n".utf8())
        out.write("ID: ${data.uniqueId}\n".utf8())

        // Footer
        out.write("========================\n".utf8())
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
        out.write(byteArrayOf(0x1B, 0x45, 0x01))
        out.write("CONSERVE ESTE TICKET\n".utf8())
        out.write("PARA SU SALIDA\n".utf8())
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write("========================\n".utf8())

        out.write("\n\n\n".utf8())

        // Corte
        out.write(byteArrayOf(0x1D, 0x56, 0x01))
    }

    /**
     * Genera un código de barras ASCII visual
     * Más confiable que QR para impresoras básicas
     */
    private fun generateBarcodeText(plate: String): String {
        val sb = StringBuilder()

        // Título
        sb.appendLine("  CODIGO DE VEHICULO:")
        sb.appendLine()

        // Barras verticales para cada carácter de la placa
        sb.append("  ")
        for (char in plate) {
            val hash = char.hashCode() % 5 + 3 // 3-7 barras por carácter
            sb.append("|".repeat(hash))
            sb.append(" ")
        }
        sb.appendLine()

        // Placa legible debajo
        sb.append("  ")
        for (char in plate) {
            sb.append(" $char ")
        }
        sb.appendLine()

        // Línea de separación
        sb.append("  ")
        sb.append("-".repeat(plate.length * 3))

        return sb.toString()
    }

    private fun buildReceiptText(data: ReceiptData): String {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return buildString {
            appendLine("SISTEMA DE PARQUEO")
            appendLine("TICKET DE INGRESO")
            appendLine("========================")
            appendLine()
            appendLine(generateBarcodeText(data.plate))
            appendLine()
            appendLine("PLACA: ${data.plate}")
            appendLine()
            appendLine("FECHA/HORA:")
            appendLine(dateFormatter.format(data.entryTime))
            appendLine()
            appendLine("ID: ${data.uniqueId}")
            appendLine("========================")
            appendLine("CONSERVE ESTE TICKET")
            appendLine("PARA SU SALIDA")
            appendLine("========================")
        }
    }

    private fun showFallback(context: Context, text: String, reason: String) {
        Toast.makeText(context, "$reason\n\n$text", Toast.LENGTH_LONG).show()
    }

    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }

    fun isPrinterPaired(context: Context): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            adapter.bondedDevices.any { it.address == PRINTER_MAC }
        } catch (e: Exception) {
            false
        }
    }

    private fun String.utf8(): ByteArray = toByteArray(Charset.forName("UTF-8"))
}