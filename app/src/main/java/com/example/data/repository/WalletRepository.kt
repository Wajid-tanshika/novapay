package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.TransactionDao
import com.example.data.model.UserEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WalletRepository(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao
) {
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsForUser(phone: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForUser(phone)
    }

    fun getUserByPhone(phone: String): Flow<UserEntity?> {
        return userDao.getUserByPhone(phone)
    }

    suspend fun getUserByPhoneSync(phone: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByPhoneSync(phone)
    }

    suspend fun registerUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    /**
     * Seed initial mock users and transactions to allow immediate interactive testing
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        // Demo numbers and users have been removed as requested.
    }

    /**
     * Perform a wallet-to-wallet transfer securely
     */
    suspend fun transferMoney(
        senderPhone: String,
        receiverPhone: String,
        amount: Double,
        notes: String
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext Result.failure(Exception("Amount must be greater than zero"))
        }

        val sender = userDao.getUserByPhoneSync(senderPhone)
            ?: return@withContext Result.failure(Exception("Sender account not found"))

        if (sender.isFrozen) {
            return@withContext Result.failure(Exception("Your account is frozen. Please contact customer support."))
        }

        if (sender.walletBalance < amount) {
            // Log as a failed transaction first
            val failedTx = TransactionEntity(
                senderPhone = senderPhone,
                receiverPhone = receiverPhone,
                senderName = sender.fullName,
                receiverName = "Unknown",
                amount = amount,
                status = "Failed",
                type = "Transfer",
                notes = "$notes (Failed: Insufficient funds)",
                auditHash = TransactionEntity.generateAuditHash(senderPhone, receiverPhone, amount, System.currentTimeMillis(), "Failed: Insufficient funds")
            )
            transactionDao.insertTransaction(failedTx)
            return@withContext Result.failure(Exception("Insufficient wallet balance. Available: ₹${sender.walletBalance}"))
        }

        var receiver = userDao.getUserByPhoneSync(receiverPhone)
        if (receiver == null) {
            val newReceiver = UserEntity(
                phoneNumber = receiverPhone,
                fullName = "NovaPay User ${receiverPhone.takeLast(4)}",
                dob = "01/01/2000",
                email = "user${receiverPhone.takeLast(4)}@novapay.in",
                kycStatus = "Approved",
                walletBalance = 0.0,
                aadhaarNumber = "",
                panCard = ""
            )
            userDao.insertUser(newReceiver)
            receiver = newReceiver
        }

        if (receiver.isFrozen) {
            return@withContext Result.failure(Exception("Receiver account is currently frozen and cannot receive money"))
        }

        if (senderPhone == receiverPhone) {
            return@withContext Result.failure(Exception("Cannot send money to your own mobile number"))
        }

        try {
            // Update sender balance
            val updatedSender = sender.copy(walletBalance = sender.walletBalance - amount)
            userDao.updateUser(updatedSender)

            // Update receiver balance
            val updatedReceiver = receiver.copy(walletBalance = receiver.walletBalance + amount)
            userDao.updateUser(updatedReceiver)

            // Insert success transaction
            val timestamp = System.currentTimeMillis()
            val tx = TransactionEntity(
                senderPhone = senderPhone,
                receiverPhone = receiverPhone,
                senderName = sender.fullName,
                receiverName = receiver.fullName,
                amount = amount,
                timestamp = timestamp,
                status = "Success",
                type = "Transfer",
                notes = notes,
                auditHash = TransactionEntity.generateAuditHash(senderPhone, receiverPhone, amount, timestamp, notes)
            )
            transactionDao.insertTransaction(tx)
            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(Exception("Transaction failed: ${e.localizedMessage}"))
        }
    }

    /**
     * Self deposit money (simulated gateway)
     */
    suspend fun depositMoney(
        phone: String,
        amount: Double,
        notes: String = "Added via NetBanking"
    ): Result<TransactionEntity> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext Result.failure(Exception("Amount must be greater than zero"))
        }

        val user = userDao.getUserByPhoneSync(phone)
            ?: return@withContext Result.failure(Exception("User account not found"))

        if (user.isFrozen) {
            return@withContext Result.failure(Exception("Account is frozen. Cannot deposit."))
        }

        try {
            val updatedUser = user.copy(walletBalance = user.walletBalance + amount)
            userDao.updateUser(updatedUser)

            val timestamp = System.currentTimeMillis()
            val tx = TransactionEntity(
                senderPhone = "Bank Gateway",
                receiverPhone = phone,
                senderName = "Bank Gateway",
                receiverName = user.fullName,
                amount = amount,
                timestamp = timestamp,
                status = "Success",
                type = "Deposit",
                notes = notes,
                auditHash = TransactionEntity.generateAuditHash("Bank Gateway", phone, amount, timestamp, notes)
            )
            transactionDao.insertTransaction(tx)
            Result.success(tx)
        } catch (e: Exception) {
            Result.failure(Exception("Deposit failed: ${e.localizedMessage}"))
        }
    }

    /**
     * Submit KYC documents
     */
    suspend fun submitKyc(
        phone: String,
        aadhaarNum: String,
        panNum: String,
        aadhaarImg: String?,
        panImg: String?
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (aadhaarNum.length != 12) {
            return@withContext Result.failure(Exception("Aadhaar number must be exactly 12 digits"))
        }
        if (panNum.length != 10) {
            return@withContext Result.failure(Exception("PAN must be exactly 10 characters"))
        }

        val user = userDao.getUserByPhoneSync(phone)
            ?: return@withContext Result.failure(Exception("User account not found"))

        try {
            val updatedUser = user.copy(
                aadhaarNumber = aadhaarNum,
                panCard = panNum,
                aadhaarImage = aadhaarImg ?: "simulated_aadhaar_path",
                panImage = panImg ?: "simulated_pan_path",
                kycStatus = "Pending"
            )
            userDao.updateUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(Exception("KYC submission failed: ${e.localizedMessage}"))
        }
    }

    /**
     * Admin action: approve/reject KYC
     */
    suspend fun adminReviewKyc(
        phone: String,
        approve: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserByPhoneSync(phone) ?: return@withContext false
        val newStatus = if (approve) "Approved" else "Rejected"
        val updatedUser = user.copy(kycStatus = newStatus)
        userDao.updateUser(updatedUser)
        true
    }

    /**
     * Admin action: toggle user frozen state
     */
    suspend fun adminToggleFreeze(
        phone: String
    ): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserByPhoneSync(phone) ?: return@withContext false
        val updatedUser = user.copy(isFrozen = !user.isFrozen)
        userDao.updateUser(updatedUser)
        updatedUser.isFrozen
    }
}
