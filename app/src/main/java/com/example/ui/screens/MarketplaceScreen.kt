package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Artisan
import com.example.ui.theme.SiyanetaAmber
import com.example.ui.theme.SiyanetaBlue
import com.example.ui.theme.SiyanetaGreen
import com.example.ui.viewmodel.SiyanetaViewModel

private val categoryFilters = listOf(
    Pair(null, "الكل"),
    Pair("mechanic", "ميكانيكا"),
    Pair("aluminum", "ألومنيوم"),
    Pair("plumbing", "سباكة"),
    Pair("electrical", "كهرباء"),
    Pair("ac", "تكييف"),
    Pair("appliances", "أجهزة")
)

@Composable
fun MarketplaceScreen(
    viewModel: SiyanetaViewModel,
    initialCategoryFilter: String? = null,
    initialProblemDescription: String = "",
    selectedArtisanForBooking: Artisan? = null,
    onClearSelectedArtisan: () -> Unit = {}
) {
    val context = LocalContext.current
    val artisans by viewModel.artisans.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeCategory by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }
    var activeBookingArtisan by remember(selectedArtisanForBooking) { mutableStateOf(selectedArtisanForBooking) }
    var selectedArtisanProfile by remember { mutableStateOf<Artisan?>(null) }

    val filteredArtisans = artisans.filter { artisan ->
        val matchesCategory = activeCategory == null || artisan.category == activeCategory
        val matchesQuery = searchQuery.isBlank() ||
                artisan.name.contains(searchQuery, ignoreCase = true) ||
                artisan.locationName.contains(searchQuery, ignoreCase = true) ||
                artisan.bio.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "سوق الحرفيين والخبراء",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اختر من نخبة الفنيين المحليين المعتمدين مع إمكانية الحجز المباشر",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("artisan_search_input"),
                    placeholder = { Text("ابحث بالاسم، الحي، أو التخصص...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryFilters) { (key, label) ->
                    val isSelected = activeCategory == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { activeCategory = key }
                            .testTag("filter_chip_${key ?: "all"}")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Artisans List
            if (filteredArtisans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا يوجد حرفيون مطابقون لبحثك",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جرب تغيير تصنيف الخدمة أو مسح شريط البحث",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredArtisans, key = { it.id }) { artisan ->
                        ArtisanDetailedCard(
                            artisan = artisan,
                            onViewProfile = { selectedArtisanProfile = artisan },
                            onBook = { activeBookingArtisan = artisan },
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${artisan.phone}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Booking Sheet / Dialog
        if (activeBookingArtisan != null) {
            BookingDialog(
                artisan = activeBookingArtisan!!,
                initialProblem = initialProblemDescription,
                defaultAddress = userProfile?.location ?: "الرياض، المملكة العربية السعودية",
                onDismiss = {
                    activeBookingArtisan = null
                    onClearSelectedArtisan()
                },
                onConfirm = { date, time, address, problem ->
                    viewModel.createBooking(
                        artisan = activeBookingArtisan!!,
                        scheduledDate = date,
                        timeSlot = time,
                        address = address,
                        problemDescription = problem
                    )
                    activeBookingArtisan = null
                    onClearSelectedArtisan()
                }
            )
        }

        // Artisan Profile Details Dialog
        if (selectedArtisanProfile != null) {
            ArtisanProfileDialog(
                artisan = selectedArtisanProfile!!,
                onDismiss = { selectedArtisanProfile = null },
                onBook = {
                    activeBookingArtisan = selectedArtisanProfile
                    selectedArtisanProfile = null
                },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedArtisanProfile!!.phone}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun ArtisanDetailedCard(
    artisan: Artisan,
    onViewProfile: () -> Unit,
    onBook: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProfile)
            .testTag("artisan_item_${artisan.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = artisan.name.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = artisan.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثق",
                                tint = SiyanetaBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = when (artisan.category) {
                                "mechanic" -> "ميكانيكا وكهرباء سيارات"
                                "aluminum" -> "ألومنيوم ونوافذ وسحاب"
                                "plumbing" -> "سباكة وتمديدات"
                                "electrical" -> "كهرباء عامة ومنازل"
                                "ac" -> "تكييف وتبريد"
                                else -> "صيانة أجهزة منزلية"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (artisan.isAvailable) SiyanetaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (artisan.isAvailable) "متاح الآن" else "مشغول",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (artisan.isAvailable) SiyanetaGreen else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = artisan.bio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Details info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = SiyanetaAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${artisan.rating} (${artisan.reviewCount})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = artisan.locationName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Price & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "رسوم المعاينة/الساعة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = artisan.hourlyRate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCall,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onBook,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("حجز موعد", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDialog(
    artisan: Artisan,
    initialProblem: String,
    defaultAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (date: String, timeSlot: String, address: String, problem: String) -> Unit
) {
    var selectedDate by remember { mutableStateOf("اليوم") }
    var selectedTimeSlot by remember { mutableStateOf("صباحاً (9:00 - 12:00)") }
    var address by remember { mutableStateOf(defaultAddress) }
    var problemDescription by remember { mutableStateOf(initialProblem) }

    val dateOptions = listOf("اليوم", "غداً", "بعد غد")
    val timeOptions = listOf("صباحاً (9:00 - 12:00)", "ظهراً (1:00 - 4:00)", "مساءً (5:00 - 9:00)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "حجز موعد صيانة", fontWeight = FontWeight.Bold)
                Text(
                    text = "مع الفني: ${artisan.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Scheduled Date
                Text("اختر موعد الزيارة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dateOptions.forEach { date ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedDate == date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDate = date }
                        ) {
                            Text(
                                text = date,
                                color = if (selectedDate == date) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Time slot
                Text("الفترة المفضلة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    timeOptions.forEach { slot ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTimeSlot == slot) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTimeSlot = slot }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedTimeSlot == slot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selectedTimeSlot == slot) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedTimeSlot == slot) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("عنوان الموقع / الحي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    label = { Text("تفاصيل العطل أو المشكلة") },
                    placeholder = { Text("مثلاً: صوت طقطقة في العكوس أو تسريب أسفل المجلى...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDate, selectedTimeSlot, address, problemDescription)
                },
                enabled = address.isNotBlank()
            ) {
                Text("تأكيد إرسال الطلب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun ArtisanProfileDialog(
    artisan: Artisan,
    onDismiss: () -> Unit,
    onBook: () -> Unit,
    onCall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = artisan.name.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = artisan.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "خبرة ${artisan.experienceYears} سنوات في المجال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "نبذة عن الفني:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text(text = artisan.bio, style = MaterialTheme.typography.bodyMedium)

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "التقييم العام", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = SiyanetaAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${artisan.rating} / 5.0", fontWeight = FontWeight.Bold)
                        }
                    }

                    Column {
                        Text(text = "سعر المعاينة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = artisan.hourlyRate, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onBook) {
                Text("حجز موعد الآن")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCall) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("اتصال مباشر")
            }
        }
    )
}
