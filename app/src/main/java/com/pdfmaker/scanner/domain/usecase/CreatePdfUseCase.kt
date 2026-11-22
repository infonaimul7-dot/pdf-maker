package com.pdfmaker.scanner.domain.usecase

import com.pdfmaker.scanner.data.model.CompressionLevel
import com.pdfmaker.scanner.data.model.EditablePage
import com.pdfmaker.scanner.data.model.PdfCreationResult
import com.pdfmaker.scanner.data.repository.PdfRepository
import com.pdfmaker.scanner.util.FilenameValidationResult
import com.pdfmaker.scanner.util.FilenameValidator
import javax.inject.Inject

class CreatePdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(
        pages: List<EditablePage>,
        rawTitle: String,
        compressionLevel: CompressionLevel,
        password: String?,
        confirmPassword: String?,
        requirePassword: Boolean,
        onProgress: (Float) -> Unit
    ): Result<PdfCreationResult> {
        if (pages.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one image or scanned page."))
        }

        val nameValidation = FilenameValidator.validate(rawTitle)
        if (nameValidation is FilenameValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(nameValidation.reason))
        }

        val finalPassword = if (requirePassword) {
            if (password.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Enter a PDF password or uncheck password protection."))
            }
            if (password.length < 4) {
                return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
            }
            if (password != confirmPassword) {
                return Result.failure(IllegalArgumentException("Passwords do not match."))
            }
            password
        } else {
            null
        }

        val validName = (nameValidation as FilenameValidationResult.Valid).sanitizedName
        return repository.createPdfFromPages(
            pages = pages,
            desiredTitle = validName,
            compressionLevel = compressionLevel,
            password = finalPassword,
            onProgress = onProgress
        )
    }
}
