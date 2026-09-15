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

package com.t8rin.dynamic.theme

enum class PaletteStyle {
    TonalSpot,
    Neutral,
    Vibrant,
    Expressive,
    Rainbow,
    FruitSalad,
    Monochrome,
    Fidelity,
    Content
}

/**
 * 桥接到 MaterialKolor 自带的同名枚举(顺序与这里完全一致),
 * 用于让 MaterialKolor 直接生成配色(动态取色 + 非默认规范/对比度时)。
 */
internal fun PaletteStyle.toMaterialKolorStyle(): com.materialkolor.PaletteStyle =
    com.materialkolor.PaletteStyle.entries[ordinal]