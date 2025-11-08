package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.parking.system.database.CalculoResult
import com.parking.system.database.VehiculoDB
import com.parking.system.database.VehiculoRepository
import com.parking.system.database.VehiculoResult
import com.parking.system.databinding.ActivitySalidaVehiculoBinding
import kotlinx.coroutines.launch

class SalidaVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalidaVehiculoBinding
    private lateinit var vehiculoRepository: VehiculoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoRepository = VehiculoRepository(this)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registro de Salida"

        binding.btnRegistrarPorPlaca.setOnClickListener {
            val placa = binding.etPlaca.text.toString().trim()

            if (placa.isEmpty()) {
                binding.tilPlaca.error = "Ingrese la placa del vehículo"
                return@setOnClickListener
            }

            if (placa.length < 4) {
                binding.tilPlaca.error = "Placa inválida"
                return@setOnClickListener
            }

            buscarVehiculoPorPlaca(placa)
        }

        binding.btnEscanearQR.setOnClickListener {
            iniciarEscaner()
        }

        binding.etPlaca.addTextChangedListener { text ->
            val upperCase = text.toString().uppercase()
            if (text.toString() != upperCase) {
                binding.etPlaca.setText(upperCase)
                binding.etPlaca.setSelection(upperCase.length)
            }
        }

        binding.etPlaca.addTextChangedListener {
            binding.tilPlaca.error = null
        }
    }

    private fun buscarVehiculoPorPlaca(placa: String) {
        binding.btnRegistrarPorPlaca.isEnabled = false

        lifecycleScope.launch {
            when (val result = vehiculoRepository.buscarVehiculoPorPlaca(placa)) {
                is VehiculoResult.Found -> {
                    calcularYMostrarMonto(result.vehiculo)
                }
                is VehiculoResult.NotFound -> {
                    Toast.makeText(
                        this@SalidaVehiculoActivity,
                        "Vehículo no encontrado o ya salió del parqueo",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnRegistrarPorPlaca.isEnabled = true
                }
                is VehiculoResult.Error -> {
                    Toast.makeText(
                        this@SalidaVehiculoActivity,
                        "Error: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnRegistrarPorPlaca.isEnabled = true
                }
            }
        }
    }

    private fun buscarVehiculoPorCodigo(codigo: String) {
        lifecycleScope.launch {
            when (val result = vehiculoRepository.buscarVehiculoPorCodigo(codigo)) {
                is VehiculoResult.Found -> {
                    calcularYMostrarMonto(result.vehiculo)
                }
                is VehiculoResult.NotFound -> {
                    Toast.makeText(
                        this@SalidaVehiculoActivity,
                        "Ticket no encontrado o vehículo ya salió",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is VehiculoResult.Error -> {
                    Toast.makeText(
                        this@SalidaVehiculoActivity,
                        "Error: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun calcularYMostrarMonto(vehiculo: VehiculoDB) {
        val calculo = vehiculoRepository.calcularMonto(vehiculo)

        when (calculo) {
            is CalculoResult.Success -> {
                // Abrir pantalla de confirmación con los datos
                val intent = Intent(this, SalidaConfirmacionActivity::class.java)
                intent.putExtra("VEHICULO_ID", vehiculo.id)
                intent.putExtra("VEHICULO_PLACA", vehiculo.placa)
                intent.putExtra("VEHICULO_FECHA", vehiculo.fechaEntrada.time)
                intent.putExtra("VEHICULO_CODIGO", vehiculo.codigoBarras)
                intent.putExtra("TIEMPO_MINUTOS", calculo.tiempoMinutos)
                intent.putExtra("MONTO", calculo.monto)
                startActivityForResult(intent, REQUEST_CODE_CONFIRMACION)

                binding.btnRegistrarPorPlaca.isEnabled = true
            }
            is CalculoResult.Error -> {
                Toast.makeText(
                    this,
                    "Error al calcular monto: ${calculo.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnRegistrarPorPlaca.isEnabled = true
            }
        }
    }

    private fun iniciarEscaner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(
            IntentIntegrator.QR_CODE,
            IntentIntegrator.CODE_128
        )
        integrator.setPrompt("Escanee el código del ticket")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIRMACION) {
            if (resultCode == RESULT_OK) {
                binding.etPlaca.text?.clear()
                binding.btnRegistrarPorPlaca.isEnabled = true
            } else {
                binding.btnRegistrarPorPlaca.isEnabled = true
            }
            return
        }

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                val codigoEscaneado = result.contents

                Toast.makeText(
                    this,
                    "Código escaneado: $codigoEscaneado",
                    Toast.LENGTH_SHORT
                ).show()

                // Detectar si es ID único (PK-) o placa directa
                if (codigoEscaneado.startsWith("PK-", ignoreCase = true)) {
                    // Es un código de barras con ID único
                    buscarVehiculoPorCodigo(codigoEscaneado)
                } else {
                    // Es una placa directa
                    buscarVehiculoPorPlaca(codigoEscaneado)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val REQUEST_CODE_CONFIRMACION = 100
    }
}