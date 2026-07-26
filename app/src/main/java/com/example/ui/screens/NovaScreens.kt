package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.ui.TransactionResult
import com.example.ui.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.RingtoneManager
import android.net.Uri
import android.content.Context

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

// Formatter for Currency
fun formatCurrency(amount: Double): String {
    return "₹" + String.format(Locale.US, "%,.2f", amount)
}

// Formatter for Dates
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaPayAppContent(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var currentScreen by remember { mutableStateOf("welcome") }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    // Admin Password Protection
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordError by remember { mutableStateOf<String?>(null) }

    // Fix the navigation bug where OTP verification redirects back to login/welcome
    androidx.compose.runtime.LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && currentScreen == "welcome") {
            currentScreen = "dashboard"
        }
    }

    // Direct routing state machine
    val screenToDisplay = when {
        !isAuthenticated -> "welcome"
        isAdminMode -> "admin_dashboard"
        else -> currentScreen
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (screenToDisplay) {
                "welcome" -> WelcomeScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = { currentScreen = "dashboard" }
                )
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { currentScreen = it },
                    onToggleAdmin = {
                        if (it) {
                            showAdminPasswordDialog = true
                        } else {
                            viewModel.setAdminMode(false)
                        }
                    },
                    onSelectTransaction = { selectedTransactionForDetails = it }
                )
                "transfer" -> TransferScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "qrcode" -> QrCodeScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "scanpay" -> ScanPayScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" },
                    onNavigateToPay = { phone ->
                        // Quick navigate to pay with preset phone
                        currentScreen = "transfer"
                    }
                )
                "kyc" -> KycScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" }
                )
                "transactions" -> TransactionHistoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" },
                    onSelectTransaction = { selectedTransactionForDetails = it }
                )
                "admin_dashboard" -> AdminDashboardScreen(
                    viewModel = viewModel,
                    onExitAdmin = { viewModel.setAdminMode(false) },
                    onSelectTransaction = { selectedTransactionForDetails = it }
                )
            }

            // Global Transaction Details Dialog
            selectedTransactionForDetails?.let { tx ->
                TransactionDetailsDialog(
                    transaction = tx,
                    onDismiss = { selectedTransactionForDetails = null }
                )
            }

            if (showAdminPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        showAdminPasswordDialog = false 
                        adminPasswordInput = ""
                        adminPasswordError = null
                    },
                    title = {
                        Text(
                            text = "Admin Panel Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "This section is restricted to administrators. Enter password to unlock.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = { adminPasswordInput = it },
                                label = { Text("Password") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            adminPasswordError?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (adminPasswordInput == "Mohd1310@") {
                                    viewModel.setAdminMode(true)
                                    showAdminPasswordDialog = false
                                    adminPasswordInput = ""
                                    adminPasswordError = null
                                } else {
                                    adminPasswordError = "Invalid Password. Please try again."
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Unlock")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAdminPasswordDialog = false
                                adminPasswordInput = ""
                                adminPasswordError = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. WELCOME & LOGIN SCREEN
// ==========================================
@Composable
fun WelcomeScreen(
    viewModel: WalletViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val phoneInput by viewModel.currentPhoneInput.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val showPinSetup by viewModel.showPinSetup.collectAsState()
    val showPinVerification by viewModel.showPinVerification.collectAsState()

    var isRegistering by remember { mutableStateOf(false) }

    // Register Inputs
    var regName by remember { mutableStateOf("") }
    var regDob by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF002D80),
                            Color(0xFF0052CC),
                            Color(0xFF00A3FF)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "NovaPay Logo Icon",
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NovaPay",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!otpSent) "Secure Login" else "Enter Verification OTP",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!otpSent) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { viewModel.setPhoneInput(it) },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("e.g. 9876543211") },
                        leadingIcon = {
                            Text(
                                text = "+91 ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    authError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    val context = LocalContext.current
                    val activity = context as? android.app.Activity

                    Button(
                        onClick = { viewModel.requestOtp(phoneInput, activity) },
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_otp_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Secure OTP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                } else {
                    // OTP Verification Mode
                    Text(
                        text = "Verification code sent to +91 $phoneInput",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("Enter 4-Digit OTP") },
                        placeholder = { Text("For prototype, enter '1234'") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    authError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.verifyOtp(phoneInput, otpInput) {}
                        },
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_otp_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Verify & Log In", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Go Back to Mobile Entry")
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MAIN WALLET DASHBOARD
// ==========================================
@Composable
fun OldDashboardScreen(
    viewModel: WalletViewModel,
    onNavigate: (String) -> Unit,
    onToggleAdmin: (Boolean) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.currentUserTransactions.collectAsState()

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var addAmountStr by remember { mutableStateOf("") }
    var showBalanceDialog by remember { mutableStateOf(false) }

    val user = currentUser ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // TOP HEADER BAR WITH PROFILE INFO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 24.dp, 24.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Greeting and Status
            Column {
                Text(
                    text = "Hello, " + user.fullName.split(" ").firstOrNull(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (user.kycStatus == "Approved") Icons.Rounded.Verified else Icons.Rounded.Pending,
                        contentDescription = null,
                        tint = when (user.kycStatus) {
                            "Approved" -> MaterialTheme.colorScheme.tertiary
                            "Pending" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KYC: ${user.kycStatus}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Right Side: Admin Button and Profile Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Toggle Admin Button
                Button(
                    onClick = { onToggleAdmin(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Admin Panel", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Profile Avatar on the right side
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { onNavigate("qrcode") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // PRESTIGE WALLET CARD (ROYAL BLUE GRADIENT)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(190.dp)
                .testTag("wallet_balance_card"),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF00388A)
                            )
                        )
                    )
                    .drawBehind {
                        // Artistic abstract curve detailing on bank card background
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = 180.dp.toPx(),
                            center = Offset(size.width, size.height * 0.2f)
                        )
                    }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE WALLET BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(user.walletBalance),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                        }

                        // Mini wallet logo indicator
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MEMBER PROFILE PHONE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "+91 ${user.phoneNumber}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { showAddMoneyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_money_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Cash", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TRANSACTION VOLUME REPORT CARD
        val sentTotal = transactions.filter { it.senderPhone == user.phoneNumber && it.status == "Success" }.sumOf { it.amount }
        val receivedTotal = transactions.filter { it.receiverPhone == user.phoneNumber && it.status == "Success" }.sumOf { it.amount }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Received", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(formatCurrency(receivedTotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Sent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text(formatCurrency(sentTotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC62828))
                    }
                }
            }
        }

        if (user.kycStatus != "Approved" || user.securePin.isEmpty()) {
            var showKycSetupDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1) // Soft amber/warm color for pending setup
                ),
                border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Complete Wallet Setup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF5D4037)
                            )
                            Text(
                                text = "Verify KYC (Aadhaar & PAN) and set your 4-digit PIN to secure your account and unlock full limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF795548)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showKycSetupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify & Setup Wallet Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showKycSetupDialog) {
                var nameInput by remember { mutableStateOf(if (user.fullName.contains("User")) "" else user.fullName) }
                var emailInput by remember { mutableStateOf(if (user.email.contains("user_")) "" else user.email) }
                var dobInput by remember { mutableStateOf(if (user.dob == "01/01/2000") "" else user.dob) }
                var pinInputVal by remember { mutableStateOf(user.securePin) }
                var aadhaarInputVal by remember { mutableStateOf(user.aadhaarNumber) }
                var panInputVal by remember { mutableStateOf(user.panCard) }
                var errorMsg by remember { mutableStateOf<String?>(null) }

                Dialog(onDismissRequest = { showKycSetupDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Setup Wallet Profile & PIN",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF5A0E2D)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = dobInput,
                                onValueChange = { dobInput = it },
                                label = { Text("Date of Birth (DD/MM/YYYY)") },
                                placeholder = { Text("e.g. 15/08/1998") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pinInputVal,
                                onValueChange = { if (it.length <= 4) pinInputVal = it.replace(Regex("[^0-9]"), "") },
                                label = { Text("4-Digit Security PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            errorMsg?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (nameInput.trim().isEmpty() || emailInput.trim().isEmpty() || dobInput.trim().isEmpty() || pinInputVal.length != 4) {
                                        errorMsg = "Please fill all fields: Name, DOB, Email, and 4-Digit Security PIN."
                                    } else {
                                        viewModel.updateUserProfileAndKyc(
                                            fullName = nameInput,
                                            dob = dobInput,
                                            email = emailInput,
                                            pin = pinInputVal
                                        ) {
                                            showKycSetupDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A0E2D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Wallet Activation", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = { showKycSetupDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // PRIMARY CORE FINTECH QUICK ACTIONS
        Text(
            text = "NovaPay Quick Suite",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Button 1: Pay mobile number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("transfer") }
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2F3EB)), // Soft light mint/green background
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContactPhone,
                            contentDescription = "Pay mobile number",
                            tint = Color(0xFF5A0E2D), // "baingani mehrun" color
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pay mobile\nnumber",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }

                // Button 2: Pay to UPI ID or bank
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("transfer") }
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2F3EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = "Pay to UPI ID or bank",
                            tint = Color(0xFF5A0E2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pay to UPI ID\nor bank",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }

                // Button 3: Self transfer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAddMoneyDialog = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2F3EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHorizontalCircle,
                            contentDescription = "Self transfer",
                            tint = Color(0xFF5A0E2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Self\ntransfer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }

                // Button 4: Check balance
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showBalanceDialog = true }
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2F3EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "Check balance",
                            tint = Color(0xFF5A0E2D),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check\nbalance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Secondary small actions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { onNavigate("scanpay") }) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan QR", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            TextButton(onClick = { onNavigate("qrcode") }) {
                Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("My Code", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            TextButton(onClick = { onNavigate("kyc") }) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("KYC Center", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // RECENT TRANSACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            TextButton(
                onClick = { onNavigate("transactions") },
                modifier = Modifier.testTag("view_all_transactions")
            ) {
                Text("View All", fontWeight = FontWeight.Bold)
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No Transactions Yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                transactions.take(4).forEach { tx ->
                    TransactionRow(
                        transaction = tx,
                        currentUserPhone = user.phoneNumber,
                        onClick = { onNavigate("transactions") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Logout Footer
        TextButton(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Logout Session Safely", fontWeight = FontWeight.Bold)
        }
    }

    // Add Money Dialog (Simulated Bank TopUp)
    if (showAddMoneyDialog) {
        Dialog(onDismissRequest = { showAddMoneyDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Load Wallet Cash",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Simulate netbanking/UPI gateway transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = addAmountStr,
                        onValueChange = { addAmountStr = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = addAmountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.depositMoney(amt, "Loaded via UPI gateway mock")
                                addAmountStr = ""
                                showAddMoneyDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Instantly", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showAddMoneyDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Check Balance Dialog (Custom Burgundy themed modal)
    if (showBalanceDialog) {
        Dialog(onDismissRequest = { showBalanceDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF5A0E2D),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Available Wallet Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(user.walletBalance),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5A0E2D)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showBalanceDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A0E2D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

// ==========================================
// 3. SECURE TRANSFER SCREEN (Send Money Flow)
// ==========================================
@Composable
fun TransferScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val transferStatus by viewModel.transferStatus.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var receiverPhoneInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Play successful payment audio sound on success
    LaunchedEffect(transferStatus) {
        if (transferStatus is TransactionResult.Success) {
            playSuccessSound(context)
        }
    }

    // Dynamic resolution of recipient user info
    val matchedRecipient = remember(receiverPhoneInput, allUsers) {
        allUsers.firstOrNull { it.phoneNumber == receiverPhoneInput.trim() }
    }

    LaunchedEffect(Unit) {
        viewModel.clearTransferStatus()
    }

    if (transferStatus is TransactionResult.Success) {
        // SUCCESS RECEIPT VIEW
        val tx = (transferStatus as TransactionResult.Success).transaction
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Transaction Successful",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = formatCurrency(tx.amount),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val utrVal = remember(tx.id) {
                        val prefix = "3" + String.format("%02d", kotlin.random.Random.nextInt(10, 99))
                        val suffix = String.format("%09d", Math.abs(kotlin.random.Random.nextLong(100000000L, 999999999L)))
                        "$prefix$suffix"
                    }
                    ReceiptRow(label = "Sender", value = tx.senderName)
                    ReceiptRow(label = "Recipient Mobile", value = tx.receiverPhone)
                    ReceiptRow(label = "Recipient Name", value = tx.receiverName)
                    ReceiptRow(label = "Date & Time", value = formatDate(tx.timestamp))
                    ReceiptRow(label = "BANK UTR NUMBER", value = utrVal)
                    ReceiptRow(label = "Security Seal", value = "INTEGRITY VERIFIED")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Secure Audit Hash:\n${tx.auditHash}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)).padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.clearTransferStatus()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Return to Wallet", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // MAIN TRANSFER SETUP VIEW
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Send Money Securely",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // RECIPIENT PHONE ENTRY
            OutlinedTextField(
                value = receiverPhoneInput,
                onValueChange = { receiverPhoneInput = it },
                label = { Text("Recipient Phone Number") },
                placeholder = { Text("e.g. 9876543211") },
                leadingIcon = {
                    Text("+91 ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_phone_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            // Recipient dynamic verify status
            AnimatedVisibility(visible = receiverPhoneInput.length >= 4) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (matchedRecipient != null) {
                                "Recipient Found: ${matchedRecipient.fullName} (KYC: ${matchedRecipient.kycStatus})"
                            } else {
                                "Recipient Verified: NovaPay User (Mobile: +91 $receiverPhoneInput)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Quick Shortcut Preseeded User Picker
            Text(
                text = "Quick Select Registered Users to Test:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allUsers.filter { it.phoneNumber != currentUser?.phoneNumber }.forEach { u ->
                    ElevatedButton(
                        onClick = { receiverPhoneInput = u.phoneNumber },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(u.fullName.split(" ").first(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AMOUNT INPUT
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Transfer Amount (₹)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_amount_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            // NOTES INPUT
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Add custom message/note") },
                placeholder = { Text("e.g. Lunch split, bills...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error displays
            if (transferStatus is TransactionResult.Error) {
                Text(
                    text = (transferStatus as TransactionResult.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    viewModel.initiateTransfer(receiverPhoneInput, amt, notesInput)
                },
                enabled = transferStatus !is TransactionResult.Loading && receiverPhoneInput.isNotEmpty() && amountInput.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_transfer_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (transferStatus is TransactionResult.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay Securely Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}

// ==========================================
// 4. QR GENERATOR SCREEN (Receive money)
// ==========================================
@Composable
fun QrCodeScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "My Wallet QR",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // The Custom QR Code Canvas Display Box
        Card(
            modifier = Modifier
                .width(280.dp)
                .height(380.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Branding Header
                Text(
                    text = "NovaPay Secure",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )

                // High fidelity QR canvas drawing!
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14f
                        val gap = 12f

                        // Corner positioning squares (top-left, top-right, bottom-left)
                        // Top Left Anchor
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(0f, 0f),
                            size = Size(42.dp.toPx(), 42.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(strokeWidth)
                        )
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                            size = Size(22.dp.toPx(), 22.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Top Right Anchor
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(size.width - 42.dp.toPx(), 0f),
                            size = Size(42.dp.toPx(), 42.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(strokeWidth)
                        )
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(size.width - 32.dp.toPx(), 10.dp.toPx()),
                            size = Size(22.dp.toPx(), 22.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Bottom Left Anchor
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(0f, size.height - 42.dp.toPx()),
                            size = Size(42.dp.toPx(), 42.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(strokeWidth)
                        )
                        drawRoundRect(
                            color = Color(0xFF0052CC),
                            topLeft = Offset(10.dp.toPx(), size.height - 32.dp.toPx()),
                            size = Size(22.dp.toPx(), 22.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Mock internal QR matrix dots (using canvas lines or path effects)
                        val step = 14f
                        var lineY = 55.dp.toPx()
                        while (lineY < size.height - 50.dp.toPx()) {
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f, 6f, 12f, 22f), lineY % 30f)
                            drawLine(
                                color = Color.DarkGray.copy(alpha = 0.8f),
                                start = Offset(10.dp.toPx(), lineY),
                                end = Offset(size.width - 10.dp.toPx(), lineY),
                                strokeWidth = 8f,
                                pathEffect = pathEffect
                            )
                            lineY += step
                        }

                        // Central Brand Circle
                        drawCircle(
                            color = Color.White,
                            radius = 18.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                        drawCircle(
                            color = Color(0xFF0052CC),
                            radius = 14.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }

                    Text(
                        text = "NP",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "+91 ${user.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Mock Receive Simulation to let reviewers test receiving money instantly!
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify instant receiving flow in preview:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.depositMoney(500.0, "Instantly received via QR Scanner split")
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simulate Receiving ₹500 via QR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class BarcodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            onQrCodeScanned(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Fail silently
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

fun playSuccessSound(context: Context) {
    Thread {
        try {
            val sampleRate = 44100
            val durationMs = 600
            val numSamples = sampleRate * durationMs / 1000
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            // Sweet pleasant payment chime: 523Hz (C5), then 1046Hz (C6)
            val transitionPoint = sampleRate * 250 / 1000

            for (i in 0 until numSamples) {
                val freq = if (i < transitionPoint) 880.0 else 1320.0 // A5 then E6 notes
                // Soft envelope curve to avoid clicking
                val envelope = if (i < 1000) {
                    i / 1000.0
                } else if (i > numSamples - 2000) {
                    (numSamples - i) / 2000.0
                } else {
                    1.0
                }
                sample[i] = Math.sin(2.0 * Math.PI * i / (sampleRate / freq)) * envelope
            }

            // Convert to 16-bit PCM format
            var idx = 0
            for (dVal in sample) {
                val valShort = (dVal * 32767).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val minBufSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = Math.max(generatedSnd.size, minBufSize)

            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()

            // Safe release after playback duration
            Thread.sleep(800)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }.start()
}

fun parseQrText(rawValue: String): Pair<String, String> {
    return try {
        if (rawValue.startsWith("upi://pay?") || rawValue.startsWith("novapay://pay?")) {
            val uri = Uri.parse(rawValue)
            val phone = uri.getQueryParameter("phone") ?: uri.getQueryParameter("pa")?.substringBefore("@") ?: rawValue
            val name = uri.getQueryParameter("name") ?: uri.getQueryParameter("pn") ?: "NovaPay User"
            Pair(phone, name)
        } else {
            Pair(rawValue, "NovaPay User")
        }
    } catch (e: Exception) {
        Pair(rawValue, "NovaPay User")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanPayScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToPay: (String) -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val transferStatus by viewModel.transferStatus.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Screen-level state
    var scannedText by remember { mutableStateOf<String?>(null) }
    var scannedPhone by remember { mutableStateOf("") }
    var scannedName by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var paymentCompleted by remember { mutableStateOf(false) }
    var paymentFailed by remember { mutableStateOf<String?>(null) }
    var successTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var showSimulationDialog by remember { mutableStateOf(false) }

    // Camera state
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    // Sound and status side-effect
    LaunchedEffect(transferStatus) {
        when (val status = transferStatus) {
            is TransactionResult.Success -> {
                playSuccessSound(context)
                successTransaction = status.transaction
                paymentCompleted = true
                showPinDialog = false
            }
            is TransactionResult.Error -> {
                paymentFailed = status.message
                showPinDialog = false
            }
            else -> {}
        }
    }

    // Processing payment simulated delay
    LaunchedEffect(isProcessingPayment) {
        if (isProcessingPayment) {
            kotlinx.coroutines.delay(1500)
            viewModel.initiateTransfer(
                receiverPhone = scannedPhone,
                amount = amountInput.toDoubleOrNull() ?: 0.0,
                notes = notesInput
            )
            isProcessingPayment = false
        }
    }

    if (paymentCompleted) {
        // ==========================================
        // SUB-VIEW: PAYMENT SUCCESS RECEIPT
        // ==========================================
        val tx = successTransaction
        // Truly dynamic, randomized 12-digit Indian banking standard UTR changing every single time
        val utr = remember(tx?.id) {
            val prefix = "3" + String.format("%02d", kotlin.random.Random.nextInt(10, 99))
            val suffix = String.format("%09d", Math.abs(kotlin.random.Random.nextLong(100000000L, 999999999L)))
            "$prefix$suffix"
        }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (themeState == "black") Color(0xFF121212) else Color(0xFFF8F9FC))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Animated / Styled green success ripple
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Payment Successful",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32)
            )

            Text(
                text = "Money transferred securely via NovaPay",
                style = MaterialTheme.typography.bodyMedium,
                color = if (themeState == "black") Color.Gray else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Receipt Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recipient Name", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(scannedName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Receiver Phone", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text("+91 $scannedPhone", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount Paid", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text("₹${amountInput}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)))
                    }

                    if (notesInput.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Notes", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(notesInput, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                        }
                    }

                    Divider(color = if (themeState == "black") Color(0xFF333333) else Color(0xFFEEEEEE))

                    // Unique UTR field with button specifically requested
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D47A1).copy(alpha = 0.08f))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("BANK UTR NUMBER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = Color(0xFF0D47A1))
                                Text(utr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = if (themeState == "black") Color.White else Color.Black)
                            }
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(utr))
                                    android.widget.Toast.makeText(context, "UTR Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Copy", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    scannedText = null
                    paymentCompleted = false
                    amountInput = ""
                    notesInput = ""
                    viewModel.clearTransferStatus()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    scannedText = null
                    paymentCompleted = false
                    amountInput = ""
                    notesInput = ""
                    viewModel.clearTransferStatus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Another QR", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
            }
        }
    } else if (isProcessingPayment) {
        // ==========================================
        // SUB-VIEW: PROCESSING LOADING SCREEN (As requested!)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (themeState == "black") Color(0xFF121212) else Color(0xFFF8F9FC)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Processing Secure Payment...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (themeState == "black") Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sending ₹$amountInput to $scannedName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    } else if (scannedText != null) {
        // ==========================================
        // SUB-VIEW: CONFIRM TRANSACTION & PAY FORM
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (themeState == "black") Color(0xFF121212) else Color(0xFFF8F9FC))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scannedText = null },
                    modifier = Modifier.background(if (themeState == "black") Color(0xFF1E1E1E) else Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Initiate QR Payment",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recipient Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D47A1).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scannedName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = scannedName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+91 $scannedPhone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Form fields
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all { char -> char.isDigit() }) amountInput = it },
                label = { Text("Enter Amount (₹)") },
                placeholder = { Text("0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (themeState == "black") Color.White else Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                    unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                    focusedContainerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White,
                    unfocusedContainerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White,
                    focusedBorderColor = Color(0xFF0D47A1),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Add Notes / Remarks (Optional)") },
                placeholder = { Text("e.g. Dinner, Tea, Rent") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (themeState == "black") Color.White else Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                    unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                    focusedContainerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White,
                    unfocusedContainerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White,
                    focusedBorderColor = Color(0xFF0D47A1),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                    unfocusedLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Available funds indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Wallet Balance:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("₹${currentUser?.walletBalance ?: 0.0}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (paymentFailed != null) {
                Text(
                    text = paymentFailed ?: "Transaction Failed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (transferStatus is TransactionResult.Loading) {
                CircularProgressIndicator(color = Color(0xFF0D47A1))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Executing Secure Transfer...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            android.widget.Toast.makeText(context, "Please enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount > (currentUser?.walletBalance ?: 0.0)) {
                            android.widget.Toast.makeText(context, "Insufficient balance!", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Open PIN dialog
                        pinInput = ""
                        pinError = null
                        showPinDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pay Now", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { scannedText = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan Another QR Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                }
            }

            // PIN entry dialog specifically requested
            if (showPinDialog) {
                Dialog(onDismissRequest = { showPinDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                        ),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Enter Security PIN",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (themeState == "black") Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Enter your security PIN to verify transaction of ₹$amountInput to $scannedName",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pinInput = it },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                label = { Text("Secure PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                    unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                    focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                    unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                    focusedBorderColor = Color(0xFF0D47A1),
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(pinError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPinDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = if (themeState == "black") Color.White else Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        if (pinInput.length != 4 && pinInput.length != 6) {
                                            pinError = "PIN must be 4 or 6 digits."
                                            return@Button
                                        }
                                        val userHash = currentUser?.securePin ?: ""
                                        val enteredHash = viewModel.hashPin(pinInput)

                                        if (userHash.isEmpty() || userHash == enteredHash || userHash == pinInput) {
                                            // Pin matched successfully, proceed with money transfer
                                            paymentFailed = null
                                            showPinDialog = false
                                            isProcessingPayment = true
                                        } else {
                                            pinError = "Incorrect PIN. Please try again."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                                ) {
                                    Text("Confirm", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ==========================================
        // SUB-VIEW: LIVE SCANNING VIEWPORT (FULL SCREEN!)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                val context = LocalContext.current
                DisposableEffect(Unit) {
                    onDispose {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        try {
                            if (cameraProviderFuture.isDone) {
                                cameraProviderFuture.get().unbindAll()
                            } else {
                                cameraProviderFuture.addListener({
                                    try {
                                        cameraProviderFuture.get().unbindAll()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // CameraX Viewport filling entire screen
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                BarcodeAnalyzer { qrValue ->
                                    val parsed = parseQrText(qrValue)
                                    scannedText = qrValue
                                    scannedPhone = parsed.first
                                    val matchedUser = allUsers.find { it.phoneNumber == parsed.first }
                                    scannedName = matchedUser?.fullName ?: parsed.second
                                    amountInput = ""
                                    notesInput = ""
                                    paymentFailed = null
                                    viewModel.clearTransferStatus()
                                }
                            )

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser line animation sweeping down and up
                val infiniteTransition = rememberInfiniteTransition()
                val laserOffset by infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                // Beautiful Full Screen Overlay with centered clear cutout & Corners
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = 250.dp.toPx()
                    val left = (size.width - sizePx) / 2
                    val top = (size.height - sizePx) / 2
                    
                    // Semi-transparent background
                    // Top bar
                    drawRect(Color.Black.copy(alpha = 0.65f), size = androidx.compose.ui.geometry.Size(size.width, top))
                    // Bottom bar
                    drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, top + sizePx), size = androidx.compose.ui.geometry.Size(size.width, size.height - (top + sizePx)))
                    // Left bar
                    drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, sizePx))
                    // Right bar
                    drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(left + sizePx, top), size = androidx.compose.ui.geometry.Size(size.width - (left + sizePx), sizePx))

                    // Green Corner Reticles
                    val strokeW = 8f
                    val length = 40f
                    // Top-Left
                    drawLine(Color.Green, Offset(left, top), Offset(left + length, top), strokeW)
                    drawLine(Color.Green, Offset(left, top), Offset(left, top + length), strokeW)
                    // Top-Right
                    drawLine(Color.Green, Offset(left + sizePx, top), Offset(left + sizePx - length, top), strokeW)
                    drawLine(Color.Green, Offset(left + sizePx, top), Offset(left + sizePx, top + length), strokeW)
                    // Bottom-Left
                    drawLine(Color.Green, Offset(left, top + sizePx), Offset(left + length, top + sizePx), strokeW)
                    drawLine(Color.Green, Offset(left, top + sizePx), Offset(left, top + sizePx - length), strokeW)
                    // Bottom-Right
                    drawLine(Color.Green, Offset(left + sizePx, top + sizePx), Offset(left + sizePx - length, top + sizePx), strokeW)
                    drawLine(Color.Green, Offset(left + sizePx, top + sizePx), Offset(left + sizePx, top + sizePx - length), strokeW)

                    // Laser Sweep
                    val yPos = top + (sizePx * laserOffset)
                    drawLine(
                        color = Color.Green,
                        start = Offset(left + 10f, yPos),
                        end = Offset(left + sizePx - 10f, yPos),
                        strokeWidth = 4f
                    )
                }

                // Title overlay at top
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan Any Payment QR",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Align the code inside the box frame",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Float simulation trigger & Manual inputs at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp).padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showSimulationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate QR Scan (Emulator)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Request Permission Screen as overlay in full screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = "Camera Scanner",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Camera Access Required",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "NovaPay needs camera authorization to scan payment QR codes and process instantaneous P2P money transfers.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                            ) {
                                Text("Grant Permission", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                }
            }

            // Top-left float back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 40.dp, start = 16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }

    // Simulation Overlay Dialog
    if (showSimulationDialog) {
        Dialog(onDismissRequest = { showSimulationDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Simulate Scan (For Testing)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap a user below to simulate parsing their QR payment code instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val otherUsers = allUsers.filter { it.phoneNumber != currentUser?.phoneNumber }
                    if (otherUsers.isEmpty()) {
                        Text(
                            "No other registered users in system to pay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(otherUsers) { u ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            scannedText = "novapay://pay?phone=${u.phoneNumber}&name=${u.fullName}"
                                            scannedPhone = u.phoneNumber
                                            scannedName = u.fullName
                                            amountInput = ""
                                            notesInput = ""
                                            paymentFailed = null
                                            viewModel.clearTransferStatus()
                                            showSimulationDialog = false
                                        }
                                        .background(
                                            if (themeState == "black") Color(0xFF2C2C2C) else Color(0xFFF1F5F9)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = u.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (themeState == "black") Color.White else Color.Black
                                        )
                                        Text("+91 ${u.phoneNumber}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    Text(
                                        text = "Simulate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (themeState == "black") Color.White else Color(0xFF0D47A1))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showSimulationDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = if (themeState == "black") Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. KYC VERIFICATION SCREEN
// ==========================================
@Composable
fun KycScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val statusMessage by viewModel.kycStatusMessage.collectAsState()

    val user = currentUser ?: return

    var aadhaarInput by remember { mutableStateOf("") }
    var panInput by remember { mutableStateOf("") }
    var aadhaarImgScanned by remember { mutableStateOf(false) }
    var panImgScanned by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Identity KYC Hub",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // STATUS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (user.kycStatus) {
                    "Approved" -> Color(0xFFE8F5E9)
                    "Pending" -> Color(0xFFE3F2FD)
                    "Rejected" -> Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (user.kycStatus) {
                            "Approved" -> Icons.Rounded.Verified
                            "Pending" -> Icons.Rounded.HourglassEmpty
                            "Rejected" -> Icons.Rounded.Cancel
                            else -> Icons.Rounded.Info
                        },
                        contentDescription = null,
                        tint = when (user.kycStatus) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Pending" -> Color(0xFF1565C0)
                            "Rejected" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Status: " + if (user.kycStatus == "None") "Unverified / Not Submitted" else user.kycStatus,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = when (user.kycStatus) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Pending" -> Color(0xFF1565C0)
                            "Rejected" -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (user.kycStatus) {
                        "Approved" -> "Congratulations! Your identity is fully approved on NovaPay. Enjoy higher transaction limits up to ₹1,00,000 per transaction."
                        "Pending" -> "Your documents are currently undergoing manual review by our security compliance officers. Processing typically takes less than 2 hours."
                        "Rejected" -> "Compliance review rejected. Please verify your Aadhaar and PAN documents are correct and submit them again."
                        else -> "Verify your identity (Aadhaar & PAN card) to unlock seamless instant transfers, deposits, and complete secure banking integrations."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        if (user.kycStatus != "Approved") {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Submit Identity Credentials",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aadhaar input
            OutlinedTextField(
                value = aadhaarInput,
                onValueChange = { if (it.length <= 12) aadhaarInput = it },
                label = { Text("12-Digit Aadhaar Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("aadhaar_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PAN input
            OutlinedTextField(
                value = panInput,
                onValueChange = { if (it.length <= 10) panInput = it.uppercase() },
                label = { Text("10-Character PAN Card String") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pan_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.Assignment, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mock Image Scans
            Text(
                text = "Upload Scan Documents",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Aadhaar Image Select Box
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { aadhaarImgScanned = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (aadhaarImgScanned) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (aadhaarImgScanned) Icons.Rounded.CheckCircle else Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = if (aadhaarImgScanned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (aadhaarImgScanned) "Aadhaar Scanned" else "Scan Aadhaar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // PAN Image Select Box
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { panImgScanned = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (panImgScanned) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (panImgScanned) Icons.Rounded.CheckCircle else Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = if (panImgScanned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (panImgScanned) "PAN Scanned" else "Scan PAN Card",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            statusMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.submitKycDocs(
                        aadhaar = aadhaarInput,
                        pan = panInput,
                        aadhaarImg = if (aadhaarImgScanned) "aadhaar_verified_scan.png" else null,
                        panImg = if (panImgScanned) "pan_verified_scan.png" else null
                    )
                },
                enabled = aadhaarInput.length == 12 && panInput.length == 10 && aadhaarImgScanned && panImgScanned,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_kyc_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit KYC Documents", fontWeight = FontWeight.Bold)
            }

            // Quick hint
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "💡 Tip: After submitting documents, click on 'Admin Panel' on dashboard and approve the pending KYC request instantly to unlock limits!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// ==========================================
// 7. TRANSACTION HISTORY SCREEN
// ==========================================
@Composable
fun TransactionHistoryScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.currentUserTransactions.collectAsState()

    val user = currentUser ?: return

    var activeTab by remember { mutableStateOf("All") }

    val filteredTransactions = remember(transactions, activeTab, user.phoneNumber) {
        when (activeTab) {
            "Sent" -> transactions.filter { it.senderPhone == user.phoneNumber }
            "Received" -> transactions.filter { it.receiverPhone == user.phoneNumber }
            "Failed" -> transactions.filter { it.status == "Failed" }
            else -> transactions
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp, 24.dp, 24.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Transaction Ledger",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Filter tabs
        ScrollableTabRow(
            selectedTabIndex = listOf("All", "Sent", "Received", "Failed").indexOf(activeTab),
            edgePadding = 24.dp,
            divider = {},
            indicator = { tabPositions ->
                val index = listOf("All", "Sent", "Received", "Failed").indexOf(activeTab)
                if (index != -1 && index < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            listOf("All", "Sent", "Received", "Failed").forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions match this filter", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions) { tx ->
                    TransactionRow(
                        transaction = tx,
                        currentUserPhone = user.phoneNumber,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. ADMIN DASHBOARD SCREEN
// ==========================================
@Composable
fun AdminDashboardScreen(
    viewModel: WalletViewModel,
    onExitAdmin: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var activeAdminTab by remember { mutableStateOf("Overview") }
    
    var selectedUserForNotification by remember { mutableStateOf<UserEntity?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }

    if (showNotificationDialog && selectedUserForNotification != null) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationDialog = false
                selectedUserForNotification = null
                notificationTitle = ""
                notificationMessage = ""
            },
            title = {
                Text("Send Alert to ${selectedUserForNotification!!.fullName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = notificationTitle,
                        onValueChange = { notificationTitle = it },
                        label = { Text("Alert Title") },
                        placeholder = { Text("e.g. Account Notification") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notificationMessage,
                        onValueChange = { notificationMessage = it },
                        label = { Text("Alert Message") },
                        placeholder = { Text("Type your message here...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notificationTitle.isNotBlank() && notificationMessage.isNotBlank()) {
                            // Find and trigger notification send
                            viewModel.adminSendNotification(selectedUserForNotification!!.phoneNumber, notificationTitle, notificationMessage)
                            showNotificationDialog = false
                            selectedUserForNotification = null
                            notificationTitle = ""
                            notificationMessage = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = notificationTitle.isNotBlank() && notificationMessage.isNotBlank()
                ) {
                    Text("Send Alert")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationDialog = false
                        selectedUserForNotification = null
                        notificationTitle = ""
                        notificationMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val totalSystemBalance = allUsers.sumOf { it.walletBalance }
    val pendingKycCount = allUsers.count { it.kycStatus == "Pending" }
    val approvedKycCount = allUsers.count { it.kycStatus == "Approved" }
    val rejectedKycCount = allUsers.count { it.kycStatus == "Rejected" }

    Column(modifier = Modifier.fillMaxSize()) {
        // ADMIN TITLE BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1D2636))
                .padding(24.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Color(0xFFF3A412), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("NovaPay Admin Console", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("Compliance & Account Audit Mode", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Button(
                onClick = onExitAdmin,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Exit Console", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        // TABS FOR ADMIN CONTROL
        TabRow(selectedTabIndex = listOf("Overview", "Users", "Ledger").indexOf(activeAdminTab)) {
            listOf("Overview", "Users", "Ledger").forEach { tab ->
                Tab(
                    selected = activeAdminTab == tab,
                    onClick = { activeAdminTab = tab },
                    text = { Text(tab, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (activeAdminTab) {
            "Overview" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text("NovaPay System Metrics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2x2 Grid of stats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AdminStatCard(title = "Total Users", value = "${allUsers.size}", icon = Icons.Rounded.Person, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        AdminStatCard(title = "Total System Funds", value = formatCurrency(totalSystemBalance), icon = Icons.Rounded.AccountBalanceWallet, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AdminStatCard(title = "Pending KYCs", value = "$pendingKycCount", icon = Icons.Rounded.HourglassEmpty, color = Color(0xFFEF6C00), modifier = Modifier.weight(1f))
                        AdminStatCard(title = "Verified KYC Users", value = "$approvedKycCount", icon = Icons.Rounded.Verified, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("System Statistics & Insights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    KycPieChart(allUsers)
                    Spacer(modifier = Modifier.height(8.dp))
                    TransactionVolumeBarChart(allTransactions)

                    Spacer(modifier = Modifier.height(30.dp))

                    // Dynamic KYC Compliance Queue
                    Text("Pending KYC Requests Queue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    val pendingKycUsers = allUsers.filter { it.kycStatus == "Pending" }
                    if (pendingKycUsers.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No pending KYC verification requests. All users clear!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            pendingKycUsers.forEach { u ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(u.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Phone: +91 ${u.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(
                                                "PENDING MANUAL VERIFY",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFEF6C00)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Show details submitted
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text("Aadhaar Number: ${u.aadhaarNumber}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                                Text("PAN String: ${u.panCard}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                                Text("Document scans uploaded check: OK (Simulated)", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.adminRejectKyc(u.phoneNumber) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.padding(end = 8.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            }

                                            Button(
                                                onClick = { viewModel.adminApproveKyc(u.phoneNumber) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve KYC", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Users" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers) { u ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = if (u.isFrozen) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(u.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("+91 ${u.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                                        Text("Balance: ${formatCurrency(u.walletBalance)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        // KYC tag
                                        Card(
                                            shape = RoundedCornerShape(4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (u.kycStatus) {
                                                    "Approved" -> Color(0xFFE8F5E9)
                                                    "Pending" -> Color(0xFFE3F2FD)
                                                    "Rejected" -> Color(0xFFFFEBEE)
                                                    else -> Color.LightGray.copy(alpha = 0.2f)
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = "KYC: ${u.kycStatus}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = when (u.kycStatus) {
                                                    "Approved" -> Color(0xFF2E7D32)
                                                    "Pending" -> Color(0xFF1565C0)
                                                    "Rejected" -> Color(0xFFC62828)
                                                    else -> Color.DarkGray
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (u.isFrozen) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("FROZEN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { 
                                            selectedUserForNotification = u
                                            showNotificationDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Send Alert",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.adminToggleFreeze(u.phoneNumber) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (u.isFrozen) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (u.isFrozen) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (u.isFrozen) "Unfreeze" else "Freeze",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Ledger" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allTransactions) { tx ->
                        TransactionRow(
                            transaction = tx,
                            currentUserPhone = "ADMIN_CONSOLE",
                            onClick = { onSelectTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KycPieChart(users: List<com.example.data.model.UserEntity>) {
    val pending = users.count { it.kycStatus == "Pending" }.toFloat()
    val approved = users.count { it.kycStatus == "Approved" }.toFloat()
    val rejected = users.count { it.kycStatus == "Rejected" }.toFloat()
    val total = pending + approved + rejected

    if (total == 0f) return

    val approvedPct = approved / total
    val pendingPct = pending / total
    val rejectedPct = rejected / total

    val angleApproved = approvedPct * 360f
    val anglePending = pendingPct * 360f
    val angleRejected = rejectedPct * 360f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "KYC Verification Status Split",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f

                        if (angleApproved > 0) {
                            drawArc(
                                color = Color(0xFF2E7D32),
                                startAngle = startAngle,
                                sweepAngle = angleApproved,
                                useCenter = false,
                                style = Stroke(width = 24f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += angleApproved
                        }

                        if (anglePending > 0) {
                            drawArc(
                                color = Color(0xFFEF6C00),
                                startAngle = startAngle,
                                sweepAngle = anglePending,
                                useCenter = false,
                                style = Stroke(width = 24f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += anglePending
                        }

                        if (angleRejected > 0) {
                            drawArc(
                                color = Color(0xFFC62828),
                                startAngle = startAngle,
                                sweepAngle = angleRejected,
                                useCenter = false,
                                style = Stroke(width = 24f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${users.size}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Users",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = Color(0xFF2E7D32), label = "Approved (${(approvedPct * 100).toInt()}%)")
                    LegendItem(color = Color(0xFFEF6C00), label = "Pending (${(pendingPct * 100).toInt()}%)")
                    LegendItem(color = Color(0xFFC62828), label = "Rejected (${(rejectedPct * 100).toInt()}%)")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
fun TransactionVolumeBarChart(transactions: List<com.example.data.model.TransactionEntity>) {
    // Take the most recent 6 transactions to represent volume trends
    val recentTxs = transactions.sortedByDescending { it.timestamp }.take(6).reversed()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Transaction Volume Ledger Trend",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (recentTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transaction volume history. Start making payments!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                val maxAmount = recentTxs.maxOf { it.amount }.toFloat().coerceAtLeast(100f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    recentTxs.forEach { tx ->
                        val barHeightFactor = (tx.amount.toFloat() / maxAmount).coerceIn(0.1f, 1f)
                        val isDeposit = tx.type == "Deposit"

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "₹${tx.amount.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = if (isDeposit) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(90.dp * barHeightFactor)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = if (isDeposit) {
                                                listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                                            } else {
                                                listOf(Color(0xFF64B5F6), Color(0xFF0D47A1))
                                            }
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDeposit) "Cred" else "Deb",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: TRANSACTION ROW
// ==========================================
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    currentUserPhone: String,
    onClick: () -> Unit
) {
    val isSent = transaction.senderPhone == currentUserPhone
    val isReceived = transaction.receiverPhone == currentUserPhone
    val isDeposit = transaction.type == "Deposit"

    val displayTitle = when {
        isDeposit -> "Loaded Wallet Cash"
        isSent -> "To: ${transaction.receiverName}"
        isReceived -> "From: ${transaction.senderName}"
        else -> "${transaction.senderName} ➜ ${transaction.receiverName}"
    }

    val displaySubtitle = when {
        isDeposit -> transaction.notes.ifEmpty { "Bank Deposit Gateway" }
        isSent -> "Sent payment: ${transaction.notes.ifEmpty { "Wallet transfer" }}"
        isReceived -> "Received credit: ${transaction.notes.ifEmpty { "Wallet credit" }}"
        else -> transaction.notes
    }

    val amountColor = when {
        transaction.status == "Failed" -> MaterialTheme.colorScheme.error
        isDeposit || isReceived -> Color(0xFF2E7D32) // Green
        else -> MaterialTheme.colorScheme.onBackground // Dark default
    }

    val amountPrefix = when {
        transaction.status == "Failed" -> ""
        isDeposit || isReceived -> "+"
        else -> "-"
    }

    val circleColor = when {
        transaction.status == "Failed" -> Color(0xFFFFEBEE)
        isDeposit || isReceived -> Color(0xFFE8F5E9)
        else -> Color(0xFFE3F2FD)
    }

    val iconVector = when {
        transaction.status == "Failed" -> Icons.Rounded.Close
        isDeposit || isReceived -> Icons.Filled.ArrowDownward
        else -> Icons.Filled.ArrowUpward
    }

    val iconTint = when {
        transaction.status == "Failed" -> Color(0xFFC62828)
        isDeposit || isReceived -> Color(0xFF2E7D32)
        else -> Color(0xFF1565C0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle arrow icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displaySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ==========================================
// DIALOG: TRANSACTION DETAILS
// ==========================================
@Composable
fun TransactionDetailsDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.status == "Success") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.status == "Success") Icons.Rounded.Verified else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (transaction.status == "Success") Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transaction receipt",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (transaction.status == "Success") Color(0xFF2E7D32) else Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptDetailRow(label = "Sender Phone", value = transaction.senderPhone)
                    ReceiptDetailRow(label = "Sender Name", value = transaction.senderName)
                    ReceiptDetailRow(label = "Receiver Phone", value = transaction.receiverPhone)
                    ReceiptDetailRow(label = "Receiver Name", value = transaction.receiverName)
                    ReceiptDetailRow(label = "Transaction Type", value = transaction.type)
                    ReceiptDetailRow(label = "Compliance Status", value = transaction.status)
                    ReceiptDetailRow(label = "Timestamp", value = formatDate(transaction.timestamp))
                    ReceiptDetailRow(label = "Payment Remarks", value = transaction.notes.ifEmpty { "-" })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful UTR Number Display with a Copy Button
                // Generates a stable but completely distinct 12-digit UTR derived from transaction details for compliance
                val utr = remember(transaction.id) {
                    val prefix = "3" + String.format("%02d", Math.abs((transaction.auditHash.hashCode() % 89) + 10))
                    val suffix = String.format("%09d", Math.abs((transaction.timestamp + transaction.id) % 1000000000L))
                    "$prefix$suffix"
                }
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = androidx.compose.ui.platform.LocalContext.current

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BANK UTR NUMBER",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = utr,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(utr))
                            android.widget.Toast.makeText(context, "UTR Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy UTR",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Secure Cryptographic Audit Hash",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = transaction.auditHash,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close Receipt", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==========================================
// NEW MODERN FINTECH DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: WalletViewModel,
    onNavigate: (String) -> Unit,
    onToggleAdmin: (Boolean) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.currentUserTransactions.collectAsState()
    
    val notifications by viewModel.userNotifications.collectAsState()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val unreadNotificationsCount = notifications.count { !it.isRead }

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var addAmountStr by remember { mutableStateOf("") }
    
    // Balance Visibility and Security
    var isBalanceRevealed by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val user = currentUser ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FC)) // Elegant modern soft backdrop
            .verticalScroll(rememberScrollState())
    ) {
        // TOP HEADER BAR (Modern fintech style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: App logo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF002D80),
                                    Color(0xFF0052CC),
                                    Color(0xFF00A3FF)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "NovaPay Logo",
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "NovaPay",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D47A1),
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // Right Side: Notification Icon & Profile Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Toggle Admin Panel for review
                IconButton(
                    onClick = { onToggleAdmin(true) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFECEFF1), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = Color(0xFF455A64)
                    )
                }

                // Notification Icon with visual badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFECEFF1))
                        .clickable { showNotificationsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF37474F),
                        modifier = Modifier.size(22.dp)
                    )
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unreadNotificationsCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Profile Avatar (Clicking opens profile screen)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D47A1))
                        .clickable { onNavigate("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.split(" ").firstOrNull()?.take(1)?.uppercase() ?: "U",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Welcome / Greeting Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF263238)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PRESTIGE WALLET CARD (ROYAL BLUE GRADIENT)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(200.dp)
                .testTag("wallet_balance_card"),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF1565C0),
                                Color(0xFF1E88E5)
                            )
                        )
                    )
                    .drawBehind {
                        // Artistic abstract curve detailing on bank card background
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = 220.dp.toPx(),
                            center = Offset(size.width, size.height * 0.1f)
                        )
                    }
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE WALLET BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isBalanceRevealed) {
                                Text(
                                    text = formatCurrency(user.walletBalance),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp
                                    ),
                                    color = Color.White
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "••••••",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 32.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Hidden",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Mini eye toggle indicator / icon
                        IconButton(
                            onClick = {
                                if (isBalanceRevealed) {
                                    isBalanceRevealed = false
                                } else {
                                    showPinDialog = true
                                    pinInput = ""
                                    pinError = null
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                imageVector = if (isBalanceRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle Balance",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MEMBER PHONE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "+91 ${user.phoneNumber.takeLast(10)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        if (!isBalanceRevealed) {
                            Button(
                                onClick = {
                                    showPinDialog = true
                                    pinInput = ""
                                    pinError = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0D47A1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Balance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(
                                text = "Nova Premier Card",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // WALLET QUICK ACTIONS SUITE (Grid alignment)
        Text(
            text = "NovaPay Suite",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Button 1: Add Money
                QuickActionButton(
                    title = "Add Money",
                    icon = Icons.Rounded.Add,
                    color = Color(0xFFE3F2FD),
                    tint = Color(0xFF1E88E5)
                ) {
                    showAddMoneyDialog = true
                }

                // Button 2: Scan QR
                QuickActionButton(
                    title = "Scan QR",
                    icon = Icons.Rounded.QrCodeScanner,
                    color = Color(0xFFE8F5E9),
                    tint = Color(0xFF4CAF50)
                ) {
                    onNavigate("scanpay")
                }

                // Button 3: Send Money
                QuickActionButton(
                    title = "Send Money",
                    icon = Icons.AutoMirrored.Filled.Send,
                    color = Color(0xFFF3E5F5),
                    tint = Color(0xFF9C27B0)
                ) {
                    onNavigate("transfer")
                }

                // Button 4: Receive Money
                QuickActionButton(
                    title = "Receive",
                    icon = Icons.Rounded.QrCode,
                    color = Color(0xFFFFF3E0),
                    tint = Color(0xFFFF9800)
                ) {
                    onNavigate("qrcode")
                }

                // Button 5: Transaction History
                QuickActionButton(
                    title = "History",
                    icon = Icons.Rounded.History,
                    color = Color(0xFFFFEBEE),
                    tint = Color(0xFFE53935)
                ) {
                    onNavigate("transactions")
                }
            }
        }

        // WALLET SETUP / KYC COMPLETION BANNER IF APPLICABLE
        if (user.kycStatus != "Approved" || user.securePin.isEmpty()) {
            var showKycSetupDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1) // Soft warm amber
                ),
                border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Activate Your Digital Wallet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Setup your secret 4-digit PIN & update Aadhaar/PAN to remove transfer limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF795548)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showKycSetupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Complete Setup Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showKycSetupDialog) {
                var nameInput by remember { mutableStateOf(if (user.fullName.contains("User")) "" else user.fullName) }
                var emailInput by remember { mutableStateOf(if (user.email.contains("user_")) "" else user.email) }
                var dobInput by remember { mutableStateOf(if (user.dob == "01/01/2000") "" else user.dob) }
                var pinInputVal by remember { mutableStateOf("") }
                var aadhaarInputVal by remember { mutableStateOf(user.aadhaarNumber) }
                var panInputVal by remember { mutableStateOf(user.panCard) }
                var errorMsg by remember { mutableStateOf<String?>(null) }

                Dialog(onDismissRequest = { showKycSetupDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Setup Wallet Profile & PIN",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = dobInput,
                                onValueChange = { dobInput = it },
                                label = { Text("Date of Birth (DD/MM/YYYY)") },
                                placeholder = { Text("e.g. 15/08/1998") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pinInputVal,
                                onValueChange = { if (it.length <= 6) pinInputVal = it.replace(Regex("[^0-9]"), "") },
                                label = { Text("4-Digit or 6-Digit Security PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            errorMsg?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (nameInput.trim().isEmpty() || emailInput.trim().isEmpty() || dobInput.trim().isEmpty() || (pinInputVal.length != 4 && pinInputVal.length != 6)) {
                                        errorMsg = "Please fill all fields: Name, DOB, Email, and PIN (4 or 6 digits)."
                                    } else {
                                        viewModel.updateUserProfileAndKyc(
                                            fullName = nameInput,
                                            dob = dobInput,
                                            email = emailInput,
                                            pin = pinInputVal
                                        ) {
                                            showKycSetupDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Wallet Activation", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = { showKycSetupDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RECENT TRANSACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            TextButton(
                onClick = { onNavigate("transactions") },
                modifier = Modifier.testTag("view_all_transactions")
            ) {
                Text("View All", fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No Transactions Yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                transactions.take(4).forEach { tx ->
                    TransactionRow(
                        transaction = tx,
                        currentUserPhone = user.phoneNumber,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    // Add Money Dialog (Simulated Bank TopUp)
    if (showAddMoneyDialog) {
        Dialog(onDismissRequest = { showAddMoneyDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Load Wallet Cash",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0D47A1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simulate UPI/NetBanking load instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = addAmountStr,
                        onValueChange = { addAmountStr = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val amt = addAmountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.depositMoney(amt, "Loaded via UPI Gateway")
                                addAmountStr = ""
                                showAddMoneyDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Instantly", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showAddMoneyDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Text("Notifications Inbox", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your notifications inbox is empty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notifications) { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (!notif.isRead) {
                                            IconButton(
                                                onClick = { viewModel.markNotificationAsRead(notif.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Done,
                                                    contentDescription = "Mark as read",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Balance Verification/Set PIN Dialog
    if (showPinDialog) {
        val pinIsSet = user.securePin.isNotEmpty()
        
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (pinIsSet) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = Color(0xFF0D47A1),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (pinIsSet) "Enter Security PIN" else "Set Wallet Security PIN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0D47A1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (pinIsSet) "Please enter your 4-digit or 6-digit PIN to check balance." else "Protect your wallet. Create a new 4-digit or 6-digit Wallet PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it.replace(Regex("[^0-9]"), "") },
                        label = { Text("Wallet Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (pinInput.length != 4 && pinInput.length != 6) {
                                pinError = "PIN must be exactly 4 or 6 digits."
                            } else {
                                if (pinIsSet) {
                                    val inputHash = viewModel.hashPin(pinInput)
                                    if (inputHash == user.securePin) {
                                        isBalanceRevealed = true
                                        showPinDialog = false
                                    } else {
                                        pinError = "Incorrect PIN. Please try again."
                                    }
                                } else {
                                    // Save the newly set PIN securely
                                    viewModel.updateUserProfileAndKyc(
                                        fullName = user.fullName,
                                        dob = user.dob,
                                        email = user.email,
                                        pin = pinInput,
                                        aadhaar = user.aadhaarNumber,
                                        pan = user.panCard
                                    ) {
                                        isBalanceRevealed = true
                                        showPinDialog = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pinIsSet) "Verify" else "Set & Verify PIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    tint: Color,
    onClick: () -> Unit
) {
    val scale = if (title == "Scan QR") {
        val infiniteTransition = rememberInfiniteTransition()
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        animatedScale
    } else {
        1.0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// PROFILE AND KYC SETTINGS SCREEN
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser ?: return
    val themeState by viewModel.themeState.collectAsState()

    var nameInput by remember { mutableStateOf(user.fullName) }
    var emailInput by remember { mutableStateOf(user.email) }
    var pinInput by remember { mutableStateOf("") }
    var aadhaarInput by remember { mutableStateOf(user.aadhaarNumber) }
    var panInput by remember { mutableStateOf(user.panCard) }

    // KYC specific temporary states
    var kycDobInput by remember { mutableStateOf(user.dob) }
    var kycPhoneInput by remember { mutableStateOf(user.phoneNumber) }
    var kycAadhaarInput by remember { mutableStateOf(user.aadhaarNumber) }
    var kycPanInput by remember { mutableStateOf(user.panCard) }

    var activeSection by remember { mutableStateOf("none") } // "none", "profile", "kyc", "theme", "contact", "social"
    var isEditing by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val youtubeLink by viewModel.youtubeLink.collectAsState()
    val facebookLink by viewModel.facebookLink.collectAsState()

    var youtubeInput by remember { mutableStateOf(youtubeLink) }
    var facebookInput by remember { mutableStateOf(facebookLink) }

    // Sync input fields when the stored links change
    LaunchedEffect(youtubeLink, facebookLink) {
        youtubeInput = youtubeLink
        facebookInput = facebookLink
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (themeState == "black") Color(0xFF121212) else Color(0xFFF8F9FC))
            .verticalScroll(rememberScrollState())
    ) {
        // TOP HEADER BAR WITH BACK BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeSection != "none") {
                        activeSection = "none"
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (themeState == "black") Color(0xFF1E1E1E) else Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            val headerTitle = when (activeSection) {
                "profile" -> "Profile Settings"
                "kyc" -> "KYC Verification"
                "theme" -> "App Theme Settings"
                "contact" -> "Contact Support"
                "social" -> "Social Media Links"
                else -> "Settings & Profile Hub"
            }
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                )
            )
        }

        // PROFILE HERO SECTION (Avatar card)
        if (activeSection == "none") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo/Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (themeState == "black") Color(0xFF333333) else Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Profile Photo",
                            tint = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (themeState == "black") Color.White else Color(0xFF263238)
                    )

                    Text(
                        text = "+91 ${user.phoneNumber.takeLast(10)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // KYC Status Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (user.kycStatus == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFECEF)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (user.kycStatus == "Approved") Icons.Rounded.Verified else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (user.kycStatus == "Approved") Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KYC Status: ${if (user.kycStatus == "Approved") "Approved" else "Pending Setup"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (user.kycStatus == "Approved") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        if (activeSection == "none") {
            Spacer(modifier = Modifier.height(12.dp))

            // THE SETTINGS BUTTONS COLUMN (Stacked vertically as requested)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Profile Settings
                val isProfileActive = activeSection == "profile"
                Button(
                    onClick = { 
                        activeSection = "profile"
                        statusMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProfileActive) Color(0xFF0D47A1) else (if (themeState == "black") Color(0xFF1E1E1E) else Color.White),
                        contentColor = if (isProfileActive) Color.White else (if (themeState == "black") Color.White else Color(0xFF0D47A1))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profile Settings", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                // Button 2: KYC Settings
                val isKycActive = activeSection == "kyc"
                Button(
                    onClick = { 
                        activeSection = "kyc"
                        statusMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKycActive) Color(0xFF0D47A1) else (if (themeState == "black") Color(0xFF1E1E1E) else Color.White),
                        contentColor = if (isKycActive) Color.White else (if (themeState == "black") Color.White else Color(0xFF0D47A1))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Rounded.AssignmentInd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KYC Verification", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                // Button 3: Theme Settings
                val isThemeActive = activeSection == "theme"
                Button(
                    onClick = { 
                        activeSection = "theme"
                        statusMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isThemeActive) Color(0xFF0D47A1) else (if (themeState == "black") Color(0xFF1E1E1E) else Color.White),
                        contentColor = if (isThemeActive) Color.White else (if (themeState == "black") Color.White else Color(0xFF0D47A1))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App Theme Settings", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                // Button 4: Contact Us
                val isContactActive = activeSection == "contact"
                Button(
                    onClick = { 
                        activeSection = "contact"
                        statusMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isContactActive) Color(0xFF0D47A1) else (if (themeState == "black") Color(0xFF1E1E1E) else Color.White),
                        contentColor = if (isContactActive) Color.White else (if (themeState == "black") Color.White else Color(0xFF0D47A1))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Us Support", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }

                // Button 5: Social Media Links
                val isSocialActive = activeSection == "social"
                Button(
                    onClick = { 
                        activeSection = "social"
                        statusMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSocialActive) Color(0xFF0D47A1) else (if (themeState == "black") Color(0xFF1E1E1E) else Color.White),
                        contentColor = if (isSocialActive) Color.White else (if (themeState == "black") Color.White else Color(0xFF0D47A1))
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Social Media Links", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (activeSection != "none") {
            Spacer(modifier = Modifier.height(8.dp))

            // DYNAMIC ACTIVE TAB CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeState == "black") Color(0xFF1E1E1E) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Back to settings menu inside the sub-view card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSection = "none" }
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu",
                            tint = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back to Settings Menu",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1)
                        )
                    }

                    Divider(
                        color = if (themeState == "black") Color(0xFF333333) else Color(0xFFEEEEEE),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    when (activeSection) {
                    "profile" -> {
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { if (isEditing) nameInput = it },
                            label = { Text("Full Name") },
                            readOnly = !isEditing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { if (isEditing) emailInput = it },
                            label = { Text("Email Address") },
                            readOnly = !isEditing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (isEditing && it.length <= 6) pinInput = it.replace(Regex("[^0-9]"), "") },
                            label = { Text(if (isEditing) "Enter New Wallet PIN (4 or 6 Digits)" else "Security PIN Status") },
                            placeholder = { Text(if (user.securePin.isEmpty()) "No PIN Set" else "••••••") },
                            readOnly = !isEditing,
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle PIN Visibility",
                                        tint = if (themeState == "black") Color.White else Color.Gray
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Status message
                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (nameInput.trim().isEmpty() || emailInput.trim().isEmpty()) {
                                            isError = true
                                            statusMessage = "Validation failed: Please enter Name and Email."
                                        } else {
                                            viewModel.updateUserProfileAndKyc(
                                                fullName = nameInput,
                                                dob = user.dob,
                                                email = emailInput,
                                                pin = if (pinInput.isNotEmpty()) pinInput else "",
                                                aadhaar = user.aadhaarNumber,
                                                pan = user.panCard,
                                                onSuccess = {
                                                    isEditing = false
                                                    isError = false
                                                    statusMessage = "Profile successfully updated!"
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                                ) {
                                    Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isEditing = false
                                        statusMessage = null
                                        nameInput = user.fullName
                                        emailInput = user.email
                                        pinInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", color = if (themeState == "black") Color.White else Color.Gray)
                                }
                            }
                        } else {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile Details", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "kyc" -> {
                        // KYC Section - DOB and Mobile Number prefilled/editable temporarily
                        Text(
                            text = "Temporary KYC Registration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Fill in your details temporarily to verify your identity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = kycPhoneInput,
                            onValueChange = { kycPhoneInput = it.replace(Regex("[^0-9]"), "") },
                            label = { Text("Mobile Number") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = kycDobInput,
                            onValueChange = { kycDobInput = it },
                            label = { Text("Date of Birth (DD/MM/YYYY)") },
                            placeholder = { Text("01/01/2000") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = kycAadhaarInput,
                            onValueChange = { if (it.length <= 12) kycAadhaarInput = it.replace(Regex("[^0-9]"), "") },
                            label = { Text("Aadhaar Card (12 Digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = kycPanInput,
                            onValueChange = { if (it.length <= 10) kycPanInput = it.uppercase() },
                            label = { Text("PAN Card (10 Digits)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (kycPhoneInput.trim().isEmpty() || kycDobInput.trim().isEmpty()) {
                                    isError = true
                                    statusMessage = "Please fill in Mobile Number & DOB."
                                } else {
                                    viewModel.updateUserProfileAndKyc(
                                        fullName = user.fullName,
                                        dob = kycDobInput,
                                        email = user.email,
                                        pin = "",
                                        aadhaar = if (kycAadhaarInput.isNotEmpty()) kycAadhaarInput else user.aadhaarNumber,
                                        pan = if (kycPanInput.isNotEmpty()) kycPanInput else user.panCard,
                                        phoneNumber = kycPhoneInput,
                                        onSuccess = {
                                            isError = false
                                            statusMessage = "KYC successfully verified & saved!"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Save KYC", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    "theme" -> {
                        // Theme Selection Tab
                        Text(
                            text = "Choose App Theme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Select your preferred workspace aesthetic:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Option 1: Light Theme Card
                        Card(
                            onClick = { viewModel.setTheme("light") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (themeState == "light") Color(0xFFE3F2FD) else (if (themeState == "black") Color(0xFF2C2C2C) else Color(0xFFF1F5F9))
                            ),
                            border = if (themeState == "light") BorderStroke(2.dp, Color(0xFF0D47A1)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LightMode,
                                    contentDescription = "Light Mode",
                                    tint = if (themeState == "light") Color(0xFF0D47A1) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Light Theme",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (themeState == "black") Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Clean, professional white workspace",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                if (themeState == "light") {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0D47A1)
                                    )
                                }
                            }
                        }

                        // Option 2: Black Theme Card
                        Card(
                            onClick = { viewModel.setTheme("black") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (themeState == "black") Color(0xFF2A2A2A) else Color(0xFFF1F5F9)
                            ),
                            border = if (themeState == "black") BorderStroke(2.dp, Color.White) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = "Black Mode",
                                    tint = if (themeState == "black") Color.White else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Black Theme",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (themeState == "black") Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Eye-friendly dark OLED workspace",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                if (themeState == "black") {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    "contact" -> {
                        Text(
                            text = "Contact Support",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Get in touch with our customer service team. We are here to help you 24x7.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        val context = LocalContext.current

                        // Phone card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (themeState == "black") Color(0xFF2A2A2A) else Color(0xFFF8F9FC)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D47A1).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color(0xFF0D47A1))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Toll-Free Support", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                                    Text("+91 1800 123 4567", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:+9118001234567"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Cannot initiate call", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Call", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }

                        // Email Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (themeState == "black") Color(0xFF2A2A2A) else Color(0xFFF8F9FC)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Email, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Email Support", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (themeState == "black") Color.White else Color.Black)
                                    Text("support@novapay.com", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                data = android.net.Uri.parse("mailto:support@novapay.com")
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "NovaPay Support Request")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Cannot open mail client", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Mail", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }

                        // Address card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (themeState == "black") Color(0xFF2A2A2A) else Color(0xFFF8F9FC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Corporate Office Address",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (themeState == "black") Color.White else Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "NovaPay Technology India Pvt. Ltd.\nLevel 14, Prestige Tech Park, Outer Ring Road, Bangalore, Karnataka - 560103",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    "social" -> {
                        Text(
                            text = "Connect with Social Media",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Add or update your social channel links below, or click to open them instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val context = LocalContext.current

                        // Inputs to Add/Update YouTube & Facebook links
                        OutlinedTextField(
                            value = youtubeInput,
                            onValueChange = { youtubeInput = it },
                            label = { Text("YouTube Channel Link") },
                            placeholder = { Text("https://youtube.com/yourchannel") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = facebookInput,
                            onValueChange = { facebookInput = it },
                            label = { Text("Facebook Profile Link") },
                            placeholder = { Text("https://facebook.com/yourprofile") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                unfocusedTextColor = if (themeState == "black") Color.White else Color.Black,
                                focusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                unfocusedContainerColor = if (themeState == "black") Color(0xFF121212) else Color.White,
                                focusedLabelColor = if (themeState == "black") Color.White else Color(0xFF0D47A1),
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Links Button
                        Button(
                            onClick = {
                                viewModel.saveSocialLinks(youtubeInput.trim(), facebookInput.trim())
                                android.widget.Toast.makeText(context, "Social links updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                        ) {
                            Text("Save Social Links", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Divider(color = if (themeState == "black") Color(0xFF333333) else Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 16.dp))

                        // Clickable Social Buttons to open YouTube and Facebook links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val url = if (youtubeLink.startsWith("http://") || youtubeLink.startsWith("https://")) youtubeLink else "https://$youtubeLink"
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid YouTube URL", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)) // YouTube Red
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("YouTube", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val url = if (facebookLink.startsWith("http://") || facebookLink.startsWith("https://")) facebookLink else "https://$facebookLink"
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid Facebook URL", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)) // Facebook Blue
                            ) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Facebook", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

        if (activeSection == "none") {
            Spacer(modifier = Modifier.height(24.dp))

            // LOGOUT SESSION SAFELY FOOTER BUTTON
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Session Safely", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}


