package com.parking.system

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.parking.system.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Usuarios predefinidos
    private val users = mapOf(
        "Admin" to User("Admin", "Admin", UserType.ADMINISTRADOR),
        "User" to User("User", "User", UserType.OPERADOR),
        "Caja" to User("Caja", "Caja", UserType.CAJA)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        val user = users[username]

        if (user != null && user.password == password) {
            Toast.makeText(this, "Bienvenido, ${user.username}", Toast.LENGTH_SHORT).show()
            saveSession(user)

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_TYPE", user.type.name)
            intent.putExtra("USERNAME", user.username)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            binding.tilPassword.error = "Credenciales inválidas"
        }
    }

    private fun saveSession(user: User) {
        val sharedPref = getSharedPreferences("ParkingSession", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", user.username)
            putString("userType", user.type.name)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }
}

data class User(
    val username: String,
    val password: String,
    val type: UserType
)

enum class UserType {
    ADMINISTRADOR,
    OPERADOR,
    CAJA
}