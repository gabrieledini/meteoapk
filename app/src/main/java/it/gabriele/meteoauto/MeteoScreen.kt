package it.gabriele.meteoauto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Schermata principale: PaneTemplate con le condizioni attuali
 * e un pulsante per la lista dei prossimi giorni.
 */
class MeteoScreen(carContext: CarContext) : Screen(carContext) {

    private var meteo: Meteo? = null
    private var errore: String? = null

    init { aggiorna() }

    private fun aggiorna() {
        meteo = null; errore = null
        invalidate()
        lifecycleScope.launch {
            runCatching { OpenMeteo.carica() }
                .onSuccess { meteo = it }
                .onFailure { errore = it.message ?: "Errore rete" }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val header = Header.Builder()
            .setTitle("Meteo Viareggio")
            .setStartHeaderAction(Action.APP_ICON)
            .addEndHeaderAction(
                Action.Builder().setTitle("Aggiorna").setOnClickListener { aggiorna() }.build()
            )
            .build()

        val m = meteo
        val pane = Pane.Builder()

        when {
            errore != null -> pane.addRow(Row.Builder().setTitle("Errore").addText(errore!!).build())
            m == null -> pane.setLoading(true)
            else -> {
                val a = m.attuale
                pane.addRow(riga("Temperatura", "${a.temp.toInt()}°C · percepita ${a.percepita.toInt()}°C"))
                pane.addRow(riga("Vento", "${a.vento.toInt()} km/h ${OpenMeteo.nomeVento(a.direzione)} · raffiche ${a.raffiche.toInt()}"))
                pane.addRow(riga("Umidità · Pressione", "${a.umidita}% · ${a.pressione.toInt()} hPa"))
                m.onde?.let { pane.addRow(riga("Onde", String.format("%.1f m", it))) }
                pane.addAction(
                    Action.Builder().setTitle("Prossimi giorni")
                        .setOnClickListener { screenManager.push(PrevisioniScreen(carContext, m.giorni)) }
                        .build()
                )
            }
        }

        return PaneTemplate.Builder(pane.build()).setHeader(header).build()
    }

    private fun riga(titolo: String, testo: String) =
        Row.Builder().setTitle(titolo).addText(testo).build()
}

class PrevisioniScreen(carContext: CarContext, private val giorni: List<Giorno>) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val lista = ItemList.Builder()
        giorni.forEach { g ->
            lista.addItem(
                Row.Builder()
                    .setTitle(g.data.substring(5).replace("-", "/"))
                    .addText("${OpenMeteo.descrizione(g.codice)} · ${g.tMin.toInt()}/${g.tMax.toInt()}°C · vento max ${g.ventoMax.toInt()} km/h")
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setSingleList(lista.build())
            .setHeader(Header.Builder().setTitle("3-4 giorni").setStartHeaderAction(Action.BACK).build())
            .build()
    }
}
