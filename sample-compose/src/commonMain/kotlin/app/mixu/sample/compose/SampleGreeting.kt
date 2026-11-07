package app.mixu.sample.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * A tiny Compose component: exists only to prove the Compose convention wires
 * runtime, foundation, material3 and the Compose compiler plugin cleanly.
 */
@Composable
fun SampleGreeting(name: String = "Mixu") {
    Text(text = "Hello, $name!")
}
