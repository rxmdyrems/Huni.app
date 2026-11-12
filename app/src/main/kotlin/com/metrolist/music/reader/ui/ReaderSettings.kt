package com.metrolist.music.reader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

/** Reading layouts supported by Huni's local image/archive reader. */
enum class ReaderReadingMode(
    val title: String,
    val isContinuous: Boolean = false,
    val isVerticalPager: Boolean = false,
    val isRightToLeft: Boolean = false,
    val pageGapDp: Int = 0,
) {
    RIGHT_TO_LEFT("Paged · right to left", isRightToLeft = true),
    LEFT_TO_RIGHT("Paged · left to right"),
    VERTICAL("Paged · vertical", isVerticalPager = true),
    LONG_STRIP("Long strip", isContinuous = true),
    LONG_STRIP_WITH_GAPS("Long strip with gaps", isContinuous = true, pageGapDp = 12),
}

enum class ReaderScaleMode(val title: String) {
    FIT_SCREEN("Fit screen"),
    FIT_WIDTH("Fit width"),
    FIT_HEIGHT("Fit height"),
    STRETCH("Stretch"),
    ;

    fun contentScale(): ContentScale =
        when (this) {
            FIT_SCREEN -> ContentScale.Fit
            FIT_WIDTH -> ContentScale.FillWidth
            FIT_HEIGHT -> ContentScale.FillHeight
            STRETCH -> ContentScale.FillBounds
        }
}

enum class ReaderBackground(
    val title: String,
    val color: Color,
) {
    BLACK("Black", Color.Black),
    DARK_GRAY("Dark gray", Color(0xFF202124)),
    WHITE("White", Color.White),
}
