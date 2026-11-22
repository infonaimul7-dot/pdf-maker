package com.pdfmaker.scanner.util

import java.io.File

sealed class FilenameValidationResult {
    data class Valid(val sanitizedName: String) : FilenameValidationResult()
    data class Invalid(val reason: String) : FilenameValidationResult()
}

object FilenameValidator {

    private val ILLEGAL_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private const val MAX_BASE_LENGTH = 80

    fun validate(rawInput: String): FilenameValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return FilenameValidationResult.Invalid("Filename cannot be empty.")
        }

        val withoutExt = if (trimmed.endsWith(".pdf", ignoreCase = true)) {
            trimmed.dropLast(4).trim()
        } else {
            trimmed
        }

        if (withoutExt.isEmpty()) {
            return FilenameValidationResult.Invalid("Please enter a document name before .pdf.")
        }

        if (ILLEGAL_CHAR_REGEX.containsMatchIn(withoutExt)) {
            return FilenameValidationResult.Invalid("Filename contains invalid characters (\\ / : * ? \" < > |).")
        }

        if (withoutExt == "." || withoutExt == "..") {
            return FilenameValidationResult.Invalid("Filename cannot be '.' or '..'.")
        }

        val bounded = withoutExt.take(MAX_BASE_LENGTH)
        return FilenameValidationResult.Valid("$bounded.pdf")
    }

    fun resolveCollisionSafeFile(directory: File, desiredPdfName: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val cleanName = when (val result = validate(desiredPdfName)) {
            is FilenameValidationResult.Valid -> result.sanitizedName
            is FilenameValidationResult.Invalid -> "New Document.pdf"
        }

        val base = cleanName.removeSuffix(".pdf")
        var candidate = File(directory, cleanName)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base ($counter).pdf")
            counter++
        }
        return candidate
    }
}
