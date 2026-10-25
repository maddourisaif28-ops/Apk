package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Artisan
import com.example.data.model.Asset
import com.example.data.model.Booking
import com.example.data.model.DiagnosticRecord
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SiyanetaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google أولاً")
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { doc ->
                    if (doc.exists()) doc.toObject(UserProfile::class.java) else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = mapOf(
                "userId" to uid,
                "name" to profile.name.ifBlank { "عميل صيانتها" },
                "phone" to profile.phone,
                "role" to profile.role,
                "location" to profile.location,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Assets (Cars & Home Appliances) ---
    fun observeAssets(userId: String): Flow<List<Asset>> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/assets"
        emitAll(
            db.collection("users").document(userId).collection("assets")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Asset::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addAsset(asset: Asset) {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("assets")
        val docId = if (asset.id.isNotBlank()) asset.id else "asset_${System.currentTimeMillis()}"
        val docRef = colRef.document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "title" to asset.title,
                "type" to asset.type,
                "brand" to asset.brand,
                "model" to asset.model,
                "year" to asset.year,
                "hoursOrMileage" to asset.hoursOrMileage,
                "nextMaintenanceTip" to asset.nextMaintenanceTip,
                "notes" to asset.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAsset(assetId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("assets").document(assetId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Artisans Marketplace ---
    fun observeArtisans(): Flow<List<Artisan>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "artisans"
        emitAll(
            db.collection("artisans")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Artisan::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun seedDefaultArtisansIfEmpty() {
        if (auth.currentUser == null) return
        val colRef = db.collection("artisans")
        try {
            val snapshot = colRef.limit(1).get().await()
            if (snapshot.isEmpty) {
                val seedList = listOf(
                    Artisan(
                        id = "art_mech_01",
                        name = "م. خالد الحربي",
                        phone = "+966 50 111 2233",
                        category = "mechanic",
                        rating = 4.9,
                        reviewCount = 84,
                        isAvailable = true,
                        locationName = "الرياض - صناعية الشفاء",
                        experienceYears = 12,
                        hourlyRate = "95 ريال",
                        bio = "خبير ميكانيكا وفحص كمبيوتر محركات وهيدروليك لجميع السيارات الحديثة."
                    ),
                    Artisan(
                        id = "art_alum_02",
                        name = "الأستاذ أحمد الشامي",
                        phone = "+966 54 222 3344",
                        category = "aluminum",
                        rating = 4.8,
                        reviewCount = 47,
                        isAvailable = true,
                        locationName = "الرياض - حي الروضة",
                        experienceYears = 9,
                        hourlyRate = "75 ريال",
                        bio = "متخصص أبواب ونوافذ ألومنيوم وسحاب TPR، ضبط المفصلات والزجاج والعوازل."
                    ),
                    Artisan(
                        id = "art_plumb_03",
                        name = "فهد القحطاني",
                        phone = "+966 55 333 4455",
                        category = "plumbing",
                        rating = 4.7,
                        reviewCount = 63,
                        isAvailable = true,
                        locationName = "الرياض - حي السليمانية",
                        experienceYears = 10,
                        hourlyRate = "80 ريال",
                        bio = "كشف التسربات وصيانة المضخات والسخانات المركزية وتمديدات السباكة المنزلية."
                    ),
                    Artisan(
                        id = "art_elec_04",
                        name = "م. طارق العتيبي",
                        phone = "+966 56 444 5566",
                        category = "electrical",
                        rating = 4.95,
                        reviewCount = 112,
                        isAvailable = true,
                        locationName = "الرياض - حي الياسمين",
                        experienceYears = 14,
                        hourlyRate = "100 ريال",
                        bio = "فني كهرباء عامة ولوحات توزيع وتركيب إنارة ذكية وفحص الأحمال الكهربائية."
                    ),
                    Artisan(
                        id = "art_ac_05",
                        name = "عبدالرحمن النجار",
                        phone = "+966 57 555 6677",
                        category = "ac",
                        rating = 4.85,
                        reviewCount = 95,
                        isAvailable = true,
                        locationName = "الرياض - حي الصحافة",
                        experienceYears = 8,
                        hourlyRate = "85 ريال",
                        bio = "صيانة وتنظيف مكيفات سبليت ومركزي، تعبئة فريون أصلي وحل مشاكل ضعف التبريد."
                    ),
                    Artisan(
                        id = "art_app_06",
                        name = "سالم باوزير",
                        phone = "+966 58 666 7788",
                        category = "appliances",
                        rating = 4.75,
                        reviewCount = 52,
                        isAvailable = true,
                        locationName = "الرياض - حي الملز",
                        experienceYears = 11,
                        hourlyRate = "70 ريال",
                        bio = "إصلاح الثلاجات والغسالات الأوتوماتيكية وأفران الغاز والميكروويف المنزلية."
                    )
                )

                for (artisan in seedList) {
                    val payload = mapOf(
                        "id" to artisan.id,
                        "name" to artisan.name,
                        "phone" to artisan.phone,
                        "category" to artisan.category,
                        "rating" to artisan.rating,
                        "reviewCount" to artisan.reviewCount,
                        "isAvailable" to artisan.isAvailable,
                        "locationName" to artisan.locationName,
                        "experienceYears" to artisan.experienceYears,
                        "hourlyRate" to artisan.hourlyRate,
                        "bio" to artisan.bio,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    colRef.document(artisan.id).set(payload).await()
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
        }
    }

    // --- Bookings ---
    fun observeUserBookings(userId: String): Flow<List<Booking>> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "bookings"
        emitAll(
            db.collection("bookings")
                .whereEqualTo("clientId", userId)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Booking::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createBooking(booking: Booking): String {
        val uid = requireUserId()
        val docId = if (booking.id.isNotBlank()) booking.id else "booking_${System.currentTimeMillis()}"
        val docRef = db.collection("bookings").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "clientId" to uid,
                "clientName" to booking.clientName.ifBlank { "عميل صيانتها" },
                "clientPhone" to booking.clientPhone,
                "artisanId" to booking.artisanId,
                "artisanName" to booking.artisanName,
                "serviceType" to booking.serviceType,
                "scheduledDate" to booking.scheduledDate,
                "timeSlot" to booking.timeSlot,
                "address" to booking.address,
                "problemDescription" to booking.problemDescription,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: String) {
        val docRef = db.collection("bookings").document(bookingId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Diagnostics History ---
    fun observeDiagnostics(userId: String): Flow<List<DiagnosticRecord>> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/diagnostics"
        emitAll(
            db.collection("users").document(userId).collection("diagnostics")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(DiagnosticRecord::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveDiagnostic(record: DiagnosticRecord) {
        val uid = requireUserId()
        val docId = if (record.id.isNotBlank()) record.id else "diag_${System.currentTimeMillis()}"
        val docRef = db.collection("users").document(uid).collection("diagnostics").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "problemDescription" to record.problemDescription,
                "category" to record.category,
                "probableCause" to record.probableCause,
                "severity" to record.severity,
                "estimatedCost" to record.estimatedCost,
                "recommendedSteps" to record.recommendedSteps,
                "requiresArtisan" to record.requiresArtisan,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
