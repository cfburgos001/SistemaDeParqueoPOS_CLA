package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.zxing.integration.android.IntentIntegrator
import com.parking.system.databinding.ActivitySalidaVehiculoBinding

class SalidaVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalidaVehiculoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            procesarSalida(placa, null)
        }

        binding.btnEscanearQR.setOnClickListener {
            iniciarEscaneoQR()
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

    private fun iniciarEscaneoQR() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanee el código QR del ticket")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                val qrContent = result.contents
                procesarSalida(null, qrContent)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun procesarSalida(placa: String?, qrId: String?) {
        val mensaje = when {
            placa != null -> "Procesando salida de vehículo con placa: $placa"
            qrId != null -> "Procesando salida con ID: $qrId"
            else -> "Error en el procesamiento"
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // Aquí implementarás:
        // 1. Buscar registro en BD
        // 2. Calcular tiempo
        // 3. Calcular monto
        // 4. Registrar salida
        // 5. Imprimir recibo

        binding.etPlaca.text?.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}