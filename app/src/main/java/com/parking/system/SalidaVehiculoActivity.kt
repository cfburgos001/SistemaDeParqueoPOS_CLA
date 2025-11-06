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

    private fun iniciarEscaner() {
        val integrator = IntentIntegrator(this)

        // IMPORTANTE: Aceptar QR y CODE128
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
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                val codigoEscaneado = result.contents
                val tipoFormato = result.formatName

                Toast.makeText(
                    this,
                    "Escaneado: $tipoFormato\nCódigo: $codigoEscaneado",
                    Toast.LENGTH_SHORT
                ).show()

                // Procesar la salida con el código escaneado
                procesarSalida(codigoEscaneado, tipoFormato)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun procesarSalida(codigo: String, tipoFormato: String?) {
        // Aquí procesarías la salida del vehículo
        val mensaje = buildString {
            append("Procesando salida:\n")
            append("Código: $codigo\n")
            if (tipoFormato != null) {
                append("Tipo: $tipoFormato\n")
            }
            append("\nBuscando en sistema...")
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // Aquí irían las siguientes acciones:
        // 1. Buscar el registro en la base de datos por placa
        // 2. Calcular el tiempo de estancia
        // 3. Calcular el monto a cobrar
        // 4. Registrar la salida
        // 5. Imprimir recibo de salida

        binding.etPlaca.text?.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}