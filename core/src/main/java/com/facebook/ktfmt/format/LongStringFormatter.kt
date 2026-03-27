/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.ktfmt.format

import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class LongStringFormatter(
    private val maxWidth: Int,
    private val continuationIndent: Int,
) {

  fun format(code: String): String {
    if (code.lines().none { it.length > maxWidth }) return code

    val replacements = mutableListOf<Replacement>()
    Parser.parse(code)
        .accept(
            object : KtTreeVisitorVoid() {
              override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
                super.visitStringTemplateExpression(expression)
                val replacement = wrap(expression, code) ?: return
                replacements.add(
                    Replacement(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        replacement = replacement,
                    )
                )
              }
            }
        )

    if (replacements.isEmpty()) return code

    val output = StringBuilder(code)
    for (replacement in replacements.sortedByDescending { it.startOffset }) {
      output.replace(replacement.startOffset, replacement.endOffset, replacement.replacement)
    }
    return output.toString()
  }

  private fun wrap(expression: KtStringTemplateExpression, code: String): String? {
    val text = expression.text
    if (!isPlainQuotedString(expression, text) || !isSafeToRewrite(expression)) {
      return null
    }

    val lineStart = code.lastIndexOf('\n', expression.startOffset - 1).let { it + 1 }
    val lineEnd = code.indexOf('\n', expression.endOffset).let { if (it == -1) code.length else it }
    if (lineEnd - lineStart <= maxWidth) {
      return null
    }

    val literalWidth =
        minOf(
            maxWidth - (expression.startOffset - lineStart) - 5,
            maxWidth - continuationIndent - 5,
            maxWidth - (lineEnd - expression.endOffset) - continuationIndent - 2,
        )
    if (literalWidth < 8) {
      return null
    }

    val segments = reflow(splitIntoComponents(text.substring(1, text.length - 1)), literalWidth)
    if (segments.size < 2 || segments.any { it.length > literalWidth }) {
      return null
    }
    return segments.joinToString(separator = " + ") { "\"$it\"" }
  }

  private fun isPlainQuotedString(expression: KtStringTemplateExpression, text: String): Boolean {
    if (!text.startsWith('"') || text.startsWith("\"\"\"")) {
      return false
    }
    return expression.entries.all {
      it is KtLiteralStringTemplateEntry || it is KtEscapeStringTemplateEntry
    }
  }

  private fun isSafeToRewrite(expression: KtStringTemplateExpression): Boolean {
    val parent = expression.parent
    return when (parent) {
      is KtDotQualifiedExpression -> parent.receiverExpression !== expression
      is KtSafeQualifiedExpression -> parent.receiverExpression !== expression
      is KtArrayAccessExpression -> parent.arrayExpression !== expression
      is KtCallExpression -> parent.calleeExpression !== expression
      else -> true
    }
  }

  private fun splitIntoComponents(content: String): List<String> {
    val components = mutableListOf<String>()
    var current = StringBuilder()
    var index = 0
    while (index < content.length) {
      val char = content[index]
      current.append(char)
      if (char == '\\' && index + 1 < content.length) {
        index++
        current.append(content[index])
      } else if (char.isWhitespace()) {
        components.add(current.toString())
        current = StringBuilder()
      }
      index++
    }
    if (current.isNotEmpty()) {
      components.add(current.toString())
    }
    return components
  }

  private fun reflow(components: List<String>, literalWidth: Int): List<String> {
    val remaining = ArrayDeque(components)
    val lines = mutableListOf<String>()
    while (remaining.isNotEmpty()) {
      val line = StringBuilder()
      var lineLength = 0
      while (remaining.isNotEmpty()) {
        val next = remaining.first()
        if (lineLength != 0 && lineLength + next.length > literalWidth) {
          break
        }
        line.append(remaining.removeFirst())
        lineLength = line.length
      }
      if (line.isEmpty()) {
        line.append(remaining.removeFirst())
      }
      lines.add(line.toString())
    }
    return lines
  }

  private data class Replacement(
      val startOffset: Int,
      val endOffset: Int,
      val replacement: String,
  )
}
