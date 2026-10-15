package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*
import utils.ScoreLine

/** Plain lines of a table cell, separated by line breaks. */
@Composable
fun CellLines(lines: List<String>) {
    lines.forEachIndexed { i, line ->
        if (i > 0) {
            Br { }
        }
        Text(line)
    }
}

/** Decorated lines of a standings cell (utils/StandingsReport.kt): bold or plain segments with their classes, then a plain suffix. */
@Composable
fun ScoreLines(lines: List<ScoreLine>) {
    lines.forEachIndexed { i, line ->
        if (i > 0) {
            Br { }
        }
        line.segments.forEach { segment ->
            if (segment.bold) {
                B(attrs = { if (segment.classes.isNotEmpty()) classes(*segment.classes.toTypedArray()) }) { Text(segment.text) }
            } else {
                Span(attrs = { if (segment.classes.isNotEmpty()) classes(*segment.classes.toTypedArray()) }) { Text(segment.text) }
            }
        }
        if (line.suffix.isNotEmpty()) {
            Text(line.suffix)
        }
    }
}
