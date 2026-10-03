package com.example.data.auth

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.UserEntity
import com.example.model.Semester
import com.example.model.Technology
import com.example.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserProfile) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthManager(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return@withContext AuthResult.Error("Email address cannot be blank.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Password cannot be blank.")
        }

        val userEntity = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext AuthResult.Error("No student account found with this email. Please sign up first.")

        if (userEntity.accountStatus == "SUSPENDED" || userEntity.accountStatus == "BANNED") {
            return@withContext AuthResult.Error("Your account is currently suspended. Please contact the administrator.")
        }

        val hash = hashPassword(password)
        if (userEntity.passwordHash.isNotBlank() && userEntity.passwordHash != hash) {
            return@withContext AuthResult.Error("Incorrect password. Please try again.")
        }

        // Set current session
        userDao.clearCurrentSession()
        userDao.setCurrentSession(userEntity.id)

        AuthResult.Success(userEntity.toUserProfile())
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return@withContext AuthResult.Error("Please enter your full student name.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters.")
        }
        if (trimmedPhone.length < 9) {
            return@withContext AuthResult.Error("Please enter a valid contact phone number.")
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext AuthResult.Error("An account with this email already exists. Please sign in.")
        }

        val isAdmin = trimmedEmail.equals("mdmaheraz65@gmail.com", ignoreCase = true)
        val newUserId = "user_" + UUID.randomUUID().toString().take(8)

        val newEntity = UserEntity(
            id = newUserId,
            name = trimmedName,
            email = trimmedEmail,
            passwordHash = hashPassword(password),
            phone = trimmedPhone,
            college = college,
            technologyId = technology.id,
            semesterId = semester.id,
            profilePhotoRes = R.drawable.avatar_rahim_1789677826412,
            profilePhotoUri = null,
            role = if (isAdmin) "ADMIN" else "USER",
            accountStatus = "ACTIVE",
            isCurrentSession = true
        )

        userDao.clearCurrentSession()
        userDao.insertUser(newEntity)

        AuthResult.Success(newEntity.toUserProfile())
    }

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        userDao.clearCurrentSession()
    }

    suspend fun seedDefaultUsersIfEmpty() = withContext(Dispatchers.IO) {
        val current = userDao.getCurrentUser()
        if (current == null) {
            val admin = UserEntity(
                id = "user_admin_1",
                name = "Rahim Ahmed (Admin)",
                email = "mdmaheraz65@gmail.com",
                passwordHash = hashPassword("admin123"),
                phone = "01712-345678",
                college = "Feni Computer Institute",
                technologyId = Technology.CSE.id,
                semesterId = Semester.SEM_3.id,
                profilePhotoRes = R.drawable.avatar_rahim_1789677826412,
                profilePhotoUri = null,
                role = "ADMIN",
                accountStatus = "ACTIVE",
                isCurrentSession = true
            )

            val studentRafsan = UserEntity(
                id = "seller_rafsan_1",
                name = "Rafsan Ahmed",
                email = "rafsan.civil@gmail.com",
                passwordHash = hashPassword("student123"),
                phone = "01819-123456",
                college = "Feni Polytechnic Institute",
                technologyId = Technology.CIVIL.id,
                semesterId = Semester.SEM_4.id,
                profilePhotoRes = R.drawable.avatar_rahim_1789677826412,
                profilePhotoUri = null,
                role = "USER",
                accountStatus = "ACTIVE",
                isCurrentSession = false
            )

            val studentTahmid = UserEntity(
                id = "seller_tahmid_1",
                name = "Tahmidul Islam",
                email = "tahmid.fci@gmail.com",
                passwordHash = hashPassword("student123"),
                phone = "01711-987654",
                college = "Feni Computer Institute",
                technologyId = Technology.CSE.id,
                semesterId = Semester.SEM_3.id,
                profilePhotoRes = R.drawable.avatar_nusrat_1789735573987,
                profilePhotoUri = null,
                role = "USER",
                accountStatus = "ACTIVE",
                isCurrentSession = false
            )

            userDao.insertUsers(listOf(admin, studentRafsan, studentTahmid))
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun UserEntity.toUserProfile(): UserProfile {
            return UserProfile(
                id = id,
                name = name,
                email = email,
                phone = phone,
                college = college,
                technology = Technology.fromId(technologyId),
                currentSemester = Semester.fromId(semesterId),
                profilePhotoRes = profilePhotoRes,
                profilePhotoUri = profilePhotoUri,
                role = role,
                accountStatus = accountStatus
            )
        }
    }
}
