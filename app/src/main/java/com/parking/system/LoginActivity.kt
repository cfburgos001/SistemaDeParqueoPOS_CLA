package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.parking.system.database.OperadorRepository
import com.parking.system.database.OperadorResult
import com.parking.system.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var operadorRepository: OperadorRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        operadorRepository = OperadorRepository(this)

        setupUI()
    }

    private fun setupUI() {
        binding.etUsername.addTextChangedListener {
            binding.tilUsername.error = null
        }

        binding.etPassword.addTextChangedListener {
            binding.tilPassword.error = null
        }

        binding.btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.tilUsername.error = "Ingrese el usuario"
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Ingrese la contraseña"
            return
        }

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            when (val result = operadorRepository.validarOperador(username, password)) {
                is OperadorResult.Success -> {
                    val operador = result.operador

                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido, ${operador.getNombreCompleto()}",
                        Toast.LENGTH_SHORT
                    ).show()

                    saveSession(operador)

                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    intent.putExtra("USER_TYPE", operador.tipoUsuario)
                    intent.putExtra("USERNAME", operador.getNombreCompleto())
                    intent.putExtra("ID_OPERADOR", operador.id)
                    startActivity(intent)
                    finish()
                }
                is OperadorResult.Error -> {
                    Toast.makeText(
                        this@LoginActivity,
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tilPassword.error = "Credenciales inválidas"
                    binding.btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun saveSession(operador: com.parking.system.database.Operador) {
        val sharedPref = getSharedPreferences("ParkingSession", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", operador.username)
            putString("nombre_completo", operador.getNombreCompleto())
            putInt("id_operador", operador.id)
            putString("userType", operador.tipoUsuario)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }
}