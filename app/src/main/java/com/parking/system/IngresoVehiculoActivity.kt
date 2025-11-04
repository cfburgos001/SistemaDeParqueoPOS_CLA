package com.parking.system

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.parking.system.databinding.ActivityIngresoVehiculoBinding
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
            com.parking.system.printer.PrinterManager.printReceipt(this, data)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}