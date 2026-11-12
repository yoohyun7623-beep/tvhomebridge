package com.example.tvhomebridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

/** 지정한 서드파티 런처를 찾아 실행하는 유틸. */
object LauncherBridge {
    private const val PREF = "bridge"
    private const val KEY_TARGET = "target_pkg"

    /** 기본 대상: Projectivy Launcher (패키지명은 기기에서 꼭 확인!) */
    const val DEFAULT_TARGET = "com.spocky.projengmenu"

    /** 설치된 HOME(런처) 앱 목록 — 이 앱 자신은 제외 */
    fun homeCandidates(ctx: Context): List<ResolveInfo> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return ctx.packageManager.queryIntentActivities(home, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
    }

    fun targetPackage(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TARGET, DEFAULT_TARGET) ?: DEFAULT_TARGET

    fun setTargetPackage(ctx: Context, pkg: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_TARGET, pkg).apply()
    }

    /**
     * 대상 런처의 HOME 액티비티를 "명시적 컴포넌트"로 직접 실행한다.
     * (명시적 실행이라 시스템 홈 선택 로직을 거치지 않아 무한 루프가 생기지 않음)
     * @return 실행 성공 여부 (런처 미설치면 false)
     */
    fun launchTarget(ctx: Context, pkg: String = targetPackage(ctx)): Boolean {
        val info = homeCandidates(ctx).firstOrNull { it.activityInfo.packageName == pkg }
            ?: return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(pkg, info.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
