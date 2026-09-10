/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.color_tools.presentation.components

import androidx.compose.ui.graphics.Color
import com.t8rin.imagetoolbox.core.ui.theme.blend

internal val ColorToolsDefaultColors by lazy {
    listOf(
        Color(0xFF6B8E9F),
        Color(0xFF22B8CF),
        Color(0xFF52C41A),
        Color(0xFF9CCC65),
        Color(0xFFFFE066),
        Color(0xFFFF9A00),
        Color(0xFFFF6B4A),
        Color(0xFFF8130D),
        Color(0xFFFC50A6),
        Color(0xFF7B2BEC),
        Color(0xFF005FFF),
        Color(0xFF59CBF0),
        Color(0xFF07DDC3),
        Color.White,
        Color(0xFF333333),
        Color.Black,
    )
}

fun Color.mixWith(
    color: Color,
    variations: Int,
    maxPercent: Float = 1f
): List<Color> = List(variations) {
    val percent = it / ((variations + (1f - maxPercent) * 10) - 1)
    this.blend(color, percent)
}