package com.parking.system

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.parking.system.databinding.ActivityMantenimientoBinding

class MantenimientoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMantenimientoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMantenimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadServerConfig()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mantenimiento"

        // Limpiar errores al escribir
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

        // Botón guardar
        binding.btnGuardar.setOnClickListener {
            guardarConfiguracion()
        }

        // Botón probar conexión
        binding.btnProbarConexion.setOnClickListener {
            probarConexion()
        }

        // Botón restaurar valores por defecto
        binding.btnRestaurar.setOnClickListener {
            mostrarDialogoRestaurar()
        }
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

        // Validaciones
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

        // Validar formato de IP
        if (!isValidIP(serverIp)) {
            binding.tilServerIp.error = "Formato de IP inválido"
            return
        }

        // Validar puerto
        val port = serverPort.toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            binding.tilServerPort.error = "Puerto inválido (1-65535)"
            return
        }

        // Guardar configuración
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

        // Por ahora solo simular la prueba de conexión
        // Después implementaremos la conexión real a MSSQL
        AlertDialog.Builder(this)
            .setTitle("Probar Conexión")
            .setMessage("Servidor: $serverIp:$serverPort\nBase de datos: $databaseName\n\n¿Desea probar la conexión?")
            .setPositiveButton("Probar") { _, _ ->
                Toast.makeText(this, "Función de prueba de conexión\nSe implementará con MSSQL", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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