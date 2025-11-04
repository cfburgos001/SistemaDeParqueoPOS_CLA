package com.parking.system

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.parking.system.databinding.ActivityIngresoVehiculoBinding
import java.text.SimpleDateFormat
import java.util.*

class IngresoVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngresoVehiculoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresoVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registro de Entrada"

        binding.etPlaca.addTextChangedListener {
            binding.tilPlaca.error = null
        }

        binding.etPlaca.addTextChangedListener { text ->
            val upperCase = text.toString().uppercase()
            if (text.toString() != upperCase) {
                binding.etPlaca.setText(upperCase)
                binding.etPlaca.setSelection(upperCase.length)
            }
        }

        binding.btnRegistrarEntrada.setOnClickListener {
            registrarEntrada()
        }
    }

    private fun registrarEntrada() {
        val placa = binding.etPlaca.text.toString().trim()

        if (placa.isEmpty()) {
            binding.tilPlaca.error = "Ingrese la placa del vehículo"
            return
        }

        if (placa.length < 4) {
            binding.tilPlaca.error = "Placa inválida"
            return
        }

        binding.btnRegistrarEntrada.isEnabled = false

        val receiptData = ReceiptData(
            uniqueId = generateUniqueId(),
            plate = placa,
            entryTime = Date()
        )

        printReceipt(receiptData)

        Toast.makeText(this, "Entrada registrada exitosamente", Toast.LENGTH_SHORT).show()

        binding.etPlaca.text?.clear()
        binding.btnRegistrarEntrada.isEnabled = true
    }

    private fun generateUniqueId(): String {
        val timestamp = System.currentTimeMillis()
        return "PK-${timestamp}"
    }

    private fun printReceipt(data: ReceiptData) {
        try {
            val qrBitmap = generateQRCode(data.uniqueId)

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Por ahora, mostrar en Toast (después conectarás con la impresora real)
            val mensaje = """
                ================================
                   RECIBO DE INGRESO
                   SISTEMA DE PARQUEO
                ================================
                
                ID: ${data.uniqueId}
                
                PLACA: ${data.plate}
                FECHA: ${dateFormatter.format(data.entryTime)}
                HORA: ${timeFormatter.format(data.entryTime)}
                
                ================================
                  CONSERVE ESTE TICKET
                     PARA SU SALIDA
                ================================
            """.trimIndent()

            Toast.makeText(this, "Recibo generado:\n$mensaje", Toast.LENGTH_LONG).show()

            // Aquí integrarás el PrinterManager cuando tengas el SDK del ZKH300

        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar recibo: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun generateQRCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

data class ReceiptData(
    val uniqueId: String,
    val plate: String,
    val entryTime: Date
)