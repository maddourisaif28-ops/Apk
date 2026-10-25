package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiDiagnosticService
import com.example.data.ai.DiagnosticOutput
import com.example.data.model.Artisan
import com.example.data.model.Asset
import com.example.data.model.Booking
import com.example.data.model.DiagnosticRecord
import com.example.data.model.UserProfile
import com.example.data.repository.SiyanetaRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SiyanetaViewModel"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val result: DiagnosticOutput) : AiUiState
    data class Error(val message: String) : AiUiState
}

class SiyanetaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SiyanetaRepository(application)
    private val aiService = AiDiagnosticService()
    val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""

    // User Profile Flow
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(Firebase.auth.currentUser?.uid ?: "")
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User Assets Flow
    val assets: StateFlow<List<Asset>> = repository.observeAssets(Firebase.auth.currentUser?.uid ?: "")
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Artisans Directory Flow
    val artisans: StateFlow<List<Artisan>> = repository.observeArtisans()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Bookings Flow
    val bookings: StateFlow<List<Booking>> = repository.observeUserBookings(Firebase.auth.currentUser?.uid ?: "")
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Diagnostics Flow
    val diagnosticsHistory: StateFlow<List<DiagnosticRecord>> = repository.observeDiagnostics(Firebase.auth.currentUser?.uid ?: "")
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Diagnostic UI State
    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    // Status / Feedback message
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            if (Firebase.auth.currentUser != null) {
                repository.seedDefaultArtisansIfEmpty()
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun resetAiState() {
        _aiState.value = AiUiState.Idle
    }

    // --- AI Diagnostic Run ---
    fun runDiagnostic(description: String, bitmap: Bitmap? = null, audioDescription: String? = null) {
        if (description.isBlank()) {
            _message.value = "يرجى كتابة وصف للعطل أولاً"
            return
        }
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            val result = aiService.diagnose(description, bitmap, audioDescription)
            result.onSuccess { output ->
                _aiState.value = AiUiState.Success(output)
                // Save to diagnostics history in Firestore
                repository.saveDiagnostic(
                    DiagnosticRecord(
                        userId = currentUserId,
                        problemDescription = description,
                        category = output.suggestedCategory,
                        probableCause = output.probableCause,
                        severity = output.severity,
                        estimatedCost = output.estimatedCost,
                        recommendedSteps = output.recommendedSteps,
                        requiresArtisan = output.requiresArtisan
                    )
                )
            }.onFailure { error ->
                Log.e(TAG, "Diagnostic error", error)
                _aiState.value = AiUiState.Error(error.localizedMessage ?: "فشل فحص الذكاء الاصطناعي")
            }
        }
    }

    // --- Asset Operations ---
    fun addAsset(
        title: String,
        type: String,
        brand: String,
        model: String,
        year: Long,
        hoursOrMileage: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                // Auto compute smart maintenance tip
                val tip = when (type) {
                    "car" -> "تغيير زيت المحرك وفحص المكابح دورياً كل 5000-10000 كم"
                    "aluminum" -> "فحص مجاري السحاب وتزييت المفصلات والعوازل قبل الأمطار"
                    "ac" -> "تنظيف فلاتر الهواء شهرياً وفحص غاز الفريون قبل الصيف"
                    "plumbing" -> "فحص صمامات الأمان وعوامات الخزانات كل 6 أشهر"
                    "appliances" -> "تنظيف فلتر التصريف وإزالة التكلسات مرتين سنوياً"
                    else -> "فحص دوري وقائي كل 3 أشهر"
                }

                val newAsset = Asset(
                    userId = currentUserId,
                    title = title,
                    type = type,
                    brand = brand,
                    model = model,
                    year = year,
                    hoursOrMileage = hoursOrMileage,
                    nextMaintenanceTip = tip,
                    notes = notes
                )
                repository.addAsset(newAsset)
                _message.value = "تمت إضافة الأصل بنجاح"
            } catch (e: Exception) {
                _message.value = "فشلت إضافة الأصل: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAsset(assetId)
                _message.value = "تم حذف الأصل"
            } catch (e: Exception) {
                _message.value = "فشل الحذف: ${e.localizedMessage}"
            }
        }
    }

    // --- Booking Operations ---
    fun createBooking(
        artisan: Artisan,
        scheduledDate: String,
        timeSlot: String,
        address: String,
        problemDescription: String
    ) {
        viewModelScope.launch {
            try {
                val booking = Booking(
                    clientId = currentUserId,
                    clientName = Firebase.auth.currentUser?.displayName ?: "عميل صيانتها",
                    clientPhone = Firebase.auth.currentUser?.phoneNumber ?: "",
                    artisanId = artisan.id,
                    artisanName = artisan.name,
                    serviceType = artisan.category,
                    scheduledDate = scheduledDate,
                    timeSlot = timeSlot,
                    address = address,
                    problemDescription = problemDescription,
                    status = "pending"
                )
                repository.createBooking(booking)
                _message.value = "تم إرسال طلب الحجز بنجاح إلى ${artisan.name}"
            } catch (e: Exception) {
                _message.value = "فشل إرسال الحجز: ${e.localizedMessage}"
            }
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updateBookingStatus(bookingId, newStatus)
                _message.value = "تم تحديث حالة الحجز"
            } catch (e: Exception) {
                _message.value = "تعذر التحديث: ${e.localizedMessage}"
            }
        }
    }

    // --- User Profile Update ---
    fun updateProfile(name: String, phone: String, location: String) {
        viewModelScope.launch {
            try {
                val profile = UserProfile(
                    userId = currentUserId,
                    name = name,
                    phone = phone,
                    role = "client",
                    location = location
                )
                repository.saveUserProfile(profile)
                _message.value = "تم حفظ بيانات الحساب"
            } catch (e: Exception) {
                _message.value = "فشل الحفظ: ${e.localizedMessage}"
            }
        }
    }
}
