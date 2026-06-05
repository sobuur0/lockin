package com.lockin.ui.copy

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductBoundaryCopyTest {
    @Test
    fun uiCopyAvoidsAdjacentProductPromises() {
        val repoRoot = findRepoRoot()
        val uiRoots = listOf(
            repoRoot.resolve("android-app/app/src/main/kotlin/com/lockin/ui"),
            repoRoot.resolve("android-app/app/src/main/res/values")
        )
        val copySnippets = uiRoots.flatMap { root ->
            Files.walk(root).use { paths ->
                paths
                    .filter { it.isRegularFile() }
                    .filter { it.extension == "kt" || it.extension == "xml" }
                    .flatMap { file -> extractUserFacingCopy(file).stream() }
                    .toList()
            }
        }
        val alwaysForbidden = listOf(
            "parental control",
            "screen time",
            "habit",
            "productivity",
            "coach",
            "emergency unlock",
            "recovery pin",
            "temporary bypass",
            "override"
        )
        val violations = copySnippets.flatMap { snippet ->
            val lower = snippet.lowercase()
            val direct = alwaysForbidden
                .filter { phrase -> lower.contains(phrase) }
                .map { phrase -> "`$phrase` appears in UI copy: \"$snippet\"" }
            val reversibleLanguage = listOf("pause", "bypass")
                .filter { word -> lower.contains(word) && !lower.contains("cannot") && !lower.contains("can't") }
                .map { word -> "`$word` appears outside irreversible boundary copy: \"$snippet\"" }
            direct + reversibleLanguage
        }

        assertTrue(violations.joinToString(separator = "\n"), violations.isEmpty())
    }

    private fun extractUserFacingCopy(file: Path): List<String> {
        val text = file.readText()
        return when (file.extension) {
            "kt" -> Regex("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"")
                .findAll(text)
                .map { it.groupValues[1] }
                .filterNot { it.isCodeLikeLiteral() }
                .toList()
            "xml" -> Regex(">([^<]+)<")
                .findAll(text)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotBlank() }
                .toList()
            else -> emptyList()
        }
    }

    private fun String.isCodeLikeLiteral(): Boolean =
        isBlank() ||
            startsWith("com.") ||
            contains("/") ||
            contains("_") ||
            all { it.isUpperCase() || it == '_' || it.isDigit() }

    private fun findRepoRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current.parent != null) {
            if (Files.exists(current.resolve("AGENTS.md")) && current.name == "lockin") {
                return current
            }
            current = current.parent
        }
        error("Could not locate repository root")
    }
}
