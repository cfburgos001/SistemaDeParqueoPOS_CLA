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
import java.util.Date
import java.util.Locale

class SalidaConfirmacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalidaConfirmacionBinding
    private lateinit var vehiculoRepository: VehiculoRepository

    private lateinit var vehiculo: VehiculoDB
    private var tiempoMinutos: Int = 0
    private var monto: Double = 0.0
    private var fechaPago: Date? = null

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
        val fechaPagoLong = intent.getLongExtra("FECHA_PAGO", 0L)
        val bitPaid = intent.getIntExtra("BIT_PAID", 0)

        if (fechaPagoLong > 0) {
            fechaPago = Date(fechaPagoLong)
        }

        vehiculo = VehiculoDB(
            id = vehiculoId,
            placa = placa,
            fechaEntrada = Date(fechaEntrada),
            codigoBarras = codigoBarras,
            estado = "DENTRO",
            bitPaid = bitPaid,
            fechaPago = fechaPago,
            monto = monto
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

        // Monto pagado
        binding.tvMonto.text = String.format("$%.2f", monto)

        // Mostrar info de pago si existe
        if (vehiculo.fechaPago != null) {
            val infoPago = "✓ Pagado el ${dateFormat.format(vehiculo.fechaPago)}"
            // Puedes agregar un TextView para mostrar esto
            Toast.makeText(this, infoPago, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmarSalida() {
        binding.btnConfirmar.isEnabled = false
        binding.btnCancelar.isEnabled = false

        lifecycleScope.launch {
            // Registrar salida SIN monto (ya está registrado por PayStation)
            val result = vehiculoRepository.registrarSalida(vehiculo.placa, idDispositivo)

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

            // Levantar pluma
            PlumaController.levantarPluma(duracionSegundos = 5)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}