package com.example.tvhomebridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

/**
 * - HOME 버튼으로 호출되면: 지정 런처를 즉시 실행하고 종료 (브릿지 역할)
 * - 앱 목록에서 직접 열면: 런처 선택 / 설정 바로가기 / 광고 패키지 비활성화 화면
 */
class MainActivity : Activity() {

    private var status: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isHomeIntent(intent) && LauncherBridge.launchTarget(this)) {
            finish()
            overridePendingTransition(0, 0)
            return
        }
        render()
    }

    // singleTask: 이미 떠 있는 상태에서 HOME을 다시 누르면 여기로 들어온다
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isHomeIntent(intent)) LauncherBridge.launchTarget(this)
    }

    private fun isHomeIntent(i: Intent?) = i?.hasCategory(Intent.CATEGORY_HOME) == true

    // ---------- UI (XML 없이 코드로 구성: 파일 수 최소화) ----------
    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(56), dp(40), dp(56), dp(40))
            setBackgroundColor(0xFF0F172A.toInt())
        }
        root.addView(label("Home Bridge", 30f, bold = true))
        root.addView(label("현재 대상 런처: ${LauncherBridge.targetPackage(this)}", 18f))

        root.addView(label("\n① 설치된 런처 중 선택", 20f, bold = true))
        val candidates = LauncherBridge.homeCandidates(this)
        if (candidates.isEmpty()) root.addView(label("선택 가능한 런처가 없습니다. 런처 APK를 먼저 설치하세요.", 16f))
        candidates.forEach { ri ->
            val pkg = ri.activityInfo.packageName
            val name = ri.loadLabel(packageManager)
            root.addView(button("$name  ($pkg)") {
                LauncherBridge.setTargetPackage(this, pkg)
                if (!LauncherBridge.launchTarget(this, pkg)) status = "실행 실패: $pkg"
                render()
            })
        }

        root.addView(label("\n② 홈 설정", 20f, bold = true))
        root.addView(button("지정 런처 지금 실행") {
            status = if (LauncherBridge.launchTarget(this)) "실행함" else "대상 런처가 설치되어 있지 않습니다"
            render()
        })
        root.addView(button("시스템 '홈 앱' 설정 열기") {
            openFirst(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_SETTINGS)
        })
        root.addView(button("앱 설정 열기 (기본값 지우기/비활성화)") {
            openFirst(Settings.ACTION_APPLICATION_SETTINGS, Settings.ACTION_SETTINGS)
        })

        root.addView(label("\n③ 광고/추적 패키지 (shell 권한 필요)", 20f, bold = true))
        root.addView(button("목록 비활성화 시도") { runAsync { BloatManager.disableAll(this) } })
        root.addView(button("목록 되돌리기(활성화)") { runAsync { BloatManager.enableAll(this) } })

        root.addView(label("\n$status", 15f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun runAsync(job: () -> List<String>) {
        status = "실행 중..."
        render()
        thread {
            val log = job().joinToString("\n")
            runOnUiThread {
                status = log + "\n\n(실패 시: PC에서 scripts/tv_setup 의 --debloat 사용)"
                render()
            }
        }
    }

    private fun openFirst(vararg actions: String) {
        for (a in actions) {
            try { startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
            catch (_: Exception) { /* 다음 후보 */ }
        }
        status = "이 TV에서는 해당 설정 화면을 열 수 없습니다"; render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(Color.WHITE)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isFocusable = true            // D-pad(리모컨) 포커스 이동
        isAllCaps = false
        setOnClickListener { onClick() }
    }
}
