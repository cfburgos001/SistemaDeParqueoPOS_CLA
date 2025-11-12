package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.parking.system.database.DispositivoManager
import com.parking.system.database.VehiculoDB
import com.parking.system.database.VehiculoRepository
import com.parking.system.database.VehiculoResult
import com.parking.system.databinding.ActivitySalidaVehiculoBinding
import kotlinx.coroutines.launch
import java.util.Date

class SalidaVehiculoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalidaVehiculoBinding
    private lateinit var vehiculoRepository: VehiculoRepository
    private lateinit var dispositivoManager: DispositivoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehiculoRepository = VehiculoRepository(this)
        dispositivoManager = DispositivoManager(this)

        verificarTipoDispositivo()
        setupUI()
    }

    private fun verificarTipoDispositivo() {
        if (!dispositivoManager.puedeRegistrarSalida()) {
            Toast.makeText(
                this,
                "⚠ Este dispositivo está configurado como ENTRADA\nNo puede registrar salidas",
                Toast.LENGTH_LONG
            ).show()

            binding.btnRegistrarPorPlaca.isEnabled = false
            binding.btnEscanearQR.isEnabled = false
            binding.etPlaca.isEnabled = false
        }
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
                    verificarPagoYMostrarConfirmacion(result.vehiculo)
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
                    verificarPagoYMostrarConfirmacion(result.vehiculo)
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

    private fun verificarPagoYMostrarConfirmacion(vehiculo: VehiculoDB) {
        // LOG DE DEPURACIÓN
        android.util.Log.d("SalidaVehiculo", "=== VERIFICANDO PAGO ===")
        android.util.Log.d("SalidaVehiculo", "Placa: ${vehiculo.placa}")
        android.util.Log.d("SalidaVehiculo", "bitPaid: ${vehiculo.bitPaid}")
        android.util.Log.d("SalidaVehiculo", "estaPagado(): ${vehiculo.estaPagado()}")
        android.util.Log.d("SalidaVehiculo", "Monto: ${vehiculo.monto}")

        if (!vehiculo.estaPagado()) {
            android.util.Log.d("SalidaVehiculo", "NO HA PAGADO - Mostrando alerta")
            mostrarAlertaNoPagado(vehiculo)
            return
        }

        android.util.Log.d("SalidaVehiculo", "YA PAGÓ - Permitiendo salida")
        mostrarConfirmacionSalida(vehiculo)
    }

    private fun mostrarAlertaNoPagado(vehiculo: VehiculoDB) {
        val mensaje = if (vehiculo.monto > 0) {
            // Tiene monto pero bitPaid = 0
            """
            Este vehículo NO ha pagado el ticket.
            
            📋 Placa: ${vehiculo.placa}
            💵 Monto a pagar: ${'

                private fun mostrarConfirmacionSalida(vehiculo: VehiculoDB) {
                    lifecycleScope.launch {
                        val ahora = Date()
                        val tiempoMinutos = ((ahora.time - vehiculo.fechaEntrada.time) / 60000).toInt()

                        val intent = Intent(this@SalidaVehiculoActivity, SalidaConfirmacionActivity::class.java)
                        intent.putExtra("VEHICULO_ID", vehiculo.id)
                        intent.putExtra("VEHICULO_PLACA", vehiculo.placa)
                        intent.putExtra("VEHICULO_FECHA", vehiculo.fechaEntrada.time)
                        intent.putExtra("VEHICULO_CODIGO", vehiculo.codigoBarras)
                        intent.putExtra("TIEMPO_MINUTOS", tiempoMinutos)
                        intent.putExtra("MONTO", vehiculo.monto)
                        intent.putExtra("FECHA_PAGO", vehiculo.fechaPago?.time ?: 0L)
                        intent.putExtra("BIT_PAID", vehiculo.bitPaid)
                        startActivityForResult(intent, REQUEST_CODE_CONFIRMACION)

                        binding.btnRegistrarPorPlaca.isEnabled = true
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

                            if (codigoEscaneado.startsWith("PK-", ignoreCase = true)) {
                                buscarVehiculoPorCodigo(codigoEscaneado)
                            } else {
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
}${String.format("%.2f", vehiculo.monto)}
            📊 Estado de pago: ${if (vehiculo.bitPaid == 1) "Pagado (bitPaid=1)" else "Pendiente (bitPaid=0)"}
            
            ⚠️ El cliente debe dirigirse a la PayStation para realizar el pago antes de salir.
            """.trimIndent()
        } else {
            // No tiene monto registrado
            """
            Este vehículo NO tiene monto registrado.
            
            📋 Placa: ${vehiculo.placa}
            💵 Monto: ${'

                private fun mostrarConfirmacionSalida(vehiculo: VehiculoDB) {
                    lifecycleScope.launch {
                        val ahora = Date()
                        val tiempoMinutos = ((ahora.time - vehiculo.fechaEntrada.time) / 60000).toInt()

                        val intent = Intent(this@SalidaVehiculoActivity, SalidaConfirmacionActivity::class.java)
                        intent.putExtra("VEHICULO_ID", vehiculo.id)
                        intent.putExtra("VEHICULO_PLACA", vehiculo.placa)
                        intent.putExtra("VEHICULO_FECHA", vehiculo.fechaEntrada.time)
                        intent.putExtra("VEHICULO_CODIGO", vehiculo.codigoBarras)
                        intent.putExtra("TIEMPO_MINUTOS", tiempoMinutos)
                        intent.putExtra("MONTO", vehiculo.monto)
                        intent.putExtra("FECHA_PAGO", vehiculo.fechaPago?.time ?: 0L)
                        intent.putExtra("BIT_PAID", vehiculo.bitPaid)
                        startActivityForResult(intent, REQUEST_CODE_CONFIRMACION)

                        binding.btnRegistrarPorPlaca.isEnabled = true
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

                            if (codigoEscaneado.startsWith("PK-", ignoreCase = true)) {
                                buscarVehiculoPorCodigo(codigoEscaneado)
                            } else {
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
}0.00
            📊 Estado de pago: ${if (vehiculo.bitPaid == 1) "Pagado (bitPaid=1)" else "Pendiente (bitPaid=0)"}
            
            ⚠️ Debe registrarse el monto en la PayStation antes de permitir la salida.
            
            🔍 DEBUG INFO:
            - ID: ${vehiculo.id}
            - Código: ${vehiculo.codigoBarras}
            - bitPaid: ${vehiculo.bitPaid}
            - Monto: ${vehiculo.monto}
            """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("⚠️ Pago Pendiente")
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { _, _ ->
                binding.etPlaca.text?.clear()
                binding.btnRegistrarPorPlaca.isEnabled = true
            }
            .setCancelable(false)
            .show()
    }

    private fun mostrarConfirmacionSalida(vehiculo: VehiculoDB) {
        lifecycleScope.launch {
            val ahora = Date()
            val tiempoMinutos = ((ahora.time - vehiculo.fechaEntrada.time) / 60000).toInt()

            val intent = Intent(this@SalidaVehiculoActivity, SalidaConfirmacionActivity::class.java)
            intent.putExtra("VEHICULO_ID", vehiculo.id)
            intent.putExtra("VEHICULO_PLACA", vehiculo.placa)
            intent.putExtra("VEHICULO_FECHA", vehiculo.fechaEntrada.time)
            intent.putExtra("VEHICULO_CODIGO", vehiculo.codigoBarras)
            intent.putExtra("TIEMPO_MINUTOS", tiempoMinutos)
            intent.putExtra("MONTO", vehiculo.monto)
            intent.putExtra("FECHA_PAGO", vehiculo.fechaPago?.time ?: 0L)
            intent.putExtra("BIT_PAID", vehiculo.bitPaid)
            startActivityForResult(intent, REQUEST_CODE_CONFIRMACION)

            binding.btnRegistrarPorPlaca.isEnabled = true
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

                if (codigoEscaneado.startsWith("PK-", ignoreCase = true)) {
                    buscarVehiculoPorCodigo(codigoEscaneado)
                } else {
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