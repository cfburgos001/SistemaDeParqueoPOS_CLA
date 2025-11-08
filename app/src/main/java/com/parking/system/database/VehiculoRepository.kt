package com.parking.system.database

import android.content.Context
import android.util.Log
import com.parking.system.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.util.Date

/**
 * Repositorio para operaciones con la tabla Vehiculos
 */
class VehiculoRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val TAG = "VehiculoRepository"

    /**
     * Registra la entrada de un vehículo en la base de datos
     */
    /**
     * Registra la entrada de un vehículo en la base de datos
     */
    suspend fun registrarEntrada(
        receiptData: ReceiptData,
        usuario: String,
        idOperador: Int,
        idDispositivo: String
    ): DatabaseResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext DatabaseResult.Error("No se pudo conectar a la base de datos")
                }

                // Usar procedimiento almacenado actualizado
                val sql = "{CALL sp_RegistrarEntrada(?, ?, ?, ?, ?)}"
                val callableStatement = connection.prepareCall(sql)

                callableStatement.setString(1, receiptData.plate)
                callableStatement.setString(2, receiptData.uniqueId)
                callableStatement.setString(3, usuario)
                callableStatement.setInt(4, idOperador)
                callableStatement.setString(5, idDispositivo)

                val resultSet = callableStatement.executeQuery()

                var id = 0
                if (resultSet.next()) {
                    id = resultSet.getInt("Id")
                }

                resultSet.close()
                callableStatement.close()

                Log.d(TAG, "✓ Entrada registrada - ID: $id, Placa: ${receiptData.plate}, Operador: $idOperador, Dispositivo: $idDispositivo")

                DatabaseResult.Success("Entrada registrada correctamente. ID: $id")

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al registrar entrada", e)
                DatabaseResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar entrada", e)
                DatabaseResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    /**
     * Busca un vehículo por placa que esté dentro del parqueo
     */
    suspend fun buscarVehiculoPorPlaca(placa: String): VehiculoResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext VehiculoResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = """
                    SELECT TOP 1 Id, Placa, FechaEntrada, CodigoBarras, Estado
                    FROM Vehiculos 
                    WHERE Placa = ? AND Estado = 'DENTRO'
                    ORDER BY FechaEntrada DESC
                """

                val preparedStatement = connection.prepareStatement(sql)
                preparedStatement.setString(1, placa)

                val resultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val vehiculo = VehiculoDB(
                        id = resultSet.getInt("Id"),
                        placa = resultSet.getString("Placa"),
                        fechaEntrada = resultSet.getTimestamp("FechaEntrada"),
                        codigoBarras = resultSet.getString("CodigoBarras"),
                        estado = resultSet.getString("Estado")
                    )

                    resultSet.close()
                    preparedStatement.close()

                    Log.d(TAG, "✓ Vehículo encontrado: ${vehiculo.placa}")
                    VehiculoResult.Found(vehiculo)
                } else {
                    resultSet.close()
                    preparedStatement.close()

                    Log.d(TAG, "✗ Vehículo no encontrado: $placa")
                    VehiculoResult.NotFound
                }

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al buscar vehículo", e)
                VehiculoResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al buscar vehículo", e)
                VehiculoResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    /**
     * Busca un vehículo por código de barras
     */
    suspend fun buscarVehiculoPorCodigo(codigo: String): VehiculoResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext VehiculoResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = """
                    SELECT TOP 1 Id, Placa, FechaEntrada, CodigoBarras, Estado
                    FROM Vehiculos 
                    WHERE CodigoBarras = ? AND Estado = 'DENTRO'
                    ORDER BY FechaEntrada DESC
                """

                val preparedStatement = connection.prepareStatement(sql)
                preparedStatement.setString(1, codigo)

                val resultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val vehiculo = VehiculoDB(
                        id = resultSet.getInt("Id"),
                        placa = resultSet.getString("Placa"),
                        fechaEntrada = resultSet.getTimestamp("FechaEntrada"),
                        codigoBarras = resultSet.getString("CodigoBarras"),
                        estado = resultSet.getString("Estado")
                    )

                    resultSet.close()
                    preparedStatement.close()

                    Log.d(TAG, "✓ Vehículo encontrado por código: ${vehiculo.placa}")
                    VehiculoResult.Found(vehiculo)
                } else {
                    resultSet.close()
                    preparedStatement.close()

                    Log.d(TAG, "✗ Vehículo no encontrado con código: $codigo")
                    VehiculoResult.NotFound
                }

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al buscar por código", e)
                VehiculoResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al buscar por código", e)
                VehiculoResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }


    /**
     * Registra la salida de un vehículo
     */
    suspend fun registrarSalida(placa: String, monto: Double, idDispositivo: String): DatabaseResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext DatabaseResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = "{CALL sp_RegistrarSalida(?, ?, ?)}"
                val callableStatement = connection.prepareCall(sql)

                callableStatement.setString(1, placa)
                callableStatement.setDouble(2, monto)
                callableStatement.setString(3, idDispositivo)

                val resultSet = callableStatement.executeQuery()

                var filasAfectadas = 0
                if (resultSet.next()) {
                    filasAfectadas = resultSet.getInt("FilasAfectadas")
                }

                resultSet.close()
                callableStatement.close()

                if (filasAfectadas > 0) {
                    Log.d(TAG, "✓ Salida registrada - Placa: $placa, Dispositivo: $idDispositivo")
                    DatabaseResult.Success("Salida registrada correctamente")
                } else {
                    Log.d(TAG, "✗ No se encontró vehículo para salida: $placa")
                    DatabaseResult.Error("No se encontró el vehículo en el sistema")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al registrar salida", e)
                DatabaseResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar salida", e)
                DatabaseResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }
    /**
     * Obtiene la tarifa actual
     */
    suspend fun obtenerTarifa(): TarifaResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext TarifaResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = """
                    SELECT TOP 1 PrecioPorHora, PrecioMinimo
                    FROM Tarifas 
                    WHERE Activa = 1
                """

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(sql)

                if (resultSet.next()) {
                    val tarifa = Tarifa(
                        precioPorHora = resultSet.getDouble("PrecioPorHora"),
                        precioMinimo = resultSet.getDouble("PrecioMinimo")
                    )

                    resultSet.close()
                    statement.close()

                    TarifaResult.Success(tarifa)
                } else {
                    resultSet.close()
                    statement.close()

                    // Tarifa por defecto si no hay en BD
                    TarifaResult.Success(Tarifa(2.0, 1.0))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener tarifa", e)
                // Tarifa por defecto en caso de error
                TarifaResult.Success(Tarifa(2.0, 1.0))
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }
    /**
     * Calcula el tiempo de estancia y el monto a pagar
     */
    suspend fun calcularMonto(vehiculo: VehiculoDB): CalculoResult {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener tarifa
                val tarifaResult = obtenerTarifa()
                val tarifa = when (tarifaResult) {
                    is TarifaResult.Success -> tarifaResult.tarifa
                    is TarifaResult.Error -> Tarifa(2.0, 1.0) // Tarifa por defecto
                }

                // Calcular tiempo en minutos
                val ahora = Date()
                val tiempoMinutos = ((ahora.time - vehiculo.fechaEntrada.time) / 60000).toInt()
                val tiempoHoras = tiempoMinutos / 60.0

                // Calcular monto
                var monto = tiempoHoras * tarifa.precioPorHora

                // Aplicar precio mínimo
                if (monto < tarifa.precioMinimo) {
                    monto = tarifa.precioMinimo
                }

                // Redondear a 2 decimales
                monto = (monto * 100).toInt() / 100.0

                Log.d(TAG, "Tiempo: $tiempoMinutos min (${String.format("%.2f", tiempoHoras)} hrs), Monto: $$monto")

                CalculoResult.Success(
                    tiempoMinutos = tiempoMinutos,
                    monto = monto
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error al calcular monto", e)
                CalculoResult.Error("Error en cálculo: ${e.message}")
            }
        }
    }
}







/**
 * Clase de datos para vehículo en BD
 */
data class VehiculoDB(
    val id: Int,
    val placa: String,
    val fechaEntrada: Date,
    val codigoBarras: String,
    val estado: String
)

/**
 * Clase de datos para tarifa
 */
data class Tarifa(
    val precioPorHora: Double,
    val precioMinimo: Double
)

/**
 * Resultado de operaciones en BD
 */
sealed class DatabaseResult {
    data class Success(val message: String) : DatabaseResult()
    data class Error(val message: String) : DatabaseResult()
}

/**
 * Resultado de búsqueda de vehículo
 */
sealed class VehiculoResult {
    data class Found(val vehiculo: VehiculoDB) : VehiculoResult()
    object NotFound : VehiculoResult()
    data class Error(val message: String) : VehiculoResult()
}

/**
 * Resultado de tarifa
 */
sealed class TarifaResult {
    data class Success(val tarifa: Tarifa) : TarifaResult()
    data class Error(val message: String) : TarifaResult()
}



/**
 * Resultado del cálculo de monto
 */
sealed class CalculoResult {
    data class Success(
        val tiempoMinutos: Int,
        val monto: Double
    ) : CalculoResult()
    data class Error(val message: String) : CalculoResult()
}