package com.example.githubappmanager.data.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.githubappmanager.domain.model.AppInfo
import com.example.githubappmanager.domain.model.AppInstallStatus
import com.example.githubappmanager.domain.model.GitHubRelease
import java.io.File

class AppInstallManager(private val context: Context) {
    
    private val packageManager = context.packageManager

    // NEW: Get ALL installed packages and find matches for the repo
    fun findInstalledPackageForRepo(owner: String, repoName: String): String? {
        Log.d("AppInstallManager", "Searching for installed package for $owner/$repoName")
        
        val installedPackages = getInstalledPackages()
        val possiblePatterns = generatePackagePatterns(owner, repoName)
        
        Log.d("AppInstallManager", "Checking ${installedPackages.size} installed packages against ${possiblePatterns.size} patterns")
        
        installedPackages.forEach { packageName ->
            if (matchesAnyPattern(packageName, possiblePatterns)) {
                Log.d("AppInstallManager", "Found matching package: $packageName for $owner/$repoName")
                return packageName
            }
        }
        
        Log.d("AppInstallManager", "No installed package found for $owner/$repoName")
        return null
    }

    // NEW: Get list of all installed packages
    private fun getInstalledPackages(): List<String> {
        return try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            packages.map { it.packageName }
        } catch (e: Exception) {
            Log.e("AppInstallManager", "Error getting installed packages", e)
            emptyList()
        }
    }

    // NEW: Generate possible package name patterns
    private fun generatePackagePatterns(owner: String, repoName: String): List<String> {
        val cleanRepo = repoName.lowercase().replace("-", "").replace("_", "").replace(".", "")
        val cleanOwner = owner.lowercase().replace("-", "").replace("_", "").replace(".", "")
        
        return listOf(
            "com.$cleanOwner.$cleanRepo",
            "com.github.$cleanOwner.$cleanRepo", 
            "io.github.$cleanOwner.$cleanRepo",
            "org.$cleanOwner.$cleanRepo",
            "dev.$cleanOwner.$cleanRepo",
            "net.$cleanOwner.$cleanRepo",
            "app.$cleanOwner.$cleanRepo",
            "me.$cleanOwner.$cleanRepo",
            "eu.$cleanOwner.$cleanRepo",
            "uk.$cleanOwner.$cleanRepo",
            "de.$cleanOwner.$cleanRepo",
            "fr.$cleanOwner.$cleanRepo",
            "jp.$cleanOwner.$cleanRepo",
            "kr.$cleanOwner.$cleanRepo",
            "cn.$cleanOwner.$cleanRepo",
            "in.$cleanOwner.$cleanRepo",
            "br.$cleanOwner.$cleanRepo",
            "ru.$cleanOwner.$cleanRepo",
            "es.$cleanOwner.$cleanRepo",
            "it.$cleanOwner.$cleanRepo",
            "ca.$cleanOwner.$cleanRepo",
            "au.$cleanOwner.$cleanRepo",
            "nz.$cleanOwner.$cleanRepo",
            "mx.$cleanOwner.$cleanRepo",
            "co.$cleanOwner.$cleanRepo",
            "ar.$cleanOwner.$cleanRepo",
            "cl.$cleanOwner.$cleanRepo",
            "pe.$cleanOwner.$cleanRepo",
            "za.$cleanOwner.$cleanRepo",
            "ng.$cleanOwner.$cleanRepo",
            "eg.$cleanOwner.$cleanRepo",
            "ke.$cleanOwner.$cleanRepo",
            "tz.$cleanOwner.$cleanRepo",
            "et.$cleanOwner.$cleanRepo",
            "gh.$cleanOwner.$cleanRepo",
            "ci.$cleanOwner.$cleanRepo",
            "sn.$cleanOwner.$cleanRepo",
            "cm.$cleanOwner.$cleanRepo",
            "ug.$cleanOwner.$cleanRepo",
            "ao.$cleanOwner.$cleanRepo",
            "mz.$cleanOwner.$cleanRepo",
            "zm.$cleanOwner.$cleanRepo",
            "zw.$cleanOwner.$cleanRepo",
            "mw.$cleanOwner.$cleanRepo",
            "bw.$cleanOwner.$cleanRepo",
            "na.$cleanOwner.$cleanRepo",
            "sz.$cleanOwner.$cleanRepo",
            "ls.$cleanOwner.$cleanRepo",
            "so.$cleanOwner.$cleanRepo",
            "sd.$cleanOwner.$cleanRepo",
            "ss.$cleanOwner.$cleanRepo",
            "er.$cleanOwner.$cleanRepo",
            "dj.$cleanOwner.$cleanRepo",
            "km.$cleanOwner.$cleanRepo",
            "mu.$cleanOwner.$cleanRepo",
            "sc.$cleanOwner.$cleanRepo",
            "cv.$cleanOwner.$cleanRepo",
            "gw.$cleanOwner.$cleanRepo",
            "gq.$cleanOwner.$cleanRepo",
            "ga.$cleanOwner.$cleanRepo",
            "cg.$cleanOwner.$cleanRepo",
            "cd.$cleanOwner.$cleanRepo",
            "rw.$cleanOwner.$cleanRepo",
            "bi.$cleanOwner.$cleanRepo",
            "mg.$cleanOwner.$cleanRepo",
            "ml.$cleanOwner.$cleanRepo",
            "bf.$cleanOwner.$cleanRepo",
            "ne.$cleanOwner.$cleanRepo",
            "tg.$cleanOwner.$cleanRepo",
            "bj.$cleanOwner.$cleanRepo",
            "mr.$cleanOwner.$cleanRepo",
            "lr.$cleanOwner.$cleanRepo",
            "sl.$cleanOwner.$cleanRepo",
            "gn.$cleanOwner.$cleanRepo",
            "gm.$cleanOwner.$cleanRepo",
            "gm.$cleanOwner.$cleanRepo",
            cleanRepo, // Sometimes package is just the repo name
            cleanOwner // Sometimes package is just the owner name
        ).distinct()
    }

    // NEW: Check if package matches any pattern
    private fun matchesAnyPattern(packageName: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            packageName.equals(pattern, ignoreCase = true) || 
            packageName.contains(pattern, ignoreCase = true)
        }
    }

    fun getInstalledAppInfo(packageName: String): AppInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            
            val applicationInfo = packageInfo.applicationInfo
            Log.d(
                "AppInstallManager",
                "getInstalledAppInfo: found pkg=${packageInfo.packageName}, versionName=${packageInfo.versionName}, versionCode=${packageInfo.versionCode}"
            )
            AppInfo(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                isSystemApp = applicationInfo?.let { 
                    (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 
                } ?: false
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("AppInstallManager", "getInstalledAppInfo: package not found: $packageName")
            null
        }
    }

    // UPDATED: Improved package name guessing
    fun guessPackageName(owner: String, repoName: String): String {
        val cleanRepo = repoName.lowercase().replace("-", "").replace("_", "").replace(".", "")
        val cleanOwner = owner.lowercase().replace("-", "").replace("_", "").replace(".", "")
        
        // First, try to find if any package is already installed
        val installedPackage = findInstalledPackageForRepo(owner, repoName)
        if (installedPackage != null) {
            return installedPackage
        }
        
        // If no installed package found, guess the most common pattern
        return "com.$cleanOwner.$cleanRepo"
    }

    // UPDATED: Better install status checking
    fun checkInstallStatus(release: GitHubRelease?, guessedPackageName: String?): AppInstallStatus {
        // First, try to find any installed package for this repo
        val installedPackage = if (guessedPackageName != null && getInstalledAppInfo(guessedPackageName) != null) {
            guessedPackageName
        } else {
            null
        }

        if (installedPackage == null) {
            Log.d("AppInstallManager", "No installed package found - status: NOT_INSTALLED")
            return AppInstallStatus.NOT_INSTALLED
        }

        val installedApp = getInstalledAppInfo(installedPackage) ?: return AppInstallStatus.NOT_INSTALLED
        
        // If no release info, assume installed but unknown version
        if (release == null) {
            Log.d("AppInstallManager", "No release info - status: INSTALLED_CURRENT")
            return AppInstallStatus.INSTALLED_CURRENT
        }

        return try {
            val releaseVersion = release.tagName?.removePrefix("v")?.removePrefix("V") ?: "1.0.0"
            val installedVersion = installedApp.versionName.removePrefix("v").removePrefix("V")
            
            val comparison = compareVersions(installedVersion, releaseVersion)
            
            when {
                comparison < 0 -> {
                    Log.d("AppInstallManager", "Installed version ($installedVersion) < Release version ($releaseVersion) - status: INSTALLED_OUTDATED")
                    AppInstallStatus.INSTALLED_OUTDATED
                }
                comparison >= 0 -> {
                    Log.d("AppInstallManager", "Installed version ($installedVersion) >= Release version ($releaseVersion) - status: INSTALLED_CURRENT")
                    AppInstallStatus.INSTALLED_CURRENT
                }
                else -> {
                    Log.d("AppInstallManager", "Version comparison failed - status: UNKNOWN")
                    AppInstallStatus.UNKNOWN
                }
            }
        } catch (e: Exception) {
            Log.e("AppInstallManager", "Error comparing versions", e)
            AppInstallStatus.UNKNOWN
        }
    }

    // IMPROVED: Better version comparison
    private fun compareVersions(installed: String, release: String): Int {
        val installedParts = installed.split(".", "-", "_", "+").mapNotNull { it.toIntOrNull() }
        val releaseParts = release.split(".", "-", "_", "+").mapNotNull { it.toIntOrNull() }
        
        if (installedParts.isEmpty() || releaseParts.isEmpty()) {
            // Fallback: string comparison if version parsing fails
            return installed.compareTo(release)
        }
        
        val maxLength = maxOf(installedParts.size, releaseParts.size)
        
        for (i in 0 until maxLength) {
            val installedPart = installedParts.getOrNull(i) ?: 0
            val releasePart = releaseParts.getOrNull(i) ?: 0
            
            when {
                installedPart < releasePart -> return -1
                installedPart > releasePart -> return 1
            }
        }
        return 0
    }

    fun uninstallApp(packageName: String) {
        Log.d(
            "AppInstallManager",
            "Attempting to uninstall package: $packageName"
        )
        
        val isInstalled = getInstalledAppInfo(packageName) != null
        Log.d("AppInstallManager", "Package $packageName installed: $isInstalled")
        
        if (!isInstalled) {
            Log.w("AppInstallManager", "Cannot uninstall $packageName - not installed")
            return
        }
        
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            Log.d("AppInstallManager", "Launching uninstall intent for $packageName")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppInstallManager", "Failed to launch uninstall intent for $packageName", e)
        }
    }

    fun installApk(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}