package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CodeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = buildAnnotatedString {
                val str = text.text
                append(str)
                
                // Based on Darcula theme
                val keywordColor = Color(0xFFCC7832)
                val stringColor = Color(0xFF6A8759)
                val commentColor = Color(0xFF808080)
                val numberColor = Color(0xFF6897BB)
                val annotationColor = Color(0xFFBBB529)
                
                val keywords = "\\b(package|import|class|interface|fun|val|var|if|else|while|for|return|true|false|null|is|in|object|companion|override|public|private|protected|internal|function|const|let|await|async|typeof|export|default|try|catch|finally|div|span|html|body|head|script|style|link|meta|button|input|def|elif|print|from|color|background|margin|padding|display|flex|grid|border|width|height)\\b".toRegex()
                val strings = "\"[^\"]*\"".toRegex()
                val numbers = "\\b\\d+\\b".toRegex()
                val comments = "//.*|/\\*[\\s\\S]*?\\*/".toRegex()
                val annotations = "@[a-zA-Z_][a-zA-Z0-9_]*".toRegex()
                
                numbers.findAll(str).forEach { match ->
                    addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
                }
                
                keywords.findAll(str).forEach { match ->
                    addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
                }
                
                annotations.findAll(str).forEach { match ->
                    addStyle(SpanStyle(color = annotationColor), match.range.first, match.range.last + 1)
                }
                
                strings.findAll(str).forEach { match ->
                    addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
                }
                
                comments.findAll(str).forEach { match ->
                    addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
                }
            },
            offsetMapping = OffsetMapping.Identity
        )
    }
}
