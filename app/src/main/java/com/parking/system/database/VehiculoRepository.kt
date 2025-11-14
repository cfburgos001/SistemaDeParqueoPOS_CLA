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
 * Repositorio para operaciones con la tabla IOT_Vehiculos
 */
class VehiculoRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val TAG = "VehiculoRepository"

    /**
     * Registra la entrada de un vehículo usando dbo.IOT_sp_RegistrarEntrada
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

                val sql = "{CALL dbo.IOT_sp_RegistrarEntrada(?, ?, ?, ?, ?)}"
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

                Log.d(TAG, "✓ Entrada registrada - ID: $id, Placa: ${receiptData.plate}")

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
     * Busca un vehículo por placa en IOT_Vehiculos
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
                    SELECT TOP 1 
                        Id, 
                        Placa, 
                        FechaEntrada, 
                        CodigoBarras, 
                        Estado,
                        ISNULL(bitPaid, 0) as bitPaid,
                        FechaPago,
                        ISNULL(Monto, 0.0) as Monto,
                        ISNULL(strRateKey, 'A') as strRateKey,
                        ISNULL(TiempoEstancia, 0) as TiempoEstancia
                    FROM dbo.IOT_Vehiculos 
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
                        estado = resultSet.getString("Estado"),
                        bitPaid = resultSet.getInt("bitPaid"),
                        fechaPago = resultSet.getTimestamp("FechaPago"),
                        monto = resultSet.getBigDecimal("Monto")?.toDouble() ?: 0.0,
                        strRateKey = resultSet.getString("strRateKey") ?: "A",
                        tiempoEstancia = resultSet.getInt("TiempoEstancia")
                    )

                    resultSet.close()
                    preparedStatement.close()

                    Log.d(TAG, "✓ Vehículo encontrado: ${vehiculo.placa} - bitPaid: ${vehiculo.bitPaid} - Monto: ${vehiculo.monto}")
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
                    SELECT TOP 1 
                        Id, 
                        Placa, 
                        FechaEntrada, 
                        CodigoBarras, 
                        Estado,
                        ISNULL(bitPaid, 0) as bitPaid,
                        FechaPago,
                        ISNULL(Monto, 0.0) as Monto,
                        ISNULL(strRateKey, 'A') as strRateKey,
                        ISNULL(TiempoEstancia, 0) as TiempoEstancia
                    FROM dbo.IOT_Vehiculos 
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
                        estado = resultSet.getString("Estado"),
                        bitPaid = resultSet.getInt("bitPaid"),
                        fechaPago = resultSet.getTimestamp("FechaPago"),
                        monto = resultSet.getBigDecimal("Monto")?.toDouble() ?: 0.0,
                        strRateKey = resultSet.getString("strRateKey") ?: "A",
                        tiempoEstancia = resultSet.getInt("TiempoEstancia")
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
     * Registra la salida de un vehículo usando dbo.IOT_sp_RegistrarSalida
     * Solo actualiza el registro de salida (el monto ya fue registrado por PayStation)
     * Retorna el IdDispositivoSalida
     */
    suspend fun registrarSalida(placa: String, idDispositivo: String): SalidaResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext SalidaResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = "{CALL dbo.IOT_sp_RegistrarSalida(?, ?)}"
                val callableStatement = connection.prepareCall(sql)

                callableStatement.setString(1, placa)
                callableStatement.setString(2, idDispositivo)

                val resultSet = callableStatement.executeQuery()

                var filasAfectadas = 0
                var idDispositivoSalida = ""

                if (resultSet.next()) {
                    filasAfectadas = resultSet.getInt("FilasAfectadas")
                    idDispositivoSalida = resultSet.getString("IdDispositivoSalida") ?: ""
                }

                resultSet.close()
                callableStatement.close()

                if (filasAfectadas > 0) {
                    Log.d(TAG, "✓ Salida registrada - Placa: $placa, IdDispositivoSalida: $idDispositivoSalida")
                    SalidaResult.Success("Salida registrada correctamente", idDispositivoSalida)
                } else {
                    Log.d(TAG, "✗ No se encontró vehículo para salida: $placa")
                    SalidaResult.Error("No se encontró el vehículo en el sistema")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al registrar salida", e)
                SalidaResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar salida", e)
                SalidaResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    /**
     * Calcula el monto a pagar según las reglas de negocio
     */
    suspend fun calcularMonto(placa: String): CalculoMontoResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext CalculoMontoResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = "{CALL dbo.IOT_sp_CalcularMonto(?)}"
                val callableStatement = connection.prepareCall(sql)
                callableStatement.setString(1, placa)

                val resultSet = callableStatement.executeQuery()

                if (resultSet.next()) {
                    val calculoMonto = CalculoMonto(
                        tiempoTotalMinutos = resultSet.getInt("TiempoTotalMinutos"),
                        tiempoCobrableMinutos = resultSet.getInt("TiempoCobrableMinutos"),
                        montoCalculado = resultSet.getDouble("MontoCalculado"),
                        precioPorHora = resultSet.getDouble("PrecioPorHora"),
                        precioMinimo = resultSet.getDouble("PrecioMinimo"),
                        yaPago = resultSet.getBoolean("YaPago"),
                        estadoCobro = resultSet.getString("EstadoCobro")
                    )

                    resultSet.close()
                    callableStatement.close()

                    Log.d(TAG, "✓ Monto calculado: ${calculoMonto.montoCalculado} - Estado: ${calculoMonto.estadoCobro}")
                    CalculoMontoResult.Success(calculoMonto)
                } else {
                    resultSet.close()
                    callableStatement.close()
                    CalculoMontoResult.Error("No se pudo calcular el monto")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al calcular monto", e)
                CalculoMontoResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al calcular monto", e)
                CalculoMontoResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    /**
     * Incrementa el contador de reimpresiones (bitCopy)
     */
    suspend fun incrementarBitCopy(codigoBarras: String): DatabaseResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = dbHelper.getConnection()

                if (connection == null) {
                    return@withContext DatabaseResult.Error("No se pudo conectar a la base de datos")
                }

                val sql = "{CALL dbo.IOT_sp_IncrementarBitCopy(?)}"
                val callableStatement = connection.prepareCall(sql)
                callableStatement.setString(1, codigoBarras)

                val resultSet = callableStatement.executeQuery()

                var nuevoBitCopy = 0
                if (resultSet.next()) {
                    nuevoBitCopy = resultSet.getInt("NuevoBitCopy")
                }

                resultSet.close()
                callableStatement.close()

                Log.d(TAG, "✓ bitCopy incrementado - Código: $codigoBarras, Nuevo valor: $nuevoBitCopy")
                DatabaseResult.Success("Reimpresiones: $nuevoBitCopy")

            } catch (e: SQLException) {
                Log.e(TAG, "Error SQL al incrementar bitCopy", e)
                DatabaseResult.Error("Error SQL: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al incrementar bitCopy", e)
                DatabaseResult.Error("Error: ${e.message}")
            } finally {
                dbHelper.closeConnection(connection)
            }
        }
    }

    /**
     * Obtiene la tarifa actual de IOT_Tarifas
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
                    FROM dbo.IOT_Tarifas 
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

                    TarifaResult.Success(Tarifa(2.0, 1.0))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener tarifa", e)
                TarifaResult.Success(Tarifa(2.0, 1.0))
            } finally {
                dbHelper.closeConnection(connection)
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
    val estado: String,
    val bitPaid: Int = 0,
    val fechaPago: Date? = null,
    val monto: Double = 0.0,
    val strRateKey: String = "A",
    val tiempoEstancia: Int = 0 // En minutos
) {
    fun estaPagado(): Boolean = bitPaid == 1 && monto > 0.0
    fun tieneMontoRegistrado(): Boolean = monto > 0.0
}

/**
 * Clase de datos para cálculo de monto
 */
data class CalculoMonto(
    val tiempoTotalMinutos: Int,
    val tiempoCobrableMinutos: Int,
    val montoCalculado: Double,
    val precioPorHora: Double,
    val precioMinimo: Double,
    val yaPago: Boolean,
    val estadoCobro: String // GRACIA_ENTRADA, GRACIA_SALIDA, DEBE_PAGAR, GRATIS
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
 * Resultado de cálculo de monto
 */
sealed class CalculoMontoResult {
    data class Success(val calculo: CalculoMonto) : CalculoMontoResult()
    data class Error(val message: String) : CalculoMontoResult()
}

/**
 * Resultado de salida de vehículo (incluye IdDispositivoSalida)
 */
sealed class SalidaResult {
    data class Success(val message: String, val idDispositivoSalida: String) : SalidaResult()
    data class Error(val message: String) : SalidaResult()
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