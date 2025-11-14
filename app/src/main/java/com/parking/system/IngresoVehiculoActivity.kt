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
import com.parking.system.hardware.PlumaController
import kotlinx.coroutines.launch
import java.util.Date

class IngresoVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngresoVehiculoBinding
    private lateinit var vehiculoRepository: VehiculoRepository
    private lateinit var dispositivoManager: DispositivoManager

    private var idOperador: Int = 0
    private var nombreOperador: String = ""
    private var idDispositivo: String = ""

    // Variable para guardar el último ticket impreso
    private var ultimoTicketImpreso: ReceiptData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngresoVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoRepository = VehiculoRepository(this)
        dispositivoManager = DispositivoManager(this)

        cargarDatosSesion()
        verificarTipoDispositivo()
        setupUI()
    }

    private fun verificarTipoDispositivo() {
        if (!dispositivoManager.puedeRegistrarEntrada()) {
            Toast.makeText(
                this,
                "⚠ Este dispositivo está configurado como SALIDA\nNo puede registrar entradas",
                Toast.LENGTH_LONG
            ).show()

            binding.btnRegistrarEntrada.isEnabled = false
            binding.etPlaca.isEnabled = false
        }
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

        // ⭐ NUEVO: Botón de reimpresión
        binding.btnReimprimir.setOnClickListener {
            reimprimirUltimoTicket()
        }

        // Inicialmente deshabilitar botón de reimpresión
        binding.btnReimprimir.isEnabled = false
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
                    // 1. Guardar como último ticket impreso
                    ultimoTicketImpreso = receiptData

                    // 2. Imprimir ticket
                    printReceipt(receiptData)

                    // 3. Mostrar mensaje de éxito
                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "✓ Entrada registrada exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 4. Levantar la pluma
                    levantarPluma()

                    // 5. Limpiar campo
                    binding.etPlaca.text?.clear()

                    // 6. Habilitar botón de reimpresión
                    binding.btnReimprimir.isEnabled = true
                }
                is DatabaseResult.Error -> {
                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "⚠ Error: ${result.message}\nTicket impreso localmente",
                        Toast.LENGTH_LONG
                    ).show()

                    // Aún así imprimir ticket, levantar pluma y guardar para reimpresión
                    ultimoTicketImpreso = receiptData
                    printReceipt(receiptData)
                    levantarPluma()
                    binding.etPlaca.text?.clear()
                    binding.btnReimprimir.isEnabled = true
                }
            }

            binding.btnRegistrarEntrada.isEnabled = true
        }
    }

    /**
     * ⭐ NUEVA FUNCIÓN: Reimprime el último ticket e incrementa bitCopy
     */
    private fun reimprimirUltimoTicket() {
        if (ultimoTicketImpreso == null) {
            Toast.makeText(
                this,
                "No hay ticket para reimprimir",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnReimprimir.isEnabled = false

        lifecycleScope.launch {
            // 1. Incrementar bitCopy en la BD
            val result = vehiculoRepository.incrementarBitCopy(ultimoTicketImpreso!!.uniqueId)

            when (result) {
                is DatabaseResult.Success -> {
                    // 2. Imprimir el ticket
                    printReceipt(ultimoTicketImpreso!!)

                    // 3. Mostrar mensaje con número de reimpresiones
                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "✓ Ticket reimpreso\n${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is DatabaseResult.Error -> {
                    // Si falla la BD, igual imprimir pero avisar
                    printReceipt(ultimoTicketImpreso!!)

                    Toast.makeText(
                        this@IngresoVehiculoActivity,
                        "⚠ Ticket reimpreso\n(No se actualizó contador: ${result.message})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            binding.btnReimprimir.isEnabled = true
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

    /**
     * Levanta la pluma de entrada
     */
    private fun levantarPluma() {
        lifecycleScope.launch {
            Toast.makeText(
                this@IngresoVehiculoActivity,
                "🚧 LEVANTANDO LA PLUMA...",
                Toast.LENGTH_LONG
            ).show()

            val exito = PlumaController.levantarPluma(duracionSegundos = 5)

            if (exito) {
                Toast.makeText(
                    this@IngresoVehiculoActivity,
                    "✓ Pluma levantada - Puede pasar",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@IngresoVehiculoActivity,
                    "⚠ Error al controlar la pluma",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}