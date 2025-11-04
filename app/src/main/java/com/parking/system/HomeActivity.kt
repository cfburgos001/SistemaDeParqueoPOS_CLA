package com.parking.system

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.parking.system.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var username: String
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra("USERNAME") ?: "Usuario"
        userType = intent.getStringExtra("USER_TYPE") ?: "OPERADOR"

        setupUI()
        setupBottomNavigation()
    }

    private fun setupUI() {
        binding.tvWelcome.text = "Bienvenido, $username"

        binding.btnIngresoVehiculo.setOnClickListener {
            startActivity(Intent(this, IngresoVehiculoActivity::class.java))
        }

        binding.btnSalidaVehiculo.setOnClickListener {
            startActivity(Intent(this, SalidaVehiculoActivity::class.java))
        }

        binding.btnMantenimiento.setOnClickListener {
            startActivity(Intent(this, MantenimientoActivity::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ingreso -> {
                    startActivity(Intent(this, IngresoVehiculoActivity::class.java))
                    true
                }
                R.id.nav_salida -> {
                    startActivity(Intent(this, SalidaVehiculoActivity::class.java))
                    true
                }
                R.id.nav_mantenimiento -> {
                    startActivity(Intent(this, MantenimientoActivity::class.java))
                    true
                }
                R.id.nav_home -> {
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("ParkingSession", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        showLogoutDialog()
    }
}