package com.example.githubappmanager.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.githubappmanager.data.AppInfo
import com.example.githubappmanager.data.AppInstallStatus
import com.example.githubappmanager.data.GitHubRelease
import java.io.File

class AppInstallManager(private val context: Context) {
    
    private val packageManager = context.packageManager

    fun getInstalledAppInfo(packageName: String): AppInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            
            val applicationInfo = packageInfo.applicationInfo
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
            null
        }
    }

    fun guessPackageName(owner: String, repoName: String): String {
        return "com.${owner.lowercase()}.${repoName.lowercase().replace("-", "").replace("_", "")}"
    }

    fun checkInstallStatus(release: GitHubRelease, packageName: String): AppInstallStatus {
        val installedApp = getInstalledAppInfo(packageName)
            ?: return AppInstallStatus.NOT_INSTALLED

        return try {
            val releaseVersion = release.tagName.removePrefix("v").removePrefix("V")
            val installedVersion = installedApp.versionName.removePrefix("v").removePrefix("V")
            
            when {
                compareVersions(installedVersion, releaseVersion) < 0 -> AppInstallStatus.INSTALLED_OUTDATED
                compareVersions(installedVersion, releaseVersion) >= 0 -> AppInstallStatus.INSTALLED_CURRENT
                else -> AppInstallStatus.UNKNOWN
            }
        } catch (e: Exception) {
            AppInstallStatus.UNKNOWN
        }
    }

    private fun compareVersions(installed: String, release: String): Int {
        val installedParts = installed.split(".").mapNotNull { it.toIntOrNull() }
        val releaseParts = release.split(".").mapNotNull { it.toIntOrNull() }
        
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
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        context.startActivity(intent)
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