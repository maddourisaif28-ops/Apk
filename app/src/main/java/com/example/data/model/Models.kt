package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "client",
    val location: String = "الرياض، المملكة العربية السعودية",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Asset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = "car", // car, home_appliance, aluminum, plumbing, electrical, ac
    val brand: String = "",
    val model: String = "",
    val year: Long = 2022L,
    val hoursOrMileage: String = "",
    @ServerTimestamp val lastMaintenanceDate: Timestamp? = null,
    val nextMaintenanceTip: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Artisan(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "+966 50 123 4567",
    val category: String = "mechanic", // mechanic, aluminum, plumbing, electrical, ac, appliances
    val rating: Double = 4.8,
    val reviewCount: Long = 32L,
    val isAvailable: Boolean = true,
    val locationName: String = "الرياض - حي النرجس",
    val experienceYears: Long = 7L,
    val hourlyRate: String = "90 ريال",
    val bio: String = "فني معتمد ذو خبرة في الصيانة الدورية وكشف الأعطال بأحدث الأجهزة.",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val artisanId: String = "",
    val artisanName: String = "",
    val serviceType: String = "",
    val scheduledDate: String = "",
    val timeSlot: String = "",
    val address: String = "",
    val problemDescription: String = "",
    val status: String = "pending", // pending, confirmed, completed, cancelled
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiagnosticRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val problemDescription: String = "",
    val category: String = "عام",
    val probableCause: String = "",
    val severity: String = "medium", // low, medium, urgent
    val estimatedCost: String = "",
    val recommendedSteps: String = "",
    val requiresArtisan: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
