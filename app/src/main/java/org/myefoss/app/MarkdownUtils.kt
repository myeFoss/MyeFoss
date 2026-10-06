package org.myefoss.app

import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat

object MarkdownUtils {

    /**
     * Converts a subset of Markdown to HTML, then parses it into a Spanned
     * that can be displayed in an Android TextView with LinkMovementMethod.
     */
    fun markdownToSpanned(markdown: String): Spanned {
        val html = markdownToHtml(markdown)
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Simple, fast markdown-to-HTML converter supporting:
     * - Headers (###, ##, #)
     * - Unordered lists (- or *)
     * - Ordered lists (1., 2., etc.)
     * - Bold (**text** or __text__)
     * - Italic (*text* or _text_)
     * - Code (`code`)
     * - Blockquotes (> quote)
     * - Links ([title](url))
     * - Paragraphs and line breaks
     */
    fun markdownToHtml(markdown: String): String {
        if (markdown.isBlank()) return ""

        val lines = markdown.lines()
        val sb = StringBuilder()
        var inList = false
        var inOrderedList = false

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            // Unordered list item
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (inOrderedList) {
                    sb.append("</ol>")
                    inOrderedList = false
                }
                if (!inList) {
                    sb.append("<ul>")
                    inList = true
                }
                val content = trimmed.substring(2).trim()
                sb.append("<li>").append(formatInline(content)).append("</li>")
                continue
            }

            // Ordered list item (e.g. "1. ")
            val matchOrdered = Regex("^(\\d+)\\.\\s+(.*)$").find(trimmed)
            if (matchOrdered != null) {
                if (inList) {
                    sb.append("</ul>")
                    inList = false
                }
                if (!inOrderedList) {
                    sb.append("<ol>")
                    inOrderedList = true
                }
                val content = matchOrdered.groupValues[2]
                sb.append("<li>").append(formatInline(content)).append("</li>")
                continue
            }

            // Close list if open
            if (inList) {
                sb.append("</ul>")
                inList = false
            }
            if (inOrderedList) {
                sb.append("</ol>")
                inOrderedList = false
            }

            // Headers
            if (trimmed.startsWith("### ")) {
                sb.append("<h3>").append(formatInline(trimmed.substring(4))).append("</h3>")
            } else if (trimmed.startsWith("## ")) {
                sb.append("<h2>").append(formatInline(trimmed.substring(3))).append("</h2>")
            } else if (trimmed.startsWith("# ")) {
                sb.append("<h1>").append(formatInline(trimmed.substring(2))).append("</h1>")
            } else if (trimmed.startsWith("> ")) {
                sb.append("<blockquote>").append(formatInline(trimmed.substring(2))).append("</blockquote>")
            } else if (trimmed.isEmpty()) {
                sb.append("<br/>")
            } else {
                sb.append("<p>").append(formatInline(trimmed)).append("</p>")
            }
        }

        if (inList) sb.append("</ul>")
        if (inOrderedList) sb.append("</ol>")

        return sb.toString()
    }

    private fun formatInline(text: String): String {
        var res = escapeHtmlBasic(text)

        // Bold: **text** or __text__
        res = res.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
        res = res.replace(Regex("__(.+?)__"), "<b>$1</b>")

        // Inline code: `code`
        res = res.replace(Regex("`([^`]+)`"), "<tt>$1</tt>")

        // Italic: *text* or _text_
        res = res.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "<i>$1</i>")
        res = res.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "<i>$1</i>")

        // Links: [text](url)
        res = res.replace(Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)"), "<a href=\"$2\">$1</a>")

        return res
    }

    private fun escapeHtmlBasic(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
