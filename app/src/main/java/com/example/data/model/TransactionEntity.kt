package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderPhone: String,
    val receiverPhone: String,
    val senderName: String,
    val receiverName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Success", // "Success", "Pending", "Failed"
    val type: String = "Transfer",  // "Transfer", "Deposit", "Withdrawal", "Scan"
    val notes: String = "",
    val auditHash: String = "" // Integrity validation hash for secure auditing
) {
    companion object {
        fun generateAuditHash(
            sender: String,
            receiver: String,
            amount: Double,
            timestamp: Long,
            notes: String
        ): String {
            val rawString = "$sender|$receiver|$amount|$timestamp|$notes|nova_secure_salt_2026"
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(rawString.toByteArray(Charsets.UTF_8))
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "hash_fallback_err_${timestamp}"
            }
        }
    }
}
