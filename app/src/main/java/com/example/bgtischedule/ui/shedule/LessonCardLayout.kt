package com.example.bgtischedule.ui.shedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
fun LessonCardLayout(
    graphWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    graph: @Composable () -> Unit
) {
    val density = LocalDensity.current


    Layout(
        modifier = modifier,
        content = {
            content()
            graph()
        }
    ) { measurables, constraints ->

        check(measurables.size == 2)

        val graphWidthPx = with(density) {
            graphWidth.roundToPx()
        }

        // Измеряем контент
        val contentPlaceable = measurables[0].measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = constraints.maxWidth - graphWidthPx
            )
        )

        // Измеряем график с высотой контента
        val graphPlaceable = measurables[1].measure(
            Constraints.fixed(
                graphWidthPx,
                contentPlaceable.height
            )
        )

        layout(
            width = constraints.maxWidth,
            height = contentPlaceable.height
        ) {

            contentPlaceable.placeRelative(
                x = 0,
                y = 0
            )

            graphPlaceable.placeRelative(
                x = constraints.maxWidth - graphWidthPx,
                y = 0
            )
        }
    }
}