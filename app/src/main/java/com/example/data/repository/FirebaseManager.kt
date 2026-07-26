package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    var isFirebaseAvailable: Boolean = false
        private set

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var messaging: FirebaseMessaging? = null

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun initialize(context: Context) {
        try {
            // Check if Firebase is already initialized or has valid Google services
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                auth = FirebaseAuth.getInstance()
                
                firestore = FirebaseFirestore.getInstance().apply {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    firestoreSettings = settings
                }
                
                storage = FirebaseStorage.getInstance()
                messaging = FirebaseMessaging.getInstance()
                
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase initialized successfully with offline persistence.")
            }
        } catch (e: Exception) {
            isFirebaseAvailable = false
            Log.e(TAG, "Firebase initialization skipped / failed. Falling back to local db mode.", e)
        }
    }

    // AUTHENTICATION FLOW
    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationFailed: (Exception) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit
    ) {
        if (!isFirebaseAvailable) {
            // Simulated development fallback
            Log.w(TAG, "Firebase unavailable, simulating OTP send to $phoneNumber")
            verificationId = "simulated_verification_id_${UUID.randomUUID()}"
            onCodeSent(verificationId!!)
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Verification completed automatically: $credential")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Verification failed", e)
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "Code sent: $verificationId")
                this@FirebaseManager.verificationId = verificationId
                this@FirebaseManager.resendToken = token
                onCodeSent(verificationId)
            }
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            activity,
            callbacks
        )
    }

    fun verifyOtp(
        otpCode: String,
        onSuccess: (FirebaseWalletUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val verificationId = this.verificationId
        if (!isFirebaseAvailable || verificationId == null || verificationId.startsWith("simulated_")) {
            // Simulated login fallback
            Log.w(TAG, "Simulated OTP verification successful.")
            val fakeUid = "sim_user_${UUID.randomUUID().toString().take(8)}"
            val simulatedUser = FirebaseWalletUser(
                uid = fakeUid,
                fullName = "Nova Pay User",
                mobileNumber = "+919999999999",
                email = "user@novapay.com"
            )
            onSuccess(simulatedUser)
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        auth?.signInWithCredential(credential)
            ?.addOnSuccessListener { authResult ->
                val fbUser = authResult.user
                if (fbUser != null) {
                    val uid = fbUser.uid
                    val phone = fbUser.phoneNumber ?: ""
                    
                    // Retrieve user or create user doc
                    val userRef = firestore?.collection("users")?.document(uid)
                    userRef?.get()?.addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val user = doc.toObject(FirebaseWalletUser::class.java)
                            if (user != null) {
                                onSuccess(user)
                            } else {
                                onFailure(Exception("Failed to parse user details from Firestore."))
                            }
                        } else {
                            // Automatically register user
                            val newUser = FirebaseWalletUser(
                                uid = uid,
                                fullName = "User ${phone.takeLast(4)}",
                                mobileNumber = phone,
                                email = "user_${phone.takeLast(4)}@novapay.com",
                                walletBalance = 500.0, // Free joining bonus credit
                                kycStatus = "Pending"
                            )
                            userRef.set(newUser).addOnSuccessListener {
                                // Create default wallet doc
                                firestore?.collection("wallet")?.document(uid)?.set(
                                    FirebaseWallet(uid = uid, availableBalance = 500.0, updatedAt = System.currentTimeMillis())
                                )
                                onSuccess(newUser)
                            }.addOnFailureListener {
                                onFailure(it)
                            }
                        }
                    }?.addOnFailureListener {
                        onFailure(it)
                    }
                } else {
                    onFailure(Exception("Firebase Auth User is null"))
                }
            }
            ?.addOnFailureListener {
                onFailure(it)
            }
    }

    // REAL-TIME FIRESTORE LISTENER FOR USER PROFILE
    fun listenToUserProfile(uid: String): Flow<FirebaseWalletUser?> = callbackFlow {
        if (!isFirebaseAvailable || firestore == null) {
            close()
            return@callbackFlow
        }

        val listener = firestore!!.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user profile", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(FirebaseWalletUser::class.java)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    // REAL-TIME FIRESTORE LISTENER FOR TRANSACTIONS
    fun listenToTransactions(uid: String): Flow<List<FirebaseTransaction>> = callbackFlow {
        if (!isFirebaseAvailable || firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore!!.collection("transactions")
            .whereIn("senderUid", listOf(uid, "any"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to transactions, attempting fallback query", error)
                    // If complex multi-field query fails due to missing composite index, try simple query
                    val fallbackListener = firestore!!.collection("transactions")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { innerSnapshot, innerError ->
                            if (innerError != null) {
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            if (innerSnapshot != null) {
                                val list = innerSnapshot.toObjects(FirebaseTransaction::class.java)
                                val filtered = list.filter { it.senderUid == uid || it.receiverUid == uid }
                                trySend(filtered)
                            }
                        }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.toObjects(FirebaseTransaction::class.java)
                    // If we did a simpler query to bypass index issues, filter client side
                    val filtered = list.filter { it.senderUid == uid || it.receiverUid == uid }
                    trySend(filtered)
                }
            }

        awaitClose { listener.remove() }
    }

    // GET ALL TRANSACTIONS (ADMIN)
    fun listenToAllTransactions(): Flow<List<FirebaseTransaction>> = callbackFlow {
        if (!isFirebaseAvailable || firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore!!.collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to all transactions", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(FirebaseTransaction::class.java))
                }
            }
        awaitClose { listener.remove() }
    }

    // GET ALL USERS (ADMIN)
    fun listenToAllUsers(): Flow<List<FirebaseWalletUser>> = callbackFlow {
        if (!isFirebaseAvailable || firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore!!.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to all users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(FirebaseWalletUser::class.java))
                }
            }
        awaitClose { listener.remove() }
    }

    // UPDATE USER IN FIRESTORE
    fun updateUser(user: FirebaseWalletUser, onComplete: (Boolean) -> Unit) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(true)
            return
        }

        firestore!!.collection("users").document(user.uid).set(user)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to update user", it)
                onComplete(false)
            }
    }

    // SUBMIT TRANSACTION & RECALCULATE REAL-TIME BALANCE
    fun executeTransfer(
        senderUid: String,
        receiverPhone: String,
        amount: Double,
        notes: String,
        onSuccess: (FirebaseTransaction) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable || firestore == null) {
            onFailure(Exception("Firebase is unconfigured or unavailable."))
            return
        }

        val usersColl = firestore!!.collection("users")
        val txColl = firestore!!.collection("transactions")

        // 1. Fetch Sender
        usersColl.document(senderUid).get().addOnSuccessListener { senderDoc ->
            val sender = senderDoc.toObject(FirebaseWalletUser::class.java)
            if (sender == null) {
                onFailure(Exception("Sender profile not found."))
                return@addOnSuccessListener
            }

            if (sender.isBlocked) {
                onFailure(Exception("Your account has been frozen/blocked by administrator."))
                return@addOnSuccessListener
            }

            if (sender.walletBalance < amount) {
                onFailure(Exception("Insufficient wallet balance. Available: ₹${sender.walletBalance}"))
                return@addOnSuccessListener
            }

            // 2. Query Receiver by phone
            usersColl.whereEqualTo("mobileNumber", receiverPhone).get().addOnSuccessListener { queryResult ->
                val receiverDoc = queryResult.documents.firstOrNull()
                if (receiverDoc == null) {
                    val fakeUid = "user_" + receiverPhone.replace("+", "").replace(" ", "")
                    val newReceiver = FirebaseWalletUser(
                        uid = fakeUid,
                        fullName = "NovaPay User ${receiverPhone.takeLast(4)}",
                        mobileNumber = receiverPhone,
                        email = "user${receiverPhone.takeLast(4)}@novapay.in",
                        kycStatus = "Approved",
                        walletBalance = 0.0,
                        walletPinHash = "",
                        isBlocked = false
                    )
                    usersColl.document(fakeUid).set(newReceiver).addOnSuccessListener {
                        executeTransfer(senderUid, receiverPhone, amount, notes, onSuccess, onFailure)
                    }.addOnFailureListener {
                        onFailure(Exception("Failed to register receiver on-the-fly: ${it.localizedMessage}"))
                    }
                    return@addOnSuccessListener
                }

                val receiver = receiverDoc.toObject(FirebaseWalletUser::class.java)
                if (receiver == null) {
                    onFailure(Exception("Failed to decode receiver profile."))
                    return@addOnSuccessListener
                }

                if (receiver.isBlocked) {
                    onFailure(Exception("Receiver account is currently blocked and cannot accept payments."))
                    return@addOnSuccessListener
                }

                if (sender.uid == receiver.uid) {
                    onFailure(Exception("Cannot send money to your own mobile number."))
                    return@addOnSuccessListener
                }

                // 3. Perform atomic transaction
                firestore!!.runTransaction { transaction ->
                    val freshSenderDoc = transaction.get(usersColl.document(sender.uid))
                    val freshReceiverDoc = transaction.get(usersColl.document(receiver.uid))

                    val freshSenderBal = freshSenderDoc.getDouble("walletBalance") ?: 0.0
                    if (freshSenderBal < amount) {
                        throw FirebaseException("Insufficient funds verified in cloud sync.")
                    }

                    val freshReceiverBal = freshReceiverDoc.getDouble("walletBalance") ?: 0.0

                    // Subtract from sender, add to receiver
                    transaction.update(usersColl.document(sender.uid), "walletBalance", freshSenderBal - amount)
                    transaction.update(usersColl.document(receiver.uid), "walletBalance", freshReceiverBal + amount)

                    // Write transaction log
                    val txId = txColl.document().id
                    val fbTx = FirebaseTransaction(
                        transactionId = txId,
                        senderUid = sender.uid,
                        receiverUid = receiver.uid,
                        senderName = sender.fullName,
                        receiverName = receiver.fullName,
                        amount = amount,
                        type = "Debit", // Debit from sender POV, Credit is displayed for receiver
                        status = "Success",
                        description = notes,
                        createdAt = System.currentTimeMillis()
                    )
                    transaction.set(txColl.document(txId), fbTx)

                    // Write dual credit log for Receiver
                    val rxTxId = txColl.document().id
                    val fbRxTx = FirebaseTransaction(
                        transactionId = rxTxId,
                        senderUid = sender.uid,
                        receiverUid = receiver.uid,
                        senderName = sender.fullName,
                        receiverName = receiver.fullName,
                        amount = amount,
                        type = "Credit",
                        status = "Success",
                        description = notes,
                        createdAt = System.currentTimeMillis()
                    )
                    transaction.set(txColl.document(rxTxId), fbRxTx)

                    fbTx
                }.addOnSuccessListener { tx ->
                    onSuccess(tx)
                }.addOnFailureListener {
                    onFailure(it)
                }
            }.addOnFailureListener {
                onFailure(it)
            }
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    // LOAD MONEY (Simulated deposit to Firestore)
    fun executeDeposit(
        uid: String,
        amount: Double,
        notes: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(true)
            return
        }

        val userRef = firestore!!.collection("users").document(uid)
        val txColl = firestore!!.collection("transactions")

        firestore!!.runTransaction { transaction ->
            val userSnap = transaction.get(userRef)
            val balance = userSnap.getDouble("walletBalance") ?: 0.0
            val newBalance = balance + amount

            transaction.update(userRef, "walletBalance", newBalance)

            val txId = txColl.document().id
            val fbTx = FirebaseTransaction(
                transactionId = txId,
                senderUid = "Bank Gateway",
                receiverUid = uid,
                senderName = "Bank Gateway",
                receiverName = userSnap.getString("fullName") ?: "User",
                amount = amount,
                type = "Credit",
                status = "Success",
                description = notes,
                createdAt = System.currentTimeMillis()
            )
            transaction.set(txColl.document(txId), fbTx)
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            Log.e(TAG, "Deposit transaction failed", it)
            onComplete(false)
        }
    }

    // UPLOAD IMAGE TO STORAGE
    fun uploadImage(
        uid: String,
        imageType: String, // "profile", "aadhaar", "pan"
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isFirebaseAvailable || storage == null) {
            // Offline fallback path
            Log.w(TAG, "Firebase Storage unavailable. Simulating file upload.")
            onSuccess("https://storage.googleapis.com/simulated-novapay/${uid}_${imageType}.jpg")
            return
        }

        val ref = storage!!.reference.child("users/$uid/${imageType}_${System.currentTimeMillis()}.jpg")
        val stream = ByteArrayInputStream(imageBytes)

        ref.putStream(stream)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener {
                    onFailure(it)
                }
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    // NOTIFICATIONS RETRIEVAL
    fun listenToNotifications(uid: String): Flow<List<FirebaseNotification>> = callbackFlow {
        if (!isFirebaseAvailable || firestore == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = firestore!!.collection("notifications")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listing notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(FirebaseNotification::class.java))
                }
            }

        awaitClose { listener.remove() }
    }

    fun markNotificationAsRead(notifId: String) {
        if (!isFirebaseAvailable || firestore == null) return
        firestore!!.collection("notifications").document(notifId).update("isRead", true)
    }

    // ADMIN TRIGGER: COMPLETE USER ACTION
    fun adminSetKycStatus(userUid: String, status: String, onComplete: (Boolean) -> Unit) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(true)
            return
        }

        firestore!!.collection("users").document(userUid).update("kycStatus", status)
            .addOnSuccessListener {
                // Send an admin notification regarding KYC status change
                val notifId = firestore!!.collection("notifications").document().id
                val message = when (status) {
                    "Approved" -> "Congratulations! Your KYC review is successful. Wallet limit restriction removed."
                    "Rejected" -> "KYC review rejected. Please upload clear identity documents to resume transfers."
                    else -> "Your KYC details are undergoing administrative review."
                }
                
                val notif = FirebaseNotification(
                    id = notifId,
                    uid = userUid,
                    title = "KYC Verification Update",
                    message = message,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
                firestore!!.collection("notifications").document(notifId).set(notif)
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun adminSetBlocked(userUid: String, isBlocked: Boolean, onComplete: (Boolean) -> Unit) {
        if (!isFirebaseAvailable || firestore == null) {
            onComplete(true)
            return
        }

        firestore!!.collection("users").document(userUid).update("isBlocked", isBlocked)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun adminSendNotification(recipientUid: String, title: String, message: String) {
        if (!isFirebaseAvailable || firestore == null) return
        val notifId = firestore!!.collection("notifications").document().id
        val notif = FirebaseNotification(
            id = notifId,
            uid = recipientUid,
            title = title,
            message = message,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        firestore!!.collection("notifications").document(notifId).set(notif)
    }

    fun logout() {
        if (isFirebaseAvailable) {
            auth?.signOut()
        }
    }
}
