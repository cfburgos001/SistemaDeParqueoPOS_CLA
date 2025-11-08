package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.parking.system.database.ConnectionResult
import com.parking.system.database.DatabaseHelper
import com.parking.system.database.DatabaseResult
import com.parking.system.database.DispositivoManager
import com.parking.system.databinding.ActivityMantenimientoBinding
import kotlinx.coroutines.launch

class MantenimientoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMantenimientoBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dispositivoManager: DispositivoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMantenimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        dispositivoManager = DispositivoManager(this)

        setupUI()
        loadServerConfig()
        loadDispositivoInfo()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mantenimiento"

        // Botón Gestión de Usuarios
        binding.btnGestionUsuarios.setOnClickListener {
            val intent = Intent(this, GestionUsuariosActivity::class.java)
            startActivity(intent)
        }

        // Botón Configurar Dispositivo
        binding.btnConfigDispositivo.setOnClickListener {
            mostrarDialogoConfigDispositivo()
        }

        // Listeners para campos del servidor
        binding.etServerIp.addTextChangedListener {
            binding.tilServerIp.error = null
        }

        binding.etServerPort.addTextChangedListener {
            binding.tilServerPort.error = null
        }

        binding.etDatabaseName.addTextChangedListener {
            binding.tilDatabaseName.error = null
        }

        binding.etDbUsername.addTextChangedListener {
            binding.tilDbUsername.error = null
        }

        binding.etDbPassword.addTextChangedListener {
            binding.tilDbPassword.error = null
        }

        binding.btnGuardar.setOnClickListener {
            guardarConfiguracion()
        }

        binding.btnProbarConexion.setOnClickListener {
            probarConexion()
        }

        binding.btnRestaurar.setOnClickListener {
            mostrarDialogoRestaurar()
        }
    }

    private fun loadDispositivoInfo() {
        lifecycleScope.launch {
            val idDispositivo = dispositivoManager.obtenerIdDispositivo()
            val tipoDispositivo = dispositivoManager.obtenerTipoDispositivo()

            binding.tvIdDispositivo.text = idDispositivo
            binding.tvTipoDispositivo.text = tipoDispositivo
        }
    }

    private fun mostrarDialogoConfigDispositivo() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_config_dispositivo, null)

        val tvIdDispositivo = dialogView.findViewById<TextView>(R.id.tvIdDispositivo)
        val etNombreDispositivo = dialogView.findViewById<TextInputEditText>(R.id.etNombreDispositivo)
        val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinnerTipoDispositivo)

        lifecycleScope.launch {
            val idDispositivo = dispositivoManager.obtenerIdDispositivo()
            tvIdDispositivo.text = idDispositivo
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Configurar Dispositivo")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnGuardar.setOnClickListener {
                val nombreDispositivo = etNombreDispositivo.text.toString().trim()
                val tipoDispositivo = spinnerTipo.selectedItem.toString()

                if (nombreDispositivo.isEmpty()) {
                    Toast.makeText(this, "Ingrese el nombre del dispositivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Guardar configuración
                dispositivoManager.configurarTipoDispositivo(tipoDispositivo)

                // Registrar en BD
                lifecycleScope.launch {
                    val idDispositivo = dispositivoManager.obtenerIdDispositivo()
                    val result = dispositivoManager.registrarDispositivo(
                        idDispositivo,
                        nombreDispositivo,
                        tipoDispositivo
                    )

                    when (result) {
                        is DatabaseResult.Success -> {
                            Toast.makeText(
                                this@MantenimientoActivity,
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            loadDispositivoInfo()
                        }
                        is DatabaseResult.Error -> {
                            Toast.makeText(
                                this@MantenimientoActivity,
                                result.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun loadServerConfig() {
        val sharedPref = getSharedPreferences("ServerConfig", MODE_PRIVATE)

        binding.etServerIp.setText(sharedPref.getString("server_ip", "192.168.1.100"))
        binding.etServerPort.setText(sharedPref.getString("server_port", "1433"))
        binding.etDatabaseName.setText(sharedPref.getString("database_name", "ParkingDB"))
        binding.etDbUsername.setText(sharedPref.getString("db_username", "sa"))
        binding.etDbPassword.setText(sharedPref.getString("db_password", ""))
    }

    private fun guardarConfiguracion() {
        val serverIp = binding.etServerIp.text.toString().trim()
        val serverPort = binding.etServerPort.text.toString().trim()
        val databaseName = binding.etDatabaseName.text.toString().trim()
        val dbUsername = binding.etDbUsername.text.toString().trim()
        val dbPassword = binding.etDbPassword.text.toString().trim()

        if (serverIp.isEmpty()) {
            binding.tilServerIp.error = "Ingrese la IP del servidor"
            return
        }

        if (serverPort.isEmpty()) {
            binding.tilServerPort.error = "Ingrese el puerto"
            return
        }

        if (databaseName.isEmpty()) {
            binding.tilDatabaseName.error = "Ingrese el nombre de la base de datos"
            return
        }

        if (dbUsername.isEmpty()) {
            binding.tilDbUsername.error = "Ingrese el usuario de la BD"
            return
        }

        if (!isValidIP(serverIp)) {
            binding.tilServerIp.error = "Formato de IP inválido"
            return
        }

        val port = serverPort.toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            binding.tilServerPort.error = "Puerto inválido (1-65535)"
            return
        }

        val sharedPref = getSharedPreferences("ServerConfig", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("server_ip", serverIp)
            putString("server_port", serverPort)
            putString("database_name", databaseName)
            putString("db_username", dbUsername)
            putString("db_password", dbPassword)
            apply()
        }

        Toast.makeText(this, "✓ Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun probarConexion() {
        val serverIp = binding.etServerIp.text.toString().trim()
        val serverPort = binding.etServerPort.text.toString().trim()
        val databaseName = binding.etDatabaseName.text.toString().trim()

        if (serverIp.isEmpty() || serverPort.isEmpty() || databaseName.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar temporalmente la configuración
        guardarConfiguracion()

        // Mostrar diálogo de progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Probando Conexión")
            .setMessage("Conectando a $serverIp:$serverPort...\nPor favor espere.")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Probar conexión en background
        lifecycleScope.launch {
            val result = databaseHelper.testConnection()

            progressDialog.dismiss()

            when (result) {
                is ConnectionResult.Success -> {
                    AlertDialog.Builder(this@MantenimientoActivity)
                        .setTitle("✓ Conexión Exitosa")
                        .setMessage(result.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
                is ConnectionResult.Error -> {
                    AlertDialog.Builder(this@MantenimientoActivity)
                        .setTitle("✗ Error de Conexión")
                        .setMessage(result.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun mostrarDialogoRestaurar() {
        AlertDialog.Builder(this)
            .setTitle("Restaurar Valores")
            .setMessage("¿Desea restaurar los valores por defecto de la configuración del servidor?")
            .setPositiveButton("Restaurar") { _, _ ->
                restaurarValoresPorDefecto()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun restaurarValoresPorDefecto() {
        binding.etServerIp.setText("192.168.1.100")
        binding.etServerPort.setText("1433")
        binding.etDatabaseName.setText("ParkingDB")
        binding.etDbUsername.setText("sa")
        binding.etDbPassword.setText("")

        Toast.makeText(this, "Valores restaurados (no guardados)", Toast.LENGTH_SHORT).show()
    }

    private fun isValidIP(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false

        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}