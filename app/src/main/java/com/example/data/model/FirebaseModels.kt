package com.example.data.model

import com.google.firebase.firestore.PropertyName

data class FirebaseWalletUser(
    val uid: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val profilePhoto: String? = null,
    val walletBalance: Double = 0.0,
    val walletPinHash: String = "",
    val kycStatus: String = "Pending", // Pending / Approved / Rejected
    val aadhaarNumber: String = "",
    val panNumber: String = "",
    val aadhaarImage: String? = null,
    val panImage: String? = null,
    @get:PropertyName("isBlocked") @set:PropertyName("isBlocked") var isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isAdmin: Boolean = false
)

data class FirebaseTransaction(
    val transactionId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val senderName: String = "",
    val receiverName: String = "",
    val amount: Double = 0.0,
    val type: String = "Debit", // Credit / Debit
    val status: String = "Success", // Pending / Success / Failed
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FirebaseWallet(
    val uid: String = "",
    val availableBalance: Double = 0.0,
    val totalCredit: Double = 0.0,
    val totalDebit: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirebaseQrCode(
    val uid: String = "",
    val qrData: String = "",
    val qrImage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class FirebaseNotification(
    val id: String = "",
    val uid: String = "", // Recipient UID
    val title: String = "",
    val message: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
