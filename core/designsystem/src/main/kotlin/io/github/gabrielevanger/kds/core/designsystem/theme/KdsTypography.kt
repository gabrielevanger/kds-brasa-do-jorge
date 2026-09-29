package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Tipografia dimensionada para leitura a cerca de 2 metros, no tablet sobre a bancada. */
@Immutable
data class KdsTypography(
    val orderNumber: TextStyle,
    val origin: TextStyle,
    /** Dígitos tabulares: a largura não muda de "09:59" para "10:00" e o texto não "pula". */
    val timer: TextStyle,
    val columnTitle: TextStyle,
    val itemName: TextStyle,
    val modifier: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val action: TextStyle,
)

val KitchenTypography = KdsTypography(
    orderNumber = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    origin = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
    timer = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_NUMBERS),
    columnTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    itemName = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    modifier = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    body = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
    label = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    action = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
)

/** Recurso OpenType de dígitos com largura fixa. */
private const val TABULAR_NUMBERS = "tnum"
