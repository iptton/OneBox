package com.halilibo.richtext.ui.string

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.halilibo.richtext.ui.DefaultCodeBlockBackgroundColor
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.Text
import com.halilibo.richtext.ui.currentContentColor
import com.halilibo.richtext.ui.currentRichTextStyle
import com.halilibo.richtext.ui.string.RichTextString.Format

private val CodeBackgroundPaddingHorizontal = 6.dp
private val CodeBackgroundPaddingVertical = 2.dp
private val CodeBackgroundCornerRadius = 6.dp

/**
 * Renders a [RichTextString] as created with [richTextString].
 *
 * Inline code ([Format.Code]) spans are drawn with a rounded, per-line background
 * (HTML-like rendering): a SpanStyle background cannot do rounded corners or
 * padding and an inline-content chip cannot wrap across lines, so the background is
 * painted manually behind the text, one rounded rect per line fragment.
 *
 * @sample com.halilibo.richtext.ui.previews.TextPreview
 */
@Composable
public fun RichTextScope.Text(
  text: RichTextString,
  modifier: Modifier = Modifier,
  onTextLayout: (TextLayoutResult) -> Unit = {},
  softWrap: Boolean = true,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE
) {
  val style = currentRichTextStyle.stringStyle
  val contentColor = currentContentColor

  val codeRanges = remember(text) { text.codeSpanRanges() }
  val hasCodeSpans = codeRanges.isNotEmpty()

  val annotated = remember(text, style, contentColor) {
    val resolvedStyle = (style ?: RichTextStringStyle.Default).resolveDefaults()
    // Rounded backgrounds are drawn manually; drop the square SpanStyle background.
    val spanStyle = if (hasCodeSpans) {
      resolvedStyle.withCodeBackground(Color.Unspecified)
    } else {
      resolvedStyle
    }
    text.toAnnotatedString(spanStyle, contentColor)
  }

  val codeBackground = (style?.codeStyle?.background
    ?: Format.Code.DefaultStyle.background)
    .takeOrElse { DefaultCodeBlockBackgroundColor }

  val density = LocalDensity.current
  val codePaddingH = with(density) { CodeBackgroundPaddingHorizontal.toPx() }
  val codePaddingV = with(density) { CodeBackgroundPaddingVertical.toPx() }
  val codeCorner = with(density) { CodeBackgroundCornerRadius.toPx() }

  var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
  val handleTextLayout: (TextLayoutResult) -> Unit = { result ->
    textLayout = result
    onTextLayout(result)
  }

  val codeBackgroundModifier = if (hasCodeSpans) {
    Modifier.drawBehind {
      textLayout?.let { layout ->
        codeRanges.forEach { range ->
          drawCodeSpanBackground(
            layout = layout,
            range = range,
            color = codeBackground,
            paddingH = codePaddingH,
            paddingV = codePaddingV,
            corner = codeCorner
          )
        }
      }
    }
  } else {
    Modifier
  }

  val inlineContents = remember(text) { text.getInlineContents() }

  if (inlineContents.isEmpty()) {
    Text(
      text = annotated,
      modifier = modifier.then(codeBackgroundModifier),
      onTextLayout = handleTextLayout,
      softWrap = softWrap,
      overflow = overflow,
      maxLines = maxLines
    )
  } else {
    // expensive constraints reading path
    BoxWithConstraints(modifier = modifier) {
      val inlineTextContents = manageInlineTextContents(
        inlineContents = inlineContents,
        textConstraints = constraints
      )

      Text(
        text = annotated,
        modifier = codeBackgroundModifier,
        onTextLayout = handleTextLayout,
        inlineContent = inlineTextContents,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
      )
    }
  }
}

/**
 * Draws one rounded rect per line fragment covered by [range], so a code span that
 * wraps to multiple lines gets a naturally wrapping background like HTML rendering.
 *
 * The rect height is based on the font size (not the full line height) and centered
 * vertically inside the line box, so the text sits centered in its background and
 * two wrapped fragments keep a visible gap between lines.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCodeSpanBackground(
  layout: TextLayoutResult,
  range: IntRange,
  color: Color,
  paddingH: Float,
  paddingV: Float,
  corner: Float
) {
  if (range.isEmpty() || range.last >= layout.layoutInput.text.length) return

  val styleFontSize = layout.layoutInput.style.fontSize
  val fontSizePx = if (styleFontSize.isSpecified) styleFontSize.toPx() else 0f

  val startLine = layout.getLineForOffset(range.first)
  val endLine = layout.getLineForOffset(range.last)
  for (line in startLine..endLine) {
    val left = if (line == startLine) {
      layout.getBoundingBox(range.first).left
    } else {
      layout.getLineLeft(line)
    }
    val right = if (line == endLine) {
      layout.getBoundingBox(range.last).right
    } else {
      layout.getLineRight(line)
    }
    if (right <= left) continue

    val lineTop = layout.getLineTop(line)
    val lineBottom = layout.getLineBottom(line)
    val lineHeight = lineBottom - lineTop
    // 以字号为内容高度,背景在行框内垂直居中;上下各留 paddingV,
    // 行高减去内容高度即折行后两段背景之间的间距
    val contentHeight = if (fontSizePx > 0f && fontSizePx < lineHeight) {
      fontSizePx
    } else {
      lineHeight * 0.75f
    }
    val centerY = (lineTop + lineBottom) / 2f
    val halfHeight = contentHeight / 2f + paddingV
    drawRoundRect(
      color = color,
      topLeft = Offset(left - paddingH, centerY - halfHeight),
      size = Size(
        width = right - left + paddingH * 2,
        height = halfHeight * 2
      ),
      cornerRadius = CornerRadius(corner, corner)
    )
  }
}

private fun RichTextStringStyle.withCodeBackground(background: Color) =
  RichTextStringStyle(
    boldStyle = boldStyle,
    italicStyle = italicStyle,
    underlineStyle = underlineStyle,
    strikethroughStyle = strikethroughStyle,
    subscriptStyle = subscriptStyle,
    superscriptStyle = superscriptStyle,
    codeStyle = codeStyle?.copy(background = background),
    linkStyle = linkStyle
  )

private fun AnnotatedString.getConsumableAnnotations(textFormatObjects: Map<String, Any>, offset: Int): Sequence<Format.Link> =
  getStringAnnotations(Format.FormatAnnotationScope, offset, offset)
    .asSequence()
    .mapNotNull {
      Format.findTag(
        it.item,
        textFormatObjects
      ) as? Format.Link
    }
