package br.ufrn.musi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Ponto de entrada do alvo Android — a plataforma-alvo declarada na proposta (§4.2).
 *
 * É tudo o que o Android acrescenta: a interface inteira (`App`) vive em `commonMain` e é
 * a mesma que roda no desktop, onde o ciclo é mais rápido e os testes rodam sem emulador.
 *
 * O Android entrega o deep link (`musi://obra/{id}`) como Intent para esta Activity, e a
 * navegação o resolve pela rota que o declara — ver App.kt.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // desenha sob as barras do sistema; o safeDrawingPadding cuida do resto
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
