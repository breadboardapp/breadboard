package moe.apex.rule34.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import moe.apex.rule34.R

// Set of Material typography styles to start with
val Typography = Typography(
        bodyLarge = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp
        ),
        bodySmall = Typography().bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.2.sp
        ),
        labelSmall = Typography().labelSmall.copy(
                fontFamily = FontFamily(Font(R.font.kumbh)),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                lineHeight = 10.sp
        ),
        labelMedium = Typography().labelMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp
        ),
        titleMedium = Typography().titleMedium.copy(
                fontFamily = FontFamily(Font(R.font.kumbh)),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
        ),
        headlineMedium = Typography().headlineMedium.copy(
                fontFamily = FontFamily(Font(R.font.kumbh)),
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
        ),
        headlineSmall = Typography().headlineSmall.copy(
                fontFamily = FontFamily(Font(R.font.kumbh)),
                fontWeight = FontWeight.Bold
        ),
        titleLarge = Typography().titleLarge.copy(
                fontFamily = FontFamily(Font(R.font.kumbh)),
                fontWeight = FontWeight.Bold,
        )
)

val Typography.searchField: TextStyle
        get() = Typography.bodyLarge.copy(fontSize = 16.sp)

val Typography.prefTitle: TextStyle
        get() = Typography().bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 17.sp,
                        letterSpacing = 0.sp,
                        fontFamily = FontFamily(Font(R.font.kumbh)),
                        fontWeight = FontWeight.Bold
        )