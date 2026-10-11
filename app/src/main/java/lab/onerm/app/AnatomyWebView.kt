package lab.onerm.app

import android.content.Context
import android.webkit.WebView

/** Offline anatomy artwork by Ryan Graves, CC BY 4.0. */
class AnatomyWebView(context: Context, exercise: String, dark: Boolean) : WebView(context) {
    init {
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val (primary, secondary) = MuscleMapView.muscleGroups(exercise)
        val groups = mapOf(
            "chest" to listOf("pectoralis"),
            "delts" to listOf("anterior_deltoid", "lateral_deltoid"),
            "rear delts" to listOf("posterior_deltoid"),
            "triceps" to listOf("triceps_brachii"),
            "biceps" to listOf("biceps_brachii"),
            "quads" to listOf("vastus_", "rectus_femoris"),
            "hamstrings" to listOf("semitendinosus", "semimembranosus", "biceps_femoris"),
            "glutes" to listOf("gluteus_"),
            "adductors" to listOf("adductor_", "gracilis", "pectineus"),
            "lats" to listOf("latissimus_dorsi"),
            "upper back" to listOf("infraspinatus", "trapezius_middle", "trapezius_lower"),
            "traps" to listOf("trapezius_"),
            "erectors" to emptyList(),
            "core" to listOf("rectus_abdominis", "external_oblique")
        )
        fun cssFor(keys: Set<String>, colour: String): String =
            keys.flatMap { groups[it].orEmpty() }.distinct().joinToString("\n") { prefix ->
                """[id^="$prefix"], [id^="$prefix"] path, [id^="$prefix"] * { fill: $colour !important; }"""
            }
        val front = context.assets.open("anatomy/front.svg").bufferedReader().use { it.readText() }
        val back = context.assets.open("anatomy/back.svg").bufferedReader().use { it.readText() }
        val fg = if (dark) "#f0f5f9" else "#263748"
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            html,body{margin:0;padding:0;background:transparent;color:$fg;font-family:sans-serif}
            .panels{display:flex;align-items:flex-start;gap:5px;padding:3px}
            .panel{width:50%;min-width:0;text-align:center}
            .panel svg{width:100%;height:auto;max-height:320px}
            .label{font-size:12px;font-weight:700;letter-spacing:1px;margin-bottom:5px}
            ${cssFor(secondary - primary, "#84b9cf")}
            ${cssFor(primary, "#2d89bf")}
            </style></head><body><div class="panels">
            <div class="panel"><div class="label">FRONT</div>$front</div>
            <div class="panel"><div class="label">BACK</div>$back</div>
            </div></body></html>
        """.trimIndent()
        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
