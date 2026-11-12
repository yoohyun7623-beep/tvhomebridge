package com.example.tvhomebridge

import android.content.Context

/**
 * 광고/추적 패키지 비활성화 루틴.
 *
 * ⚠️ 일반 앱은 `pm disable-user` 권한(shell 권한)이 없어 대부분 실패합니다.
 *    - 확실한 방법: PC에서 scripts/tv_setup.sh|bat --debloat (ADB shell 권한)
 *    - 앱 내부에서 성공시키려면 Shizuku 같은 shell 권한 중계 도구가 필요합니다.
 * 이 코드는 "권한이 있으면 동작하고, 없으면 실패 사유를 로그로 보여주는" 용도입니다.
 */
object BloatManager {

    fun loadPackages(ctx: Context): List<String> =
        ctx.assets.open("bloat_packages.txt").bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

    /** @return 패키지별 결과 로그 */
    fun disableAll(ctx: Context): List<String> =
        loadPackages(ctx).map { pkg -> "$pkg → ${exec("pm disable-user --user 0 $pkg")}" }

    fun enableAll(ctx: Context): List<String> =
        loadPackages(ctx).map { pkg -> "$pkg → ${exec("pm enable --user 0 $pkg")}" }

    private fun exec(cmd: String): String = try {
        val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        if (out.isEmpty()) "OK" else out.lineSequence().first()
    } catch (e: Exception) {
        "실패(${e.javaClass.simpleName})"
    }
}
