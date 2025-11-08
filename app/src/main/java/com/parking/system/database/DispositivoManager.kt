package com.parking.system.database

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

/**
 * Manager para gestión de dispositivos (Terminales POS)
 */
class DispositivoManager(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val TAG = "DispositivoManager"

    /**
     * Obtiene o registra el ID del dispositivo actual
     */
    suspend fun obtenerIdDispositivo(): String {
        val sharedPref = context.getSharedPreferences("DeviceConfig", Context.MODE_PRIVATE)
        var idDispositivo = sharedPref.getString("id_dispositivo", null)

        if (idDispositivo == null) {
            // Generar ID basado en Android ID
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            idDispositivo = "POS-${androidId.takeLast(8).uppercase()}"

            // Guardar
            sharedPref.edit().putString("id_dispositivo", idDispositivo).apply()

            Log.d(TAG, "Nuevo ID de dispositivo generado: $idDispositivo")
        }

        return idDispositivo
    }

    /**
     * Configura el tipo de dispositivo
     */
    fun configurarTipoDispositivo(tipo: String) {
        val sharedPref = context.getSharedPreferences("DeviceConfig", Context.MODE_PRIVATE)
        sharedPref.edit().putString("tipo_dispositivo", tipo).apply()
        Log.d(TAG, "Tipo de dispositivo configurado: $tipo")
    }

    /**
     * Obtiene el tipo de dispositivo
     */
    fun obtenerTipoDispositivo(): String {
        val sharedPref = context.getSharedPreferences("DeviceConfig", Context.MODE_PRIVATE)
        return sharedPref.getString("tipo_dispositivo", "MIXTO") ?: "MIXTO"
    }

    /**
     * Registra el dispositivo en la base de datos
     */
    suspend fun registrarDispositivo(
        idDispositivo: String,
        nombreDispositivo: String,
        tipoDispositivo: String
    ): DatabaseResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext DatabaseResult.Error("No se pudo conectar a la base de datos")
                }

                val macAddress = obtenerMacAddress()

                val sql = "{CALL sp_RegistrarDispositivo(?, ?, ?, ?)}"
                val callableStatement = connection.prepareCall(sql)
                callableStatement.setString(1, idDispositivo)
                callableStatement.setString(2, nombreDispositivo)
                callableStatement.setString(3, tipoDispositivo)
                callableStatement.setString(4, macAddress)

                val resultSet = callableStatement.executeQuery()

                if (resultSet.next()) {
                    val id = resultSet.getInt("Id")
                    val mensaje = resultSet.getString("Mensaje")

                    resultSet.close()
                    callableStatement.close()

                    if (id > 0) {
                        Log.d(TAG, "✓ Dispositivo registrado: $idDispositivo")
                        DatabaseResult.Success(mensaje)
                    } else {
                        Log.d(TAG, "Dispositivo ya existe: $idDispositivo")
                        DatabaseResult.Success("Dispositivo ya registrado")
                    }
                } else {
                    resultSet.close()
                    callableStatement.close()
                    DatabaseResult.Error("Error al registrar dispositivo")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar dispositivo", e)
                DatabaseResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    private fun obtenerMacAddress(): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            androidId ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}