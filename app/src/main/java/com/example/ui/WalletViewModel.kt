package com.example.ui

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FirebaseManager
import com.example.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class WalletViewModel(
    application: Application,
    private val repository: WalletRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("novapay_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(prefs.getString("app_theme", "light") ?: "light")
    val themeState: StateFlow<String> = _themeState.asStateFlow()

    private val _youtubeLink = MutableStateFlow(prefs.getString("youtube_link", "https://www.youtube.com/@novapay") ?: "https://www.youtube.com/@novapay")
    val youtubeLink: StateFlow<String> = _youtubeLink.asStateFlow()

    private val _facebookLink = MutableStateFlow(prefs.getString("facebook_link", "https://www.facebook.com/novapay") ?: "https://www.facebook.com/novapay")
    val facebookLink: StateFlow<String> = _facebookLink.asStateFlow()

    fun setTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _themeState.value = theme
    }

    fun saveSocialLinks(youtube: String, facebook: String) {
        prefs.edit()
            .putString("youtube_link", youtube)
            .putString("facebook_link", facebook)
            .apply()
        _youtubeLink.value = youtube
        _facebookLink.value = facebook
    }

    fun hashPin(pin: String): String {
        if (pin.isEmpty()) return ""
        val bytes = pin.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // Helper converters between Room entities and Firebase models
    private fun FirebaseWalletUser.toEntity(): UserEntity {
        return UserEntity(
            id = 0,
            phoneNumber = this.mobileNumber,
            fullName = this.fullName,
            dob = "01/01/2000",
            email = this.email,
            profilePhoto = this.profilePhoto,
            aadhaarNumber = this.aadhaarNumber,
            panCard = this.panNumber,
            aadhaarImage = this.aadhaarImage,
            panImage = this.panImage,
            kycStatus = this.kycStatus,
            walletBalance = this.walletBalance,
            isFrozen = this.isBlocked,
            qrCodeData = "novapay://pay?phone=${this.mobileNumber}&name=${this.fullName.replace(" ", "%20")}",
            securePin = this.walletPinHash
        )
    }

    private fun UserEntity.toFirebase(uid: String): FirebaseWalletUser {
        return FirebaseWalletUser(
            uid = uid,
            fullName = this.fullName,
            mobileNumber = this.phoneNumber,
            email = this.email,
            profilePhoto = this.profilePhoto,
            walletBalance = this.walletBalance,
            walletPinHash = this.securePin,
            kycStatus = this.kycStatus,
            aadhaarNumber = this.aadhaarNumber,
            panNumber = this.panCard,
            aadhaarImage = this.aadhaarImage,
            panImage = this.panImage,
            isBlocked = this.isFrozen,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun FirebaseTransaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = 0,
            senderPhone = if (this.senderUid == "Bank Gateway") "Bank Gateway" else this.senderUid,
            receiverPhone = this.receiverUid,
            senderName = this.senderName,
            receiverName = this.receiverName,
            amount = this.amount,
            timestamp = this.createdAt,
            status = this.status,
            type = this.type,
            notes = this.description,
            auditHash = TransactionEntity.generateAuditHash(this.senderUid, this.receiverUid, this.amount, this.createdAt, this.description)
        )
    }

    // Authentication States
    private val _currentPhoneInput = MutableStateFlow("")
    val currentPhoneInput = _currentPhoneInput.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent = _otpSent.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating = _isAuthenticating.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _jwtToken = MutableStateFlow<String?>(null)
    val jwtToken = _jwtToken.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    // PIN States
    private val _showPinSetup = MutableStateFlow(false)
    val showPinSetup = _showPinSetup.asStateFlow()

    private val _showPinVerification = MutableStateFlow(false)
    val showPinVerification = _showPinVerification.asStateFlow()

    private val _pendingUserForPin = MutableStateFlow<UserEntity?>(null)
    val pendingUserForPin = _pendingUserForPin.asStateFlow()

    // Screen States
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode = _isAdminMode.asStateFlow()

    // Transfer Screen State
    private val _transferStatus = MutableStateFlow<TransactionResult?>(null)
    val transferStatus = _transferStatus.asStateFlow()

    // KYC Submission State
    private val _kycStatusMessage = MutableStateFlow<String?>(null)
    val kycStatusMessage = _kycStatusMessage.asStateFlow()

    // Dynamic Flow lists
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allUsers: StateFlow<List<UserEntity>> = if (FirebaseManager.isFirebaseAvailable) {
        FirebaseManager.listenToAllUsers()
            .map { list -> list.map { it.toEntity() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        repository.allUsers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<TransactionEntity>> = if (FirebaseManager.isFirebaseAvailable) {
        FirebaseManager.listenToAllTransactions()
            .map { list -> list.map { it.toEntity() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Current user's transactions
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUserTransactions: StateFlow<List<TransactionEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                if (FirebaseManager.isFirebaseAvailable) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    FirebaseManager.listenToTransactions(uid)
                        .map { list -> list.map { it.toEntity() } }
                } else {
                    repository.getTransactionsForUser(user.phoneNumber)
                }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications List StateFlow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userNotifications: StateFlow<List<FirebaseNotification>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && FirebaseManager.isFirebaseAvailable) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                FirebaseManager.listenToNotifications(uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Pre-seed local database fallback if empty
            repository.seedIfNeeded()

            if (FirebaseManager.isFirebaseAvailable) {
                val fbAuthUser = FirebaseAuth.getInstance().currentUser
                if (fbAuthUser != null) {
                    val uid = fbAuthUser.uid
                    val token = "firebase_token_$uid"
                    _jwtToken.value = token
                    _isAuthenticated.value = true

                    // Setup real-time listeners for Firestore User profile
                    viewModelScope.launch {
                        FirebaseManager.listenToUserProfile(uid).collect { fbUser ->
                            if (fbUser != null) {
                                val entity = fbUser.toEntity()
                                _currentUser.value = entity
                                // Synchronize locally in Room
                                repository.updateUser(entity)
                            }
                        }
                    }
                }
            } else {
                // Restore local legacy login session
                val savedPhone = prefs.getString("phone", null)
                val savedToken = prefs.getString("token", null)
                if (savedPhone != null && savedToken != null) {
                    val user = repository.getUserByPhoneSync(savedPhone)
                    if (user != null) {
                        _jwtToken.value = savedToken
                        _currentUser.value = user
                        _isAuthenticated.value = true

                        viewModelScope.launch {
                            repository.getUserByPhone(savedPhone).collect { liveUser ->
                                _currentUser.value = liveUser
                            }
                        }
                    }
                }
            }
        }
    }

    // Set phone number for login
    fun setPhoneInput(phone: String) {
        _currentPhoneInput.value = phone
    }

    // Clear error
    fun clearAuthError() {
        _authError.value = null
    }

    // Robust phone number normalization
    private fun normalizePhone(phone: String): String {
        val trimmed = phone.trim()
        val digitsOnly = trimmed.replace(Regex("[^0-9+]"), "")
        return if (digitsOnly.startsWith("+")) {
            digitsOnly
        } else if (digitsOnly.length == 10) {
            "+91$digitsOnly"
        } else if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
            "+$digitsOnly"
        } else {
            if (digitsOnly.isEmpty()) "" else "+91$digitsOnly"
        }
    }

    // Step 1: Request OTP
    fun requestOtp(phone: String, activity: Activity? = null) {
        val normalized = normalizePhone(phone)
        if (normalized.length < 13) {
            _authError.value = "Please enter a valid 10-digit mobile number"
            return
        }
        _isAuthenticating.value = true
        _authError.value = null

        if (FirebaseManager.isFirebaseAvailable && activity != null) {
            FirebaseManager.sendOtp(
                activity = activity,
                phoneNumber = normalized,
                onCodeSent = { verificationId ->
                    _otpSent.value = true
                    _isAuthenticating.value = false
                },
                onVerificationFailed = { error ->
                    _authError.value = error.localizedMessage ?: "OTP delivery failed."
                    _isAuthenticating.value = false
                },
                onVerificationCompleted = { credential ->
                    verifyCredential(credential)
                }
            )
        } else {
            // Simulated fallback
            viewModelScope.launch {
                delay(1000)
                _otpSent.value = true
                _isAuthenticating.value = false
            }
        }
    }

    private fun verifyCredential(credential: PhoneAuthCredential) {
        _isAuthenticating.value = true
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val fbUser = authResult.user
                if (fbUser != null) {
                    val uid = fbUser.uid
                    val token = "firebase_token_$uid"
                    _jwtToken.value = token
                    _isAuthenticated.value = true
                    _otpSent.value = false
                    _isAuthenticating.value = false

                    viewModelScope.launch {
                        FirebaseManager.listenToUserProfile(uid).collect { updatedUser ->
                            if (updatedUser != null) {
                                val entity = updatedUser.toEntity()
                                _currentUser.value = entity
                                repository.updateUser(entity)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                _authError.value = it.localizedMessage ?: "Auto verification failed."
                _isAuthenticating.value = false
            }
    }

    // Step 2: Verify OTP
    fun verifyOtp(phone: String, otp: String, onNavigateToRegister: () -> Unit) {
        if (otp.length != 4) {
            _authError.value = "Invalid OTP code. Please enter a 4-digit OTP."
            return
        }

        _isAuthenticating.value = true
        _authError.value = null

        val normalized = normalizePhone(phone)

        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.verifyOtp(
                otpCode = otp,
                onSuccess = { fbUser ->
                    _isAuthenticating.value = false
                    val token = "firebase_token_${fbUser.uid}"
                    _jwtToken.value = token
                    _isAuthenticated.value = true
                    _otpSent.value = false
                    _showPinVerification.value = false
                    _showPinSetup.value = false
                    _pendingUserForPin.value = null

                    val entity = fbUser.toEntity()
                    _currentUser.value = entity
                    prefs.edit().putString("phone", normalized).putString("token", token).apply()

                    viewModelScope.launch {
                        FirebaseManager.listenToUserProfile(fbUser.uid).collect { updatedUser ->
                            if (updatedUser != null) {
                                val updatedEntity = updatedUser.toEntity()
                                _currentUser.value = updatedEntity
                                repository.updateUser(updatedEntity)
                            }
                        }
                    }
                },
                onFailure = { error ->
                    _authError.value = error.localizedMessage ?: "Invalid OTP."
                    _isAuthenticating.value = false
                }
            )
        } else {
            // SQLite/Room Fallback Flow
            viewModelScope.launch {
                val user = repository.getUserByPhoneSync(normalized)
                _isAuthenticating.value = false
                
                val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.novapay_session_${UUID.randomUUID()}"
                _jwtToken.value = token
                _isAuthenticated.value = true
                _otpSent.value = false
                _showPinVerification.value = false
                _showPinSetup.value = false
                _pendingUserForPin.value = null

                if (user != null) {
                    _currentUser.value = user
                    prefs.edit().putString("phone", normalized).putString("token", token).apply()
                    viewModelScope.launch {
                        repository.getUserByPhone(normalized).collect { liveUser ->
                            _currentUser.value = liveUser
                        }
                    }
                } else {
                    val defaultName = "User ${normalized.takeLast(4)}"
                    val newUser = UserEntity(
                        phoneNumber = normalized,
                        fullName = defaultName,
                        dob = "01/01/2000",
                        email = "user_${normalized.takeLast(4)}@novapay.com",
                        walletBalance = 500.0,
                        kycStatus = "None",
                        qrCodeData = "novapay://pay?phone=$normalized&name=${defaultName.replace(" ", "%20")}",
                        securePin = ""
                    )
                    val id = repository.registerUser(newUser)
                    if (id > 0) {
                        _currentUser.value = newUser
                        prefs.edit().putString("phone", normalized).putString("token", token).apply()
                        viewModelScope.launch {
                            repository.getUserByPhone(normalized).collect { liveUser ->
                                _currentUser.value = liveUser
                            }
                        }
                    } else {
                        _authError.value = "Login failed. Please try again."
                    }
                }
            }
        }
    }

    // Register new user
    fun registerNewUser(phone: String, fullName: String, dob: String, email: String) {
        if (fullName.trim().isEmpty() || dob.trim().isEmpty() || email.trim().isEmpty()) {
            _authError.value = "Please fill in all mandatory fields"
            return
        }

        _isAuthenticating.value = true
        _authError.value = null

        val normalized = normalizePhone(phone)

        viewModelScope.launch {
            val existing = repository.getUserByPhoneSync(normalized)
            if (existing != null) {
                _authError.value = "Mobile number already registered"
                _isAuthenticating.value = false
                return@launch
            }

            val newUser = UserEntity(
                phoneNumber = normalized,
                fullName = fullName,
                dob = dob,
                email = email,
                walletBalance = 500.0,
                kycStatus = "None",
                qrCodeData = "novapay://pay?phone=$normalized&name=${fullName.replace(" ", "%20")}",
                securePin = ""
            )

            _pendingUserForPin.value = newUser
            _showPinSetup.value = true
            _isAuthenticating.value = false
        }
    }

    // Submit new PIN for registration
    fun submitSetPin(pin: String) {
        if (pin.length != 4) {
            _authError.value = "PIN must be exactly 4 digits"
            return
        }
        val tempUser = _pendingUserForPin.value ?: return
        _isAuthenticating.value = true
        _authError.value = null

        viewModelScope.launch {
            val hashed = hashPin(pin)
            val finalUser = tempUser.copy(securePin = hashed)
            
            if (FirebaseManager.isFirebaseAvailable) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "sim_uid_${UUID.randomUUID()}"
                val fbUser = finalUser.toFirebase(uid)
                FirebaseManager.updateUser(fbUser) { success ->
                    _isAuthenticating.value = false
                    if (success) {
                        val token = "firebase_token_$uid"
                        _jwtToken.value = token
                        _currentUser.value = finalUser
                        _isAuthenticated.value = true
                        _otpSent.value = false
                        _showPinSetup.value = false
                        _pendingUserForPin.value = null

                        viewModelScope.launch {
                            FirebaseManager.listenToUserProfile(uid).collect { liveUser ->
                                if (liveUser != null) {
                                    val entity = liveUser.toEntity()
                                    _currentUser.value = entity
                                    repository.updateUser(entity)
                                }
                            }
                        }
                    } else {
                        _authError.value = "Failed to establish cloud profile."
                    }
                }
            } else {
                // Room fallback path
                val id = repository.registerUser(finalUser)
                if (id > 0) {
                    val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.novapay_session_${UUID.randomUUID()}"
                    _jwtToken.value = token
                    _currentUser.value = finalUser
                    _isAuthenticated.value = true
                    _otpSent.value = false
                    _showPinSetup.value = false
                    _pendingUserForPin.value = null
                    _isAuthenticating.value = false

                    viewModelScope.launch {
                        repository.getUserByPhone(finalUser.phoneNumber).collect { liveUser ->
                            _currentUser.value = liveUser
                        }
                    }
                } else {
                    _authError.value = "Registration failed. Try again."
                    _isAuthenticating.value = false
                }
            }
        }
    }

    // Verify PIN for login
    fun submitVerifyPin(pin: String) {
        if (pin.length != 4) {
            _authError.value = "PIN must be exactly 4 digits"
            return
        }
        val user = _pendingUserForPin.value ?: return
        _isAuthenticating.value = true
        _authError.value = null

        val hashed = hashPin(pin)

        viewModelScope.launch {
            if (user.securePin.isEmpty()) {
                val updatedUser = user.copy(securePin = hashed)
                
                if (FirebaseManager.isFirebaseAvailable) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    FirebaseManager.updateUser(updatedUser.toFirebase(uid)) {
                        _isAuthenticating.value = false
                        val token = "firebase_token_$uid"
                        _jwtToken.value = token
                        _currentUser.value = updatedUser
                        _isAuthenticated.value = true
                        _otpSent.value = false
                        _showPinVerification.value = false
                        _pendingUserForPin.value = null
                    }
                } else {
                    repository.registerUser(updatedUser)
                    _isAuthenticating.value = false
                    val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.novapay_session_${UUID.randomUUID()}"
                    _jwtToken.value = token
                    _currentUser.value = updatedUser
                    _isAuthenticated.value = true
                    _otpSent.value = false
                    _showPinVerification.value = false
                    _pendingUserForPin.value = null
                }
            } else if (user.securePin == hashed || user.securePin == pin) {
                _isAuthenticating.value = false
                val token = if (FirebaseManager.isFirebaseAvailable) {
                    "firebase_token_${FirebaseAuth.getInstance().currentUser?.uid}"
                } else {
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.novapay_session_${UUID.randomUUID()}"
                }
                _jwtToken.value = token
                _currentUser.value = user
                _isAuthenticated.value = true
                _otpSent.value = false
                _showPinVerification.value = false
                _pendingUserForPin.value = null
            } else {
                _authError.value = "Incorrect PIN. Please try again."
                _isAuthenticating.value = false
            }
        }
    }

    // Cancel PIN flow and return
    fun cancelPinFlow() {
        _showPinSetup.value = false
        _showPinVerification.value = false
        _pendingUserForPin.value = null
        _authError.value = null
    }

    // Toggle Admin Mode
    fun setAdminMode(enabled: Boolean) {
        _isAdminMode.value = enabled
    }

    // Clear transfer state
    fun clearTransferStatus() {
        _transferStatus.value = null
    }

    // Wallet-to-Wallet Transfer
    fun initiateTransfer(receiverPhone: String, amount: Double, notes: String) {
        val sender = _currentUser.value ?: return
        _transferStatus.value = TransactionResult.Loading

        val normalizedReceiver = normalizePhone(receiverPhone)

        if (FirebaseManager.isFirebaseAvailable) {
            val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            FirebaseManager.executeTransfer(
                senderUid = senderUid,
                receiverPhone = normalizedReceiver,
                amount = amount,
                notes = notes,
                onSuccess = { tx ->
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000) // Beautiful delay for realistic payment animation
                        _transferStatus.value = TransactionResult.Success(tx.toEntity())
                    }
                },
                onFailure = { error ->
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1000)
                        _transferStatus.value = TransactionResult.Error(error.localizedMessage ?: "Transfer failed.")
                    }
                }
            )
        } else {
            // Room Fallback transfer
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000) // Beautiful delay for realistic payment animation
                val result = repository.transferMoney(
                    senderPhone = sender.phoneNumber,
                    receiverPhone = normalizedReceiver,
                    amount = amount,
                    notes = notes
                )
                result.onSuccess { tx ->
                    _transferStatus.value = TransactionResult.Success(tx)
                }.onFailure { error ->
                    _transferStatus.value = TransactionResult.Error(error.localizedMessage ?: "Unknown Error")
                }
            }
        }
    }

    // Deposit loading gateway
    fun depositMoney(amount: Double, notes: String = "Loaded via NovaGateway") {
        val user = _currentUser.value ?: return
        if (FirebaseManager.isFirebaseAvailable) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            FirebaseManager.executeDeposit(uid, amount, notes) { success ->
                if (!success) Log.e("WalletViewModel", "Deposit failed on cloud sync")
            }
        } else {
            viewModelScope.launch {
                repository.depositMoney(user.phoneNumber, amount, notes)
            }
        }
    }

    // KYC Submission
    fun submitKycDocs(aadhaar: String, pan: String, aadhaarImg: String?, panImg: String?) {
        val user = _currentUser.value ?: return
        _kycStatusMessage.value = "Uploading and processing KYC..."
        
        if (FirebaseManager.isFirebaseAvailable) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            // Mock bytes for mock upload when run in emulator
            val mockBytes = "image_data_payload_simulated".toByteArray()
            
            FirebaseManager.uploadImage(uid, "aadhaar", mockBytes, { aadhaarUrl ->
                FirebaseManager.uploadImage(uid, "pan", mockBytes, { panUrl ->
                    // Submit documents inside user Firestore
                    val updatedUser = user.copy(
                        aadhaarNumber = aadhaar,
                        panCard = pan,
                        aadhaarImage = aadhaarUrl,
                        panImage = panUrl,
                        kycStatus = "Pending"
                    )
                    FirebaseManager.updateUser(updatedUser.toFirebase(uid)) { success ->
                        if (success) {
                            _kycStatusMessage.value = "KYC documents submitted successfully. Status: Pending Admin Review."
                        } else {
                            _kycStatusMessage.value = "KYC Firestore Sync Failed."
                        }
                    }
                }, { error ->
                    _kycStatusMessage.value = "PAN upload failed: ${error.localizedMessage}"
                })
            }, { error ->
                _kycStatusMessage.value = "Aadhaar upload failed: ${error.localizedMessage}"
            })
        } else {
            // SQLite/Room Fallback
            viewModelScope.launch {
                val result = repository.submitKyc(user.phoneNumber, aadhaar, pan, aadhaarImg, panImg)
                result.onSuccess {
                    _kycStatusMessage.value = "KYC documents submitted successfully. Status: Pending Admin Review."
                }.onFailure { error ->
                    _kycStatusMessage.value = "Submission failed: ${error.localizedMessage}"
                }
            }
        }
    }

    // Admin Actions
    fun adminApproveKyc(userPhoneOrUid: String) {
        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.adminSetKycStatus(userPhoneOrUid, "Approved") {}
        } else {
            viewModelScope.launch {
                repository.adminReviewKyc(userPhoneOrUid, approve = true)
            }
        }
    }

    fun adminRejectKyc(userPhoneOrUid: String) {
        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.adminSetKycStatus(userPhoneOrUid, "Rejected") {}
        } else {
            viewModelScope.launch {
                repository.adminReviewKyc(userPhoneOrUid, approve = false)
            }
        }
    }

    fun adminToggleFreeze(userPhoneOrUid: String) {
        if (FirebaseManager.isFirebaseAvailable) {
            // Fetch first to toggle
            viewModelScope.launch {
                // If uid was passed, get that document and negate block status
                FirebaseManager.adminSetBlocked(userPhoneOrUid, true) {}
            }
        } else {
            viewModelScope.launch {
                repository.adminToggleFreeze(userPhoneOrUid)
            }
        }
    }

    fun adminFreezeUser(uid: String, freeze: Boolean) {
        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.adminSetBlocked(uid, freeze) {}
        }
    }

    fun adminSendNotification(uid: String, title: String, message: String) {
        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.adminSendNotification(uid, title, message)
        }
    }

    fun markNotificationAsRead(notifId: String) {
        if (FirebaseManager.isFirebaseAvailable) {
            FirebaseManager.markNotificationAsRead(notifId)
        }
    }

    fun updateUserProfileAndKyc(
        fullName: String,
        dob: String,
        email: String,
        pin: String,
        aadhaar: String = "123456789012",
        pan: String = "ABCDE1234F",
        phoneNumber: String = "",
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (fullName.trim().isEmpty() || dob.trim().isEmpty() || email.trim().isEmpty() || (pin.isNotEmpty() && pin.length != 4 && pin.length != 6)) {
            _authError.value = "Please fill all profile fields: Name, DOB, Email, and 4-Digit Security PIN."
            return
        }
        _isAuthenticating.value = true
        _authError.value = null

        val hashed = if (pin.isNotEmpty()) hashPin(pin) else user.securePin
        val finalAadhaar = if (aadhaar.trim().length == 12) aadhaar else "123456789012"
        val finalPan = if (pan.trim().length == 10) pan.uppercase() else "ABCDE1234F"
        val finalPhone = if (phoneNumber.trim().isNotEmpty()) phoneNumber else user.phoneNumber

        val updatedUser = user.copy(
            fullName = fullName,
            dob = dob,
            email = email,
            phoneNumber = finalPhone,
            securePin = hashed,
            aadhaarNumber = finalAadhaar,
            panCard = finalPan,
            kycStatus = "Approved",
            qrCodeData = "novapay://pay?phone=${finalPhone}&name=${fullName.replace(" ", "%20")}"
        )

        viewModelScope.launch {
            if (FirebaseManager.isFirebaseAvailable) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                FirebaseManager.updateUser(updatedUser.toFirebase(uid)) { success ->
                    _isAuthenticating.value = false
                    if (success) {
                        _currentUser.value = updatedUser
                        _authError.value = null
                        onSuccess()
                    } else {
                        _authError.value = "Profile Cloud update failed."
                    }
                }
            } else {
                repository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                _isAuthenticating.value = false
                _authError.value = null
                onSuccess()
            }
        }
    }

    // Logout
    fun logout() {
        FirebaseManager.logout()
        _currentUser.value = null
        _jwtToken.value = null
        _isAuthenticated.value = false
        _otpSent.value = false
        _isAdminMode.value = false
        _showPinSetup.value = false
        _showPinVerification.value = false
        _pendingUserForPin.value = null
        _authError.value = null
        prefs.edit().clear().apply()
    }
}

// Sealed class for Transaction results
sealed class TransactionResult {
    object Loading : TransactionResult()
    data class Success(val transaction: TransactionEntity) : TransactionResult()
    data class Error(val message: String) : TransactionResult()
}

// ViewModel factory helper
class WalletViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val repository = WalletRepository(db.userDao(), db.transactionDao())
            @Suppress("UNCHECKED_CAST")
            return WalletViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
