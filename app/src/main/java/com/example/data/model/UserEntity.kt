package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String, // Acts as unique index (wallet-to-wallet identifier)
    val fullName: String,
    val dob: String,
    val email: String,
    val profilePhoto: String? = null, // Path or type descriptor
    val aadhaarNumber: String = "",
    val panCard: String = "",
    val aadhaarImage: String? = null,
    val panImage: String? = null,
    val kycStatus: String = "None", // "None", "Pending", "Approved", "Rejected"
    val walletBalance: Double = 500.0, // Pre-loaded with ₹500 starting credit
    val isFrozen: Boolean = false,
    val qrCodeData: String = "",
    val securePin: String = ""
)
