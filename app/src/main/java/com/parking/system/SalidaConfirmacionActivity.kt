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

        cargarDatosIntent()
        setupUI()
        mostrarInformacion()
    }

    private fun cargarDatosIntent() {
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
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        binding.tvPlaca.text = vehiculo.placa
        binding.tvFechaEntrada.text = dateFormat.format(vehiculo.fechaEntrada)

        val horas = tiempoMinutos / 60
        val minutos = tiempoMinutos % 60
        val tiempoTexto = if (horas > 0) {
            "$horas hora${if (horas > 1) "s" else ""} $minutos min"
        } else {
            "$minutos minutos"
        }
        binding.tvTiempoEstancia.text = tiempoTexto
        binding.tvMonto.text = String.format("$%.2f", monto)

        mostrarInfoPago(dateFormat, tiempoTexto)
    }

    private fun mostrarInfoPago(dateFormat: SimpleDateFormat, tiempoTexto: String) {
        val infoPago = StringBuilder()
        infoPago.append("✅ INFORMACIÓN DE PAGO:\n\n")

        if (vehiculo.fechaPago != null) {
            infoPago.append("📅 Fecha de pago: ${dateFormat.format(vehiculo.fechaPago)}\n")
        } else {
            infoPago.append("📅 Fecha de pago: No registrada\n")
        }

        infoPago.append("💳 Estado: ${if (vehiculo.bitPaid == 1) "PAGADO" else "PENDIENTE"}\n")
        infoPago.append("🔑 Tarifa aplicada: ${vehiculo.strRateKey}\n")
        infoPago.append("⏱️ Tiempo total: $tiempoTexto")

        Toast.makeText(this, infoPago.toString(), Toast.LENGTH_LONG).show()
    }

    private fun confirmarSalida() {
        binding.btnConfirmar.isEnabled = false
        binding.btnCancelar.isEnabled = false

        lifecycleScope.launch {
            val result = vehiculoRepository.registrarSalida(vehiculo.placa, idDispositivo)

            when (result) {
                is DatabaseResult.Success -> {
                    Toast.makeText(
                        this@SalidaConfirmacionActivity,
                        "✓ Salida registrada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    levantarPlumaYSalir()
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

    private fun levantarPlumaYSalir() {
        lifecycleScope.launch {
            Toast.makeText(
                this@SalidaConfirmacionActivity,
                "🚧 LEVANTANDO LA PLUMA...",
                Toast.LENGTH_LONG
            ).show()

            PlumaController.levantarPluma(duracionSegundos = 5)

            delay(3000)
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}