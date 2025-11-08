package com.parking.system

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.parking.system.database.DatabaseResult
import com.parking.system.database.DispositivoManager
import com.parking.system.database.VehiculoDB
import com.parking.system.database.VehiculoRepository
import com.parking.system.databinding.ActivitySalidaConfirmacionBinding
import com.parking.system.hardware.PlumaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalidaConfirmacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalidaConfirmacionBinding
    private lateinit var vehiculoRepository: VehiculoRepository

    private lateinit var vehiculo: VehiculoDB
    private var tiempoMinutos: Int = 0
    private var monto: Double = 0.0

    private lateinit var dispositivoManager: DispositivoManager
    private var idDispositivo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaConfirmacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoRepository = VehiculoRepository(this)
        dispositivoManager = DispositivoManager(this)

        lifecycleScope.launch {
            idDispositivo = dispositivoManager.obtenerIdDispositivo()
        }

        // Obtener datos del vehículo
        val vehiculoId = intent.getIntExtra("VEHICULO_ID", -1)
        val placa = intent.getStringExtra("VEHICULO_PLACA") ?: ""
        val fechaEntrada = intent.getLongExtra("VEHICULO_FECHA", 0)
        val codigoBarras = intent.getStringExtra("VEHICULO_CODIGO") ?: ""
        tiempoMinutos = intent.getIntExtra("TIEMPO_MINUTOS", 0)
        monto = intent.getDoubleExtra("MONTO", 0.0)

        vehiculo = VehiculoDB(
            id = vehiculoId,
            placa = placa,
            fechaEntrada = Date(fechaEntrada),
            codigoBarras = codigoBarras,
            estado = "DENTRO"
        )

        setupUI()
        mostrarInformacion()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Confirmar Salida"

        binding.btnCancelar.setOnClickListener {
            finish()
        }

        binding.btnConfirmar.setOnClickListener {
            confirmarSalida()
        }
    }

    private fun mostrarInformacion() {
        // Placa
        binding.tvPlaca.text = vehiculo.placa

        // Fecha de entrada
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvFechaEntrada.text = dateFormat.format(vehiculo.fechaEntrada)

        // Tiempo de estancia
        val horas = tiempoMinutos / 60
        val minutos = tiempoMinutos % 60
        binding.tvTiempoEstancia.text = if (horas > 0) {
            "$horas hora${if (horas > 1) "s" else ""} $minutos min"
        } else {
            "$minutos minutos"
        }

        // Monto
        binding.tvMonto.text = String.format("$%.2f", monto)
    }

    private fun confirmarSalida() {
        binding.btnConfirmar.isEnabled = false
        binding.btnCancelar.isEnabled = false

        lifecycleScope.launch {
            // Registrar salida con ID de dispositivo
            val result = vehiculoRepository.registrarSalida(vehiculo.placa, monto, idDispositivo)

            when (result) {
                is DatabaseResult.Success -> {
                    Toast.makeText(
                        this@SalidaConfirmacionActivity,
                        "✓ Salida registrada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    mostrarMensajePluma()

                    delay(3000)
                    setResult(RESULT_OK)
                    finish()
                }
                is DatabaseResult.Error -> {
                    Toast.makeText(
                        this@SalidaConfirmacionActivity,
                        "✗ Error: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnConfirmar.isEnabled = true
                    binding.btnCancelar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarMensajePluma() {
        lifecycleScope.launch {
            Toast.makeText(
                this@SalidaConfirmacionActivity,
                "🚧 LEVANTANDO LA PLUMA...",
                Toast.LENGTH_LONG
            ).show()

            // Levantar pluma (por ahora simulado)
            PlumaController.levantarPluma(duracionSegundos = 5)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
