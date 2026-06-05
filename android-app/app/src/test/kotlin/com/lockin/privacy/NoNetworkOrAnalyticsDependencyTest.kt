package com.lockin.privacy

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class NoNetworkOrAnalyticsDependencyTest {
    @Test
    fun projectDoesNotDeclareNetworkAnalyticsAccountVpnOrRemoteManagementSurface() {
        val repoRoot = findRepoRoot()
        val forbiddenDependencyTerms = listOf(
            "firebase",
            "analytics",
            "crashlytics",
            "retrofit",
            "okhttp",
            "ktor-client",
            "play-services-auth",
            "auth0",
            "appwrite",
            "amplitude",
            "mixpanel",
            "sentry",
            "vpnservice",
            "wireguard",
            "remote-config",
            "remoteconfig",
            "management-api",
            "website-blocking"
        )
        val forbiddenPermissions = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.BIND_VPN_SERVICE",
            "android.permission.GET_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "android.permission.AUTHENTICATE_ACCOUNTS"
        )
        val checkedFiles = listOf(
            repoRoot.resolve("android-app/build.gradle.kts"),
            repoRoot.resolve("android-app/app/build.gradle.kts"),
            repoRoot.resolve("android-app/gradle/libs.versions.toml"),
            repoRoot.resolve("android-app/app/src/main/AndroidManifest.xml")
        )
        val violations = checkedFiles
            .filter { it.exists() }
            .flatMap { file ->
                val text = file.readText()
                val dependencyViolations = forbiddenDependencyTerms
                    .filter { term -> text.contains(term, ignoreCase = true) }
                    .map { term -> "${repoRoot.relativize(file)} contains forbidden dependency/surface term `$term`" }
                val permissionViolations = forbiddenPermissions
                    .filter { permission -> text.contains(permission, ignoreCase = true) }
                    .map { permission -> "${repoRoot.relativize(file)} declares forbidden permission `$permission`" }
                dependencyViolations + permissionViolations
            }

        assertTrue(violations.joinToString(separator = "\n"), violations.isEmpty())
    }

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
