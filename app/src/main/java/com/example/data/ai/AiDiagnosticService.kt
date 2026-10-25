package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiDiagnosticService"

data class DiagnosticOutput(
    val probableCause: String,
    val severity: String, // "low", "medium", "urgent"
    val estimatedCost: String,
    val recommendedSteps: String,
    val requiresArtisan: Boolean,
    val suggestedCategory: String
)

class AiDiagnosticService {

    private val diagnosticSchema = Schema.obj(
        properties = mapOf(
            "probableCause" to Schema.string(description = "السبب المحتمل والمفصل للعطل بناء على الوصف والصورة أو الصوت"),
            "severity" to Schema.enumeration(
                values = listOf("low", "medium", "urgent"),
                description = "مستوى الخطورة والاستعجال: low للمنخفض، medium للمتوسط، urgent للعاجل والخطير"
            ),
            "estimatedCost" to Schema.string(description = "التكلفة التقديرية للإصلاح أو قطع الغيار بالريال السعودي"),
            "recommendedSteps" to Schema.string(description = "خطوات مبدئية وإجراءات وقائية وإرشادات أمان للمستخدم"),
            "requiresArtisan" to Schema.boolean(description = "هل يتطلب تدخلاً عاجلاً من حرفي أو فني مختص"),
            "suggestedCategory" to Schema.string(description = "فئة الفني المقترح: mechanic, aluminum, plumbing, electrical, ac, appliances")
        )
    )

    private val config by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = diagnosticSchema
        }
    }

    private val systemInstruction = """
        أنت مساعد تقني وفني ذكي متخصص في صيانة السيارات والمنازل والأجهزة في تطبيق 'صيانتها'.
        قم بتحليل وصف العطل أو الصورة أو التسجيل الصوتي الوارد من المستخدم، وحدد بدقة:
        1. السبب المحتمل للعطل.
        2. درجة الخطورة (منخفض low / متوسط medium / عاجل urgent).
        3. خطوات الإصلاح المبدئية وإجراءات الأمان.
        4. هل يتطلب تدخلاً عاجلاً من حرفي مختص؟ أجب بنعم أو لا، مع تحديد فئة الحرفي المناسبة.
        5. التكلفة التقديرية بالريال السعودي.
        أجب دائماً بلغة عربية فصحى منظمة وواضحة وموثوقة.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
            systemInstruction = content { text(systemInstruction) }
        )
    }

    suspend fun diagnose(
        description: String,
        bitmap: Bitmap? = null,
        audioDescription: String? = null
    ): Result<DiagnosticOutput> {
        return try {
            val promptText = buildString {
                append("وصف العطل من المستخدم: ")
                append(description)
                if (!audioDescription.isNullOrBlank()) {
                    append("\nملاحظة التسجيل الصوتي المرفق: ")
                    append(audioDescription)
                }
            }

            val responseText = withTimeout(60_000L) {
                if (bitmap != null) {
                    val response = model.generateContent(
                        content {
                            image(bitmap)
                            text(promptText)
                        }
                    )
                    response.text
                } else {
                    val response = model.generateContent(
                        content {
                            text(promptText)
                        }
                    )
                    response.text
                }
            }

            if (responseText.isNullOrBlank()) {
                Result.failure(IllegalStateException("لم يقدم نموذج الذكاء الاصطناعي تحليلاً"))
            } else {
                val json = JSONObject(responseText)
                val output = DiagnosticOutput(
                    probableCause = json.optString("probableCause", "تعذر تحديد السبب بدقة"),
                    severity = json.optString("severity", "medium"),
                    estimatedCost = json.optString("estimatedCost", "بين 80 إلى 200 ريال حسب المعاينة"),
                    recommendedSteps = json.optString("recommendedSteps", "يُرجى عدم تشغيل الجهاز أو المركبة حتى فحصها من قبل فني."),
                    requiresArtisan = json.optBoolean("requiresArtisan", true),
                    suggestedCategory = json.optString("suggestedCategory", "mechanic")
                )
                Result.success(output)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Diagnosis timed out after 60s", e)
            Result.failure(IllegalStateException("انتهت مهلة التحليل الذكي (60 ثانية). يرجى المحاولة مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI diagnosis failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
