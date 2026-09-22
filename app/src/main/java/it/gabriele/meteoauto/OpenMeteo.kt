package it.gabriele.meteoauto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class Attuale(
    val temp: Double, val percepita: Double, val umidita: Int,
    val vento: Double, val raffiche: Double, val direzione: Int, val pressione: Double
)

data class Giorno(val data: String, val tMin: Double, val tMax: Double, val ventoMax: Double, val codice: Int)

data class Meteo(val attuale: Attuale, val giorni: List<Giorno>, val onde: Double?)

object OpenMeteo {
    private const val LAT = 43.87
    private const val LON = 10.25

    private const val FORECAST =
        "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
        "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,surface_pressure" +
        "&daily=temperature_2m_min,temperature_2m_max,wind_speed_10m_max,weather_code" +
        "&timezone=Europe%2FRome&forecast_days=4"

    private const val MARINE =
        "https://marine-api.open-meteo.com/v1/marine?latitude=$LAT&longitude=$LON&current=wave_height"

    suspend fun carica(): Meteo = withContext(Dispatchers.IO) {
        val f = JSONObject(URL(FORECAST).readText())
        val c = f.getJSONObject("current")
        val attuale = Attuale(
            c.getDouble("temperature_2m"), c.getDouble("apparent_temperature"),
            c.getInt("relative_humidity_2m"), c.getDouble("wind_speed_10m"),
            c.getDouble("wind_gusts_10m"), c.getInt("wind_direction_10m"),
            c.getDouble("surface_pressure")
        )
        val d = f.getJSONObject("daily")
        val giorni = (0 until d.getJSONArray("time").length()).map { i ->
            Giorno(
                d.getJSONArray("time").getString(i),
                d.getJSONArray("temperature_2m_min").getDouble(i),
                d.getJSONArray("temperature_2m_max").getDouble(i),
                d.getJSONArray("wind_speed_10m_max").getDouble(i),
                d.getJSONArray("weather_code").getInt(i)
            )
        }
        val onde = runCatching {
            JSONObject(URL(MARINE).readText()).getJSONObject("current").getDouble("wave_height")
        }.getOrNull()
        Meteo(attuale, giorni, onde)
    }

    fun nomeVento(gradi: Int): String = when (((gradi + 22) % 360) / 45) {
        0 -> "Tramontana"; 1 -> "Grecale"; 2 -> "Levante"; 3 -> "Scirocco"
        4 -> "Ostro"; 5 -> "Libeccio"; 6 -> "Ponente"; else -> "Maestrale"
    }

    fun descrizione(code: Int): String = when (code) {
        0 -> "Sereno"; 1, 2 -> "Poco nuvoloso"; 3 -> "Coperto"
        45, 48 -> "Nebbia"; in 51..57 -> "Pioviggine"; in 61..67 -> "Pioggia"
        in 71..77 -> "Neve"; in 80..82 -> "Rovesci"; in 95..99 -> "Temporale"
        else -> "—"
    }
}
