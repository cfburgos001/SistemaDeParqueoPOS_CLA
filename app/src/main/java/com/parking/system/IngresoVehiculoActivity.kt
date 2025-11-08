package com.parking.system

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.parking.system.database.DatabaseResult
import com.parking.system.database.DispositivoManager
import com.parking.system.database.VehiculoRepository
import com.parking.system.databinding.ActivityIngresoVehiculoBinding
import kotlinx.coroutines.launch
import java.util.*

class IngresoVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngresoVehiculoBinding
    private lateinit var vehiculoRepository: VehiculoRepository
    private lateinit var dispositivoManager: DispositivoManager

    private var idOperador: Int = 0
    private var nombreOperador: String = ""
    private var idDispositivo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresoVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoRepository = VehiculoRepository(this)
        dispositivoManager = DispositivoManager(this)

        cargarDatosSesion()
        setupUI()
    }

    private fun cargarDatosSesion() {
        val sharedPref = getSharedPreferences("ParkingSession", MODE_PRIVATE)
        idOperador = sharedPref.getInt("id_operador", 0)
        nombreOperador = sharedPref.getString("nombre_completo", "Desconocido") ?: "Desconocido"

        lifecycleScope.launch {
            idDispositivo = dispositivoManager.obtenerIdDispositivo()
        }
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

        lifecycleScope.launch {
            val result = vehiculoRepository.registrarEntrada(
                receiptData = receiptData,
                usuario = nombreOperador,
                idOperador = idOperador,
                idDispositivo = idDispositivo
            )

            when (result) {
                is DatabaseResult.Success -> {
                    printReceipt(receiptData)
                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "✓ Entrada registrada exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.etPlaca.text?.clear()
                }
                is DatabaseResult.Error -> {
                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "⚠ Error: ${result.message}\nTicket impreso localmente",
                        Toast.LENGTH_LONG
                    ).show()
                    printReceipt(receiptData)
                    binding.etPlaca.text?.clear()
                }
            }

            binding.btnRegistrarEntrada.isEnabled = true
        }
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