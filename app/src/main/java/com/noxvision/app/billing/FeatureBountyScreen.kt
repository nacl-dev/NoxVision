package com.noxvision.app.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.noxvision.app.ui.NightColors
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FeatureBountyScreen(
    billingManager: BillingManager,
    repository: FeatureBountyRepository,
    onClose: () -> Unit
) {
    val userCredits by repository.userCredits.collectAsState()
    val bounties by repository.bounties.collectAsState()
    var showBuyCreditsDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf<FeatureBounty?>(null) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(BountyStatus.ACTIVE) }

    val filteredBounties = remember(bounties, selectedStatus) {
        bounties.filter { it.status == selectedStatus }
    }

    // Connect billing when screen opens
    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightColors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = NightColors.onBackground
                            )
                        }
                        Text(
                            text = "Feature Bounties",
                            color = NightColors.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFaqDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "FAQ",
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // User Credit Balance & Activity
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Available Credits",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$userCredits",
                            color = NightColors.onBackground,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* TODO: View Activity */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF424242)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.width(160.dp)
                            ) {
                                Text("VIEW ACTIVITY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { showBuyCreditsDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF43A047)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.width(160.dp)
                            ) {
                                Text("GET MORE CREDITS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(8.dp)) }
            
            // Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BountyStatus.values().forEach { status ->
                        val isSelected = selectedStatus == status
                        val (title, selectedColor) = when(status) {
                            BountyStatus.ACTIVE -> "ACTIVE" to Color(0xFF424242)
                            BountyStatus.IN_DEV -> "IN DEV" to Color(0xFF1976D2)
                            BountyStatus.SHIPPED -> "SHIPPED" to Color(0xFF43A047)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) selectedColor else Color.Transparent)
                                .clickable { selectedStatus = status }
                                .then(if (!isSelected) Modifier.border(1.dp, Color(0xFF424242), RoundedCornerShape(4.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Bounty List
            if (filteredBounties.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No bounties found.", color = Color.Gray)
                    }
                }
            } else {
                items(filteredBounties) { bounty ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        BountyCard(
                            bounty = bounty,
                            onDonateClick = { showDonateDialog = bounty },
                            primaryColor = NightColors.primary
                        )
                    }
                }
            }
        }

        if (showBuyCreditsDialog) {
            BuyCreditsDialog(
                billingManager = billingManager,
                onDismiss = { showBuyCreditsDialog = false }
            )
        }

        if (showDonateDialog != null) {
            DonateDialog(
                bounty = showDonateDialog!!,
                userCredits = userCredits,
                onConfirm = { amount ->
                    if (repository.donateToBounty(showDonateDialog!!.id, amount)) {
                        showDonateDialog = null
                    }
                },
                onDismiss = { showDonateDialog = null }
            )
        }
        
        if (showFaqDialog) {
            FeatureBountyFaqDialog(onDismiss = { showFaqDialog = false })
        }
    }
}

@Composable
fun BountyCard(bounty: FeatureBounty, onDonateClick: () -> Unit, primaryColor: Color) {
    val progress = if (bounty.goalCredits > 0) bounty.currentCredits.toFloat() / bounty.goalCredits else 0f
    val isActive = bounty.status == BountyStatus.ACTIVE

    Card(
        colors = CardDefaults.cardColors(containerColor = NightColors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bounty.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NightColors.onBackground
            )
            
            Spacer(Modifier.height(4.dp))
            Text(
                text = bounty.description,
                fontSize = 14.sp,
                color = NightColors.onSurface
            )
            
            if (isActive) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = primaryColor,
                    trackColor = Color.DarkGray
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${bounty.currentCredits} / ${bounty.goalCredits}",
                        color = NightColors.onSurface,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = onDonateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Donate")
                    }
                }
            }
        }
    }
}

@Composable
fun BuyCreditsDialog(billingManager: BillingManager, onDismiss: () -> Unit) {
    val productDetails by billingManager.productDetails.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NightColors.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Purchase Credits",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightColors.onBackground
                )
                Spacer(Modifier.height(16.dp))

                if (productDetails.isEmpty()) {
                    Text("Loading packages...", color = NightColors.onSurface)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productDetails) { product ->
                            val offer = product.oneTimePurchaseOfferDetails
                            val price = offer?.formattedPrice ?: "N/A"
                            // Extract bonus info logic here ideally
                            
                            CreditPackageCard(
                                title = product.name, // "120 Credits"
                                price = price,
                                onClick = {
                                    if (activity != null) {
                                        billingManager.launchBillingFlow(activity, product)
                                        // Dismiss handled by flow return or manual close
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = NightColors.onSurface)
                }
            }
        }
    }
}

@Composable
fun CreditPackageCard(title: String, price: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = NightColors.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, color = NightColors.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(price, color = NightColors.onSurface, fontSize = 14.sp)
        }
    }
}

@Composable
fun DonateDialog(
    bounty: FeatureBounty,
    userCredits: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("10") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NightColors.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Donate to ${bounty.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightColors.onBackground
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NightColors.onBackground,
                        unfocusedTextColor = NightColors.onBackground,
                        focusedBorderColor = NightColors.primary,
                        unfocusedBorderColor = NightColors.onSurface
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text("Available: $userCredits", color = NightColors.onSurface, fontSize = 12.sp)
                
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NightColors.onSurface)
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toIntOrNull() ?: 0
                            if (amount > 0 && amount <= userCredits) {
                                onConfirm(amount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NightColors.primary),
                        enabled = (amountText.toIntOrNull() ?: 0) in 1..userCredits
                    ) {
                        Text("Donate")
                    }
                }
            }
        }
    }
}    
@Composable
fun FeatureBountyFaqDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NightColors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NightColors.onSurface)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Icon + Title
                Icon(
                    imageVector = Icons.Default.Star, // Placeholder for target icon
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Feature Bounty FAQs",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightColors.onBackground
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = NightColors.primary,
                    thickness = 2.dp
                )
                
                Text(
                    text = "The Feature Bounty system was designed to allow the community to support the development of features that are not on the roadmap for NoxVision by purchasing credits that can be applied to the features you're interested in.",
                    fontSize = 16.sp,
                    color = NightColors.onSurface
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Why did you take this approach?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightColors.onBackground
                )
                
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "The community continually requests amazing feature ideas that exceed my ability to deliver on all of them. Instead of never getting to these ideas, and to help bring in additional revenue to support the development of NoxVision, I wanted to provide an option for the community to prioritize specific features to the top of the development roadmap.",
                    fontSize = 16.sp,
                    color = NightColors.onSurface
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Will all new features be going through this system?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightColors.onBackground
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "No. The core roadmap features will still be developed as planned. Bounties are for extra features that are highly requested but not currently prioritized.",
                    fontSize = 16.sp,
                    color = NightColors.onSurface
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
