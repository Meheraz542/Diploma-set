package com.example.data

import android.content.Context
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

object MarketplaceRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var database: AppDatabase? = null
    private var authManager: AuthManager? = null

    // Current Logged in User (Default Rahim Ahmed, admin email mdmaheraz65@gmail.com)
    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "user_rahim_1",
            name = "Rahim Ahmed",
            email = "mdmaheraz65@gmail.com",
            phone = "01712-345678",
            college = "Feni Computer Institute",
            technology = Technology.CSE,
            currentSemester = Semester.SEM_3,
            profilePhotoRes = R.drawable.avatar_rahim_1789677826412,
            role = "ADMIN",
            accountStatus = "ACTIVE"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Logged in state
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // All Book Sets
    private val _bookSets = MutableStateFlow<List<BookSet>>(emptyList())
    val bookSets: StateFlow<List<BookSet>> = _bookSets.asStateFlow()

    // Saved Sets
    private val _savedSetIds = MutableStateFlow<Set<String>>(setOf("set_civil_4th_1", "set_mech_4th_1"))
    val savedSetIds: StateFlow<Set<String>> = _savedSetIds.asStateFlow()

    // Conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Mega Deals
    private val _megaDeals = MutableStateFlow<List<MegaDeal>>(emptyList())
    val megaDeals: StateFlow<List<MegaDeal>> = _megaDeals.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // All Registered Users for Admin panel
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    init {
        // Load in-memory fallback first
        loadInitialData()
    }

    fun initialize(context: Context) {
        val db = AppDatabase.getInstance(context)
        database = db
        val auth = AuthManager(context)
        authManager = auth

        scope.launch(Dispatchers.IO) {
            // Seed defaults into database if empty
            auth.seedDefaultUsersIfEmpty()
            seedDatabaseIfEmpty(db)

            // Collect real-time updates from Room DB
            launch {
                db.userDao().getCurrentUserFlow().collect { userEntity ->
                    if (userEntity != null) {
                        _currentUser.value = userEntity.toDomain()
                        _isLoggedIn.value = true
                    } else {
                        _isLoggedIn.value = false
                    }
                }
            }

            launch {
                db.userDao().getAllUsersFlow().collect { users ->
                    if (users.isNotEmpty()) {
                        _allUsers.value = users.map { it.toDomain() }
                    }
                }
            }

            launch {
                db.bookSetDao().getAllBookSetsWithBooksFlow().collect { list ->
                    if (list.isNotEmpty()) {
                        _bookSets.value = list.map { it.toDomain() }
                    }
                }
            }

            launch {
                db.bookSetDao().getSavedBookSetIdsFlow().collect { ids ->
                    _savedSetIds.value = ids.toSet()
                }
            }

            launch {
                db.megaDealDao().getAllMegaDealsFlow().collect { deals ->
                    if (deals.isNotEmpty()) {
                        _megaDeals.value = deals.map { it.toDomain() }
                    }
                }
            }

            launch {
                db.chatDao().getAllConversationsFlow().collect { convs ->
                    if (convs.isNotEmpty()) {
                        // Gather all messages for each conversation
                        db.chatDao().getAllMessagesFlow().collect { messages ->
                            _conversations.value = convs.map { it.toDomain(messages) }
                        }
                    }
                }
            }

            launch {
                db.notificationDao().getAllNotificationsFlow().collect { notifs ->
                    if (notifs.isNotEmpty()) {
                        _notifications.value = notifs.map { it.toDomain() }
                    }
                }
            }
        }
    }

    private suspend fun seedDatabaseIfEmpty(db: AppDatabase) {
        val existingSets = db.bookSetDao().getBookSetWithBooksById("set_civil_4th_1")
        if (existingSets == null) {
            // Seed book sets
            _bookSets.value.forEach { set ->
                val entity = set.toEntity()
                val bookEntities = set.includedBooks.map { it.toEntity(set.id) }
                db.bookSetDao().insertBookSetWithBooks(entity, bookEntities)
            }

            // Seed conversations & messages
            _conversations.value.forEach { conv ->
                db.chatDao().insertConversation(conv.toEntity())
                conv.messages.forEach { msg ->
                    db.chatDao().insertMessage(msg.toEntity(conv.id))
                }
            }

            // Seed mega deals
            _megaDeals.value.forEach { deal ->
                db.megaDealDao().insertMegaDeal(deal.toEntity())
            }

            // Seed notifications
            _notifications.value.forEach { notif ->
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    private fun loadInitialData() {
        val civilBooks = listOf(
            IncludedBook("b1", "set_civil_4th_1", "Building Construction", "Civil Engineering", "S. M. Ahmed", "4th Edition", "CE-401"),
            IncludedBook("b2", "set_civil_4th_1", "Structural Mechanics", "Structural Engineering", "Dr. K. Zaman", "3rd Edition", "CE-402"),
            IncludedBook("b3", "set_civil_4th_1", "Surveying Principles", "Civil Survey", "Engr. M. Rahman", "2nd Edition", "CE-403"),
            IncludedBook("b4", "set_civil_4th_1", "Estimating & Costing", "Civil Management", "P. K. Das", "5th Edition", "CE-404"),
            IncludedBook("b5", "set_civil_4th_1", "Applied Mathematics-IV", "Common Subject", "Prof. N. Haque", "6th Edition", "MATH-401"),
            IncludedBook("b6", "set_civil_4th_1", "Environmental Studies", "General Engineering", "S. Sultana", "1st Edition", "ENV-401")
        )

        val elecBooks = listOf(
            IncludedBook("b7", "set_elec_3rd_1", "Circuit Analysis", "Electrical Circuits", "Charles Alexander", "5th Edition", "EE-301"),
            IncludedBook("b8", "set_elec_3rd_1", "Electrical Machines-I", "Power Systems", "B. L. Theraja", "4th Edition", "EE-302"),
            IncludedBook("b9", "set_elec_3rd_1", "Electronics Devices", "Analog Electronics", "Boylestad", "11th Edition", "EE-303"),
            IncludedBook("b10", "set_elec_3rd_1", "Electrical Measurement", "Instrumentation", "A. K. Sawhney", "8th Edition", "EE-304"),
            IncludedBook("b11", "set_elec_3rd_1", "Mathematics-III", "General Mathematics", "H. K. Dass", "3rd Edition", "MATH-301")
        )

        val mechBooks = listOf(
            IncludedBook("b12", "set_mech_4th_1", "Thermodynamics", "Thermal Engineering", "P. K. Nag", "6th Edition", "ME-401"),
            IncludedBook("b13", "set_mech_4th_1", "Fluid Mechanics & Machinery", "Fluid Dynamics", "R. K. Bansal", "9th Edition", "ME-402"),
            IncludedBook("b14", "set_mech_4th_1", "Manufacturing Process", "Workshop Tech", "H. S. Bawa", "2nd Edition", "ME-403"),
            IncludedBook("b15", "set_mech_4th_1", "Machine Drawing", "Mechanical CAD", "K. L. Narayana", "4th Edition", "ME-404"),
            IncludedBook("b16", "set_mech_4th_1", "Applied Mechanics", "Engineering Physics", "S. S. Bhavikatti", "3rd Edition", "ME-405")
        )

        val cseBooks = listOf(
            IncludedBook("b17", "set_cse_3rd_1", "Data Structures & Algorithms", "Computer Science", "Seymour Lipschutz", "Revised", "CST-301"),
            IncludedBook("b18", "set_cse_3rd_1", "Database Management Systems", "Software Eng", "Korth & Silberschatz", "7th Edition", "CST-302"),
            IncludedBook("b19", "set_cse_3rd_1", "Object Oriented Programming (Java)", "Programming", "Herbert Schildt", "11th Edition", "CST-303"),
            IncludedBook("b20", "set_cse_3rd_1", "Digital Electronics", "Hardware Architecture", "M. Morris Mano", "5th Edition", "CST-304"),
            IncludedBook("b21", "set_cse_3rd_1", "Discrete Mathematics", "Mathematics", "Kenneth Rosen", "8th Edition", "CST-305")
        )

        val initialSets = listOf(
            BookSet(
                id = "set_civil_4th_1",
                sellerId = "seller_rafsan_1",
                sellerName = "Rafsan Ahmed",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Civil Technology",
                sellerSem = "4th Sem",
                sellerPhone = "01711-223344",
                sellerEmail = "rafsan@example.com",
                title = "Building Construction",
                subject = "Civil Engineering",
                technology = Technology.CIVIL,
                semester = Semester.SEM_4,
                college = "Feni Polytechnic Institute",
                totalPrice = 550,
                overallCondition = SetCondition.USED,
                description = "Complete 6-book semester set in good condition. Some highlights are there. No missing pages. Perfect for 4th semester students.",
                includedBooks = civilBooks,
                coverDrawableRes = R.drawable.book_cover_civil_1789677775416,
                status = SetStatus.AVAILABLE,
                isSaved = true
            ),
            BookSet(
                id = "set_mech_1st_1",
                sellerId = "seller_tanjim",
                sellerName = "Tanjim Hasan",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Mechanical Technology",
                sellerSem = "1st Sem",
                sellerPhone = "01822-334455",
                sellerEmail = "tanjim@example.com",
                title = "Engineering Mechanics",
                subject = "Mechanical Engineering",
                technology = Technology.MECHANICAL,
                semester = Semester.SEM_1,
                college = "Feni Polytechnic Institute",
                totalPrice = 450,
                overallCondition = SetCondition.GOOD,
                description = "Full 1st semester fresh set including Engineering Mechanics, Basic Electricity, Physics-1, Math-1, and Bangla-1. All covers intact.",
                includedBooks = mechBooks.take(4),
                coverDrawableRes = R.drawable.book_cover_mech_1789677808779,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_elec_3rd_1",
                sellerId = "seller_nusrat",
                sellerName = "Nusrat Jahan",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Electrical Technology",
                sellerSem = "3rd Sem",
                sellerPhone = "01933-445566",
                sellerEmail = "nusrat@example.com",
                title = "Circuit Analysis",
                subject = "Electrical Engineering",
                technology = Technology.ELECTRICAL,
                semester = Semester.SEM_3,
                college = "Feni Polytechnic Institute",
                totalPrice = 550,
                overallCondition = SetCondition.LIKE_NEW,
                description = "Barely used 5-book electrical set. Pristine condition with protective transparent covers on every book.",
                includedBooks = elecBooks,
                coverDrawableRes = R.drawable.book_cover_elec_1789677789123,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_mech_4th_1",
                sellerId = "seller_sabbir",
                sellerName = "Sabbir Ahmed",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Mechanical Technology",
                sellerSem = "4th Sem",
                sellerPhone = "01544-556677",
                sellerEmail = "sabbir@example.com",
                title = "Thermodynamics",
                subject = "Mechanical Technology",
                technology = Technology.MECHANICAL,
                semester = Semester.SEM_4,
                college = "Feni Polytechnic Institute",
                totalPrice = 600,
                overallCondition = SetCondition.USED,
                description = "Complete 4th semester Mechanical set including Thermodynamics, Fluid Dynamics, and Manufacturing. Clean pages with formula notes.",
                includedBooks = mechBooks,
                coverDrawableRes = R.drawable.book_cover_mech_1789677808779,
                status = SetStatus.AVAILABLE,
                isSaved = true
            ),
            BookSet(
                id = "set_civil_5th_1",
                sellerId = "seller_afsana",
                sellerName = "Afsana Mim",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Civil Technology",
                sellerSem = "5th Sem",
                sellerPhone = "01655-667788",
                sellerEmail = "afsana@example.com",
                title = "Structural Analysis",
                subject = "Civil Engineering",
                technology = Technology.CIVIL,
                semester = Semester.SEM_5,
                college = "Feni Polytechnic Institute",
                totalPrice = 650,
                overallCondition = SetCondition.GOOD,
                description = "5th semester Civil set. Includes Structural Analysis, Design of RCC Structures, Transportation Engineering, and Geotechnical Eng.",
                includedBooks = civilBooks.take(5),
                coverDrawableRes = R.drawable.book_cover_civil_1789677775416,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_cse_3rd_1",
                sellerId = "seller_imran",
                sellerName = "Imran Hossain",
                sellerCollege = "Feni Computer Institute",
                sellerTech = "Computer Science & Technology",
                sellerSem = "3rd Sem",
                sellerPhone = "01766-778899",
                sellerEmail = "imran@example.com",
                title = "Data Structures & Java OOP",
                subject = "Computer Science",
                technology = Technology.CSE,
                semester = Semester.SEM_3,
                college = "Feni Computer Institute",
                totalPrice = 520,
                overallCondition = SetCondition.LIKE_NEW,
                description = "Complete CST 3rd semester book set. Includes Data Structures, Java OOP, DBMS, and Digital Electronics. Very well kept.",
                includedBooks = cseBooks,
                coverDrawableRes = R.drawable.book_cover_cse_1789735549141,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_feni_poly_civil_1",
                sellerId = "seller_feni_1",
                sellerName = "Kamrul Islam",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Civil Technology",
                sellerSem = "4th Sem",
                sellerPhone = "01811-224466",
                sellerEmail = "kamrul.fpi@example.com",
                title = "Civil Engineering 4th Semester Set",
                subject = "Civil Technology",
                technology = Technology.CIVIL,
                semester = Semester.SEM_4,
                college = "Feni Polytechnic Institute",
                totalPrice = 520,
                overallCondition = SetCondition.GOOD,
                description = "Feni Polytechnic Institute Civil 4th semester complete book set. Structural mechanics, Surveying, and Estimating.",
                includedBooks = civilBooks.take(4),
                coverDrawableRes = R.drawable.book_cover_civil_1789677775416,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_feni_poly_aidt_1",
                sellerId = "seller_feni_aidt",
                sellerName = "Nabila Rahman",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Architecture & Interior Design Technology",
                sellerSem = "3rd Sem",
                sellerPhone = "01855-667788",
                sellerEmail = "nabila.fpi@example.com",
                title = "AIDT Architecture & Interior Design 3rd Sem",
                subject = "Architecture & Interior Design",
                technology = Technology.AIDT,
                semester = Semester.SEM_3,
                college = "Feni Polytechnic Institute",
                totalPrice = 620,
                overallCondition = SetCondition.LIKE_NEW,
                description = "Feni Polytechnic Institute AIDT Department specialized set with Architectural Drafting, Interior Design Principles, and Materials.",
                includedBooks = listOf(
                    IncludedBook("aidt_1", "set_feni_poly_aidt_1", "Architectural Graphics & Drafting", "Architecture", "Francis Ching", "5th Edition", "AIDT-301"),
                    IncludedBook("aidt_2", "set_feni_poly_aidt_1", "Interior Design Fundamentals", "Design", "John Pile", "4th Edition", "AIDT-302"),
                    IncludedBook("aidt_3", "set_feni_poly_aidt_1", "Building Materials in Architecture", "Civil/AIDT", "S. C. Rangwala", "2nd Edition", "AIDT-303")
                ),
                coverDrawableRes = R.drawable.book_cover_civil_1789677775416,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_feni_poly_power_1",
                sellerId = "seller_feni_power",
                sellerName = "Mehedi Hasan",
                sellerCollege = "Feni Polytechnic Institute",
                sellerTech = "Power Technology",
                sellerSem = "2nd Sem",
                sellerPhone = "01866-998877",
                sellerEmail = "mehedi.fpi@example.com",
                title = "Power Technology 2nd Semester Set",
                subject = "Power Engineering",
                technology = Technology.POWER,
                semester = Semester.SEM_2,
                college = "Feni Polytechnic Institute",
                totalPrice = 480,
                overallCondition = SetCondition.GOOD,
                description = "Feni Polytechnic Institute Power Department 2nd semester complete bundle. Applied Mechanics, Basic Electricity, and Engine Principles.",
                includedBooks = listOf(
                    IncludedBook("pow_1", "set_feni_poly_power_1", "Automobile & Power Fundamentals", "Power Tech", "William Crouse", "3rd Edition", "POW-201"),
                    IncludedBook("pow_2", "set_feni_poly_power_1", "Basic Electricity", "Electrical", "B. L. Theraja", "4th Edition", "EE-201"),
                    IncludedBook("pow_3", "set_feni_poly_power_1", "Applied Thermodynamics", "Thermal", "P. K. Nag", "5th Edition", "ME-201")
                ),
                coverDrawableRes = R.drawable.book_cover_mech_1789677808779,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_feni_computer_cse_1",
                sellerId = "seller_fci_cse",
                sellerName = "Sakib Al Hasan",
                sellerCollege = "Feni Computer Institute",
                sellerTech = "Computer Science & Technology",
                sellerSem = "3rd Sem",
                sellerPhone = "01833-112233",
                sellerEmail = "sakib.fci@example.com",
                title = "FCI Computer Technology 3rd Sem Set",
                subject = "Computer Science & Technology",
                technology = Technology.CSE,
                semester = Semester.SEM_3,
                college = "Feni Computer Institute",
                totalPrice = 540,
                overallCondition = SetCondition.LIKE_NEW,
                description = "Feni Computer Institute (FCI) CST Department 3rd semester book set. Data Structures, Java OOP, DBMS, Web Development.",
                includedBooks = cseBooks,
                coverDrawableRes = R.drawable.book_cover_cse_1789735549141,
                status = SetStatus.AVAILABLE,
                isSaved = false
            ),
            BookSet(
                id = "set_feni_computer_telecom_1",
                sellerId = "seller_fci_telecom",
                sellerName = "Farhana Akter",
                sellerCollege = "Feni Computer Institute",
                sellerTech = "Telecommunication Technology",
                sellerSem = "4th Sem",
                sellerPhone = "01844-556677",
                sellerEmail = "farhana.fci@example.com",
                title = "FCI Telecom Technology 4th Sem Set",
                subject = "Telecommunication Engineering",
                technology = Technology.TELECOM,
                semester = Semester.SEM_4,
                college = "Feni Computer Institute",
                totalPrice = 580,
                overallCondition = SetCondition.GOOD,
                description = "Feni Computer Institute Telecommunication Technology 4th semester bundle. Signal & Systems, Optical Fiber Communication, and Wireless Networks.",
                includedBooks = listOf(
                    IncludedBook("tel_1", "set_feni_computer_telecom_1", "Electronic Communication Systems", "Telecommunication", "Wayne Tomasi", "5th Edition", "TEL-401"),
                    IncludedBook("tel_2", "set_feni_computer_telecom_1", "Optical Fiber Communications", "Optics", "Gerd Keiser", "4th Edition", "TEL-402"),
                    IncludedBook("tel_3", "set_feni_computer_telecom_1", "Data & Computer Communications", "Networking", "William Stallings", "8th Edition", "TEL-403")
                ),
                coverDrawableRes = R.drawable.book_cover_elec_1789677789123,
                status = SetStatus.AVAILABLE,
                isSaved = false
            )
        )
        _bookSets.value = initialSets

        // Initial Conversations
        val initialMessages = listOf(
            ChatMessage("m1", "user_rahim_1", "Rahim Ahmed", "Hi, is this book set still available?", "10:12 AM", true),
            ChatMessage("m2", "seller_rafsan_1", "Rafsan Ahmed", "Yes, it is available. Asking price is Tk 550.", "10:15 AM", false),
            ChatMessage("m3", "user_rahim_1", "Rahim Ahmed", "Would you take Tk 500 for the full set?", "10:17 AM", true),
            ChatMessage("m4", "seller_rafsan_1", "Rafsan Ahmed", "Sure, books are in great condition without torn pages.", "10:20 AM", false),
            ChatMessage("m5", "user_rahim_1", "Rahim Ahmed", "Sounds good! Let's arrange a deal on campus.", "10:22 AM", true)
        )

        val conv1 = Conversation(
            id = "conv_1",
            buyerId = "user_rahim_1",
            sellerId = "seller_rafsan_1",
            otherUserName = "Rafsan Ahmed",
            otherUserCollege = "Feni Polytechnic Institute",
            otherUserAvatarRes = R.drawable.avatar_rahim_1789677826412,
            setId = "set_civil_4th_1",
            setTitle = "Building Construction",
            setPrice = 550,
            lastMessage = "Can you do Tk 500?",
            timestamp = "10:24 AM",
            unreadCount = 2,
            isOnline = true,
            messages = initialMessages
        )

        val conv2 = Conversation(
            id = "conv_2",
            buyerId = "user_rahim_1",
            sellerId = "seller_nusrat",
            otherUserName = "Nusrat Jahan",
            otherUserCollege = "Feni Polytechnic Institute",
            otherUserAvatarRes = R.drawable.avatar_nusrat_1789735573987,
            setId = "set_elec_3rd_1",
            setTitle = "Circuit Analysis",
            setPrice = 550,
            lastMessage = "Okay, see you on campus tomorrow.",
            timestamp = "09:45 AM",
            unreadCount = 1,
            isOnline = true,
            messages = listOf(
                ChatMessage("m20", "seller_nusrat", "Nusrat Jahan", "The books are ready for pickup.", "09:40 AM", false),
                ChatMessage("m21", "user_rahim_1", "Rahim Ahmed", "Can you meet at the campus library?", "09:42 AM", true),
                ChatMessage("m22", "seller_nusrat", "Nusrat Jahan", "Okay, see you on campus tomorrow.", "09:45 AM", false)
            )
        )

        val conv3 = Conversation(
            id = "conv_3",
            buyerId = "user_rahim_1",
            sellerId = "seller_tanjim",
            otherUserName = "Tanjim Hasan",
            otherUserCollege = "Feni Polytechnic Institute",
            otherUserAvatarRes = R.drawable.avatar_rahim_1789677826412,
            setId = "set_mech_1st_1",
            setTitle = "Engineering Mechanics",
            setPrice = 450,
            lastMessage = "Book is still available?",
            timestamp = "Yesterday",
            unreadCount = 0,
            isOnline = false,
            messages = listOf(
                ChatMessage("m30", "user_rahim_1", "Rahim Ahmed", "Book is still available?", "Yesterday", true),
                ChatMessage("m31", "seller_tanjim", "Tanjim Hasan", "Yes, available bro!", "Yesterday", false)
            )
        )

        _conversations.value = listOf(conv1, conv2, conv3)

        // Mega deals
        _megaDeals.value = listOf(
            MegaDeal(
                dealId = "deal_1",
                setId = "set_civil_4th_1",
                setTitle = "Building Construction (6 Books)",
                buyerId = "user_rahim_1",
                buyerName = "Rahim Ahmed",
                sellerId = "seller_rafsan_1",
                sellerName = "Rafsan Ahmed",
                finalPrice = 500,
                location = "College Main Gate",
                date = "15 September 2026",
                time = "4:30 PM",
                status = MegaDealStatus.CONFIRMED
            )
        )

        // Notifications
        _notifications.value = listOf(
            AppNotification(
                id = "notif_1",
                type = "MEGA_DEAL_CONFIRMED",
                title = "Mega Deal Confirmed! 🎉",
                message = "Rafsan Ahmed confirmed your deal for Building Construction set at Tk 500.",
                timeAgo = "10 min ago",
                isRead = false,
                relatedSetId = "set_civil_4th_1",
                relatedDealId = "deal_1"
            ),
            AppNotification(
                id = "notif_2",
                type = "NEW_MESSAGE",
                title = "New Message from Nusrat",
                message = "Okay, see you on campus tomorrow.",
                timeAgo = "1 hour ago",
                isRead = false,
                relatedSetId = "set_elec_3rd_1"
            ),
            AppNotification(
                id = "notif_3",
                type = "MEETING_REMINDER",
                title = "Meeting Reminder ⏰",
                message = "Exchange scheduled for College Main Gate at 4:30 PM.",
                timeAgo = "3 hours ago",
                isRead = true,
                relatedDealId = "deal_1"
            )
        )

        _allUsers.value = listOf(
            _currentUser.value,
            UserProfile("seller_rafsan_1", "Rafsan Ahmed", "rafsan@example.com", "01711-223344", "Feni Polytechnic Institute", Technology.CIVIL, Semester.SEM_4, R.drawable.avatar_rahim_1789677826412),
            UserProfile("seller_nusrat", "Nusrat Jahan", "nusrat@example.com", "01933-445566", "Feni Polytechnic Institute", Technology.ELECTRICAL, Semester.SEM_3, R.drawable.avatar_nusrat_1789735573987),
            UserProfile("seller_tanjim", "Tanjim Hasan", "tanjim@example.com", "01822-334455", "Feni Polytechnic Institute", Technology.MECHANICAL, Semester.SEM_1, R.drawable.avatar_rahim_1789677826412),
            UserProfile("seller_sabbir", "Sabbir Ahmed", "sabbir@example.com", "01544-556677", "Feni Polytechnic Institute", Technology.MECHANICAL, Semester.SEM_4, R.drawable.avatar_nusrat_1789735573987),
            UserProfile("seller_afsana", "Afsana Mim", "afsana@example.com", "01655-667788", "Feni Polytechnic Institute", Technology.CIVIL, Semester.SEM_5, R.drawable.avatar_nusrat_1789735573987),
            UserProfile("seller_imran", "Imran Hossain", "imran@example.com", "01766-778899", "Feni Computer Institute", Technology.CSE, Semester.SEM_3, R.drawable.avatar_rahim_1789677826412)
        )
    }

    // Toggle Save / Bookmark
    fun toggleSaveSet(setId: String) {
        val currentlySaved = _savedSetIds.value.contains(setId)
        val newSavedState = !currentlySaved

        _savedSetIds.update { set ->
            if (currentlySaved) set - setId else set + setId
        }
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(isSaved = newSavedState) else it }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.bookSetDao().updateSavedStatus(setId, newSavedState)
            }
        }
    }

    // Publish New Book Set
    fun publishSet(
        title: String,
        subject: String,
        technology: Technology,
        semester: Semester,
        college: String,
        totalPrice: Int,
        overallCondition: SetCondition,
        description: String,
        includedBooks: List<IncludedBook>,
        imageUrls: List<String> = emptyList()
    ): BookSet {
        val user = _currentUser.value
        val newId = "set_" + UUID.randomUUID().toString().take(8)
        val coverRes = when (technology) {
            Technology.CIVIL -> R.drawable.book_cover_civil_1789677775416
            Technology.ELECTRICAL, Technology.TELECOM -> R.drawable.book_cover_elec_1789677789123
            Technology.CSE -> R.drawable.book_cover_cse_1789735549141
            else -> R.drawable.book_cover_mech_1789677808779
        }
        val newSet = BookSet(
            id = newId,
            sellerId = user.id,
            sellerName = user.name,
            sellerCollege = college.ifBlank { user.college },
            sellerTech = technology.displayName,
            sellerSem = semester.displayName,
            sellerPhone = user.phone,
            sellerEmail = user.email,
            title = title,
            subject = subject,
            technology = technology,
            semester = semester,
            college = college.ifBlank { user.college },
            totalPrice = totalPrice,
            overallCondition = overallCondition,
            description = description,
            includedBooks = includedBooks,
            coverDrawableRes = coverRes,
            imageUrls = imageUrls,
            status = SetStatus.AVAILABLE
        )

        _bookSets.update { listOf(newSet) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val entity = newSet.toEntity()
                val bookEntities = includedBooks.map { it.toEntity(newId) }
                db.bookSetDao().insertBookSetWithBooks(entity, bookEntities)
            }
        }

        return newSet
    }

    // Send a message
    fun sendMessage(conversationId: String, text: String) {
        val user = _currentUser.value
        val newMsg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestamp = "Just now",
            isMe = true
        )
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        messages = conv.messages + newMsg,
                        lastMessage = text,
                        timestamp = "Just now"
                    )
                } else conv
            }
        }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().insertMessage(newMsg.toEntity(conversationId))
                db.chatDao().updateConversationSnippet(conversationId, text, "Just now")
            }
        }
    }

    // Start or get conversation for a book set
    fun startOrGetConversation(set: BookSet): String {
        val existing = _conversations.value.find { it.setId == set.id }
        if (existing != null) return existing.id

        val newId = "conv_" + UUID.randomUUID().toString().take(8)
        val initialMsg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            senderId = _currentUser.value.id,
            senderName = _currentUser.value.name,
            text = "Hello! Is the complete set '${set.title}' available?",
            timestamp = "Just now",
            isMe = true
        )
        val newConv = Conversation(
            id = newId,
            buyerId = _currentUser.value.id,
            sellerId = set.sellerId,
            otherUserName = set.sellerName,
            otherUserCollege = set.sellerCollege,
            otherUserAvatarRes = R.drawable.avatar_rahim_1789677826412,
            setId = set.id,
            setTitle = set.title,
            setPrice = set.totalPrice,
            lastMessage = "Started conversation about ${set.title}",
            timestamp = "Just now",
            unreadCount = 0,
            isOnline = true,
            messages = listOf(initialMsg)
        )
        _conversations.update { listOf(newConv) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().insertConversation(newConv.toEntity())
                db.chatDao().insertMessage(initialMsg.toEntity(newId))
            }
        }

        return newId
    }

    // Create a Mega Deal
    fun createMegaDeal(
        set: BookSet,
        finalPrice: Int,
        location: String,
        date: String,
        time: String
    ): MegaDeal {
        val user = _currentUser.value
        val newDeal = MegaDeal(
            dealId = "deal_" + UUID.randomUUID().toString().take(8),
            setId = set.id,
            setTitle = set.title,
            buyerId = user.id,
            buyerName = user.name,
            sellerId = set.sellerId,
            sellerName = set.sellerName,
            finalPrice = finalPrice,
            location = location,
            date = date,
            time = time,
            status = MegaDealStatus.PENDING
        )
        _megaDeals.update { listOf(newDeal) + it }

        // Also add system message in conversation
        val conv = _conversations.value.find { it.setId == set.id }
        if (conv != null) {
            val systemMsg = ChatMessage(
                id = "m_deal_" + UUID.randomUUID().toString().take(6),
                senderId = user.id,
                senderName = user.name,
                text = "🔥 Mega Deal Request Sent: Tk $finalPrice at $location on $date ($time)",
                timestamp = "Just now",
                isMe = true
            )
            _conversations.update { list ->
                list.map {
                    if (it.id == conv.id) it.copy(messages = it.messages + systemMsg, lastMessage = "🔥 Mega Deal Requested")
                    else it
                }
            }
            database?.let { db ->
                scope.launch(Dispatchers.IO) {
                    db.chatDao().insertMessage(systemMsg.toEntity(conv.id))
                    db.chatDao().updateConversationSnippet(conv.id, systemMsg.text, "Just now")
                }
            }
        }

        // Add notification
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "MEGA_DEAL_REQUEST",
            title = "Mega Deal Request Created",
            message = "Waiting for ${set.sellerName} to confirm deal for '${set.title}'.",
            timeAgo = "Just now",
            relatedSetId = set.id,
            relatedDealId = newDeal.dealId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().insertMegaDeal(newDeal.toEntity())
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }

        return newDeal
    }

    // Confirm Mega Deal
    fun confirmMegaDeal(dealId: String) {
        _megaDeals.update { list ->
            list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.CONFIRMED) else it }
        }
        val deal = _megaDeals.value.find { it.dealId == dealId } ?: return
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "MEGA_DEAL_CONFIRMED",
            title = "Mega Deal Confirmed! ✅",
            message = "Deal confirmed for '${deal.setTitle}' at Tk ${deal.finalPrice}. Meet at ${deal.location}.",
            timeAgo = "Just now",
            relatedSetId = deal.setId,
            relatedDealId = dealId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.CONFIRMED.name)
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    // Decline Mega Deal
    fun declineMegaDeal(dealId: String) {
        _megaDeals.update { list ->
            list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.CANCELLED) else it }
        }
        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.CANCELLED.name)
            }
        }
    }

    // Physical Exchange Completed -> Mark Set As SOLD
    fun markSetAsSold(dealId: String?, setId: String) {
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(status = SetStatus.SOLD) else it }
        }
        if (dealId != null) {
            _megaDeals.update { list ->
                list.map { if (it.dealId == dealId) it.copy(status = MegaDealStatus.COMPLETED) else it }
            }
        }
        val set = _bookSets.value.find { it.id == setId }
        val notif = AppNotification(
            id = "notif_" + UUID.randomUUID().toString().take(6),
            type = "SET_SOLD",
            title = "Book Set Sold! 🎓",
            message = "Physical exchange completed for '${set?.title ?: "Set"}'. Marked as SOLD.",
            timeAgo = "Just now",
            relatedSetId = setId
        )
        _notifications.update { listOf(notif) + it }

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                db.bookSetDao().updateBookSetStatus(setId, SetStatus.SOLD.name)
                if (dealId != null) {
                    db.megaDealDao().updateMegaDealStatus(dealId, MegaDealStatus.COMPLETED.name)
                }
                db.notificationDao().insertNotification(notif.toEntity())
            }
        }
    }

    // Update Profile
    fun updateUserProfile(
        name: String,
        email: String = _currentUser.value.email,
        phone: String,
        college: String,
        tech: Technology,
        sem: Semester
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = tech,
            currentSemester = sem
        )
        _currentUser.value = updated

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val currentEntity = db.userDao().getUserById(updated.id)
                if (currentEntity != null) {
                    db.userDao().updateUser(
                        currentEntity.copy(
                            name = name,
                            email = email,
                            phone = phone,
                            college = college,
                            technologyId = tech.id,
                            semesterId = sem.id
                        )
                    )
                }
            }
        }
    }

    // Update Profile Photo
    fun updateProfilePhotoUri(uri: String?) {
        val updated = _currentUser.value.copy(profilePhotoUri = uri)
        _currentUser.value = updated

        database?.let { db ->
            scope.launch(Dispatchers.IO) {
                val currentEntity = db.userDao().getUserById(updated.id)
                if (currentEntity != null) {
                    db.userDao().updateUser(currentEntity.copy(profilePhotoUri = uri))
                }
            }
        }
    }

    // Authentication: Login & Logout & SignUp
    fun logout() {
        _isLoggedIn.value = false
        scope.launch(Dispatchers.IO) {
            authManager?.signOut()
        }
    }

    suspend fun performSignIn(email: String, password: String): AuthResult {
        val auth = authManager ?: return AuthResult.Error("Database initializing. Please retry.")
        val result = auth.signIn(email, password)
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            _isLoggedIn.value = true
        }
        return result
    }

    suspend fun performSignUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): AuthResult {
        val auth = authManager ?: return AuthResult.Error("Database initializing. Please retry.")
        val result = auth.signUp(name, email, password, phone, college, technology, semester)
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            _isLoggedIn.value = true
        }
        return result
    }

    fun login(
        name: String = "Rahim Ahmed",
        email: String = "mdmaheraz65@gmail.com",
        phone: String = "01712-345678",
        college: String = "Feni Computer Institute",
        tech: Technology = Technology.CSE,
        sem: Semester = Semester.SEM_3
    ) {
        val updated = _currentUser.value.copy(
            name = name,
            email = email,
            phone = phone,
            college = college,
            technology = tech,
            currentSemester = sem
        )
        _currentUser.value = updated
        _isLoggedIn.value = true

        scope.launch(Dispatchers.IO) {
            val db = database ?: return@launch
            val existing = db.userDao().getUserByEmail(email)
            if (existing != null) {
                db.userDao().clearCurrentSession()
                db.userDao().setCurrentSession(existing.id)
            }
        }
    }

    // Admin: Suspend User
    fun adminSuspendUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "SUSPENDED") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "SUSPENDED"))
                }
            }
        }
    }

    // Admin: Ban User
    fun adminBanUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "BANNED") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "BANNED"))
                }
            }
        }
    }

    // Admin: Restore User
    fun adminRestoreUser(userId: String) {
        _allUsers.update { list ->
            list.map { if (it.id == userId) it.copy(accountStatus = "ACTIVE") else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.let { db ->
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.userDao().updateUser(user.copy(accountStatus = "ACTIVE"))
                }
            }
        }
    }

    // Admin: Remove Inappropriate Set
    fun adminRemoveSet(setId: String) {
        _bookSets.update { list ->
            list.map { if (it.id == setId) it.copy(status = SetStatus.REMOVED) else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.bookSetDao()?.updateBookSetStatus(setId, SetStatus.REMOVED.name)
        }
    }

    // Mark notification as read
    fun markNotificationRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
        scope.launch(Dispatchers.IO) {
            database?.notificationDao()?.markAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        scope.launch(Dispatchers.IO) {
            database?.notificationDao()?.markAllAsRead()
        }
    }

    // Converters between entities and domain models
    private fun UserEntity.toDomain(): UserProfile {
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

    private fun BookSetWithBooks.toDomain(): BookSet {
        val cond = try { SetCondition.valueOf(bookSet.overallCondition) } catch (e: Exception) { SetCondition.GOOD }
        val stat = try { SetStatus.valueOf(bookSet.status) } catch (e: Exception) { SetStatus.AVAILABLE }
        val imgs = if (bookSet.imageUrlsString.isBlank()) emptyList() else bookSet.imageUrlsString.split("||")
        return BookSet(
            id = bookSet.id,
            sellerId = bookSet.sellerId,
            sellerName = bookSet.sellerName,
            sellerCollege = bookSet.sellerCollege,
            sellerTech = bookSet.sellerTech,
            sellerSem = bookSet.sellerSem,
            sellerPhone = bookSet.sellerPhone,
            sellerEmail = bookSet.sellerEmail,
            title = bookSet.title,
            subject = bookSet.subject,
            technology = Technology.fromId(bookSet.technologyId),
            semester = Semester.fromId(bookSet.semesterId),
            college = bookSet.college,
            totalPrice = bookSet.totalPrice,
            overallCondition = cond,
            description = bookSet.description,
            includedBooks = books.map { it.toDomain() },
            coverDrawableRes = bookSet.coverDrawableRes,
            imageUrls = imgs,
            status = stat,
            isSaved = bookSet.isSaved,
            createdAt = bookSet.createdAt
        )
    }

    private fun IncludedBookEntity.toDomain(): IncludedBook {
        return IncludedBook(
            id = id,
            setId = setId,
            bookName = bookName,
            subject = subject,
            author = author,
            edition = edition,
            bookCode = bookCode,
            condition = condition
        )
    }

    private fun BookSet.toEntity(): BookSetEntity {
        return BookSetEntity(
            id = id,
            sellerId = sellerId,
            sellerName = sellerName,
            sellerCollege = sellerCollege,
            sellerTech = sellerTech,
            sellerSem = sellerSem,
            sellerPhone = sellerPhone,
            sellerEmail = sellerEmail,
            title = title,
            subject = subject,
            technologyId = technology.id,
            semesterId = semester.id,
            college = college,
            totalPrice = totalPrice,
            overallCondition = overallCondition.name,
            description = description,
            coverDrawableRes = coverDrawableRes,
            imageUrlsString = imageUrls.joinToString("||"),
            status = status.name,
            isSaved = isSaved,
            createdAt = createdAt
        )
    }

    private fun IncludedBook.toEntity(setId: String): IncludedBookEntity {
        return IncludedBookEntity(
            id = id,
            setId = setId,
            bookName = bookName,
            subject = subject,
            author = author,
            edition = edition,
            bookCode = bookCode,
            condition = condition
        )
    }

    private fun ConversationEntity.toDomain(messages: List<ChatMessageEntity>): Conversation {
        return Conversation(
            id = id,
            buyerId = buyerId,
            sellerId = sellerId,
            otherUserName = otherUserName,
            otherUserCollege = otherUserCollege,
            otherUserAvatarRes = otherUserAvatarRes,
            setId = setId,
            setTitle = setTitle,
            setPrice = setPrice,
            lastMessage = lastMessage,
            timestamp = timestamp,
            unreadCount = unreadCount,
            isOnline = isOnline,
            messages = messages.filter { it.conversationId == id }.map { it.toDomain() }
        )
    }

    private fun Conversation.toEntity(): ConversationEntity {
        return ConversationEntity(
            id = id,
            buyerId = buyerId,
            sellerId = sellerId,
            otherUserName = otherUserName,
            otherUserCollege = otherUserCollege,
            otherUserAvatarRes = otherUserAvatarRes,
            setId = setId,
            setTitle = setTitle,
            setPrice = setPrice,
            lastMessage = lastMessage,
            timestamp = timestamp,
            unreadCount = unreadCount,
            isOnline = isOnline
        )
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            isMe = isMe
        )
    }

    private fun ChatMessage.toEntity(convId: String): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            conversationId = convId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            isMe = isMe
        )
    }

    private fun MegaDealEntity.toDomain(): MegaDeal {
        val stat = try { MegaDealStatus.valueOf(status) } catch (e: Exception) { MegaDealStatus.PENDING }
        return MegaDeal(
            dealId = dealId,
            setId = setId,
            setTitle = setTitle,
            buyerId = buyerId,
            buyerName = buyerName,
            sellerId = sellerId,
            sellerName = sellerName,
            finalPrice = finalPrice,
            location = location,
            date = date,
            time = time,
            status = stat,
            createdAt = createdAt
        )
    }

    private fun MegaDeal.toEntity(): MegaDealEntity {
        return MegaDealEntity(
            dealId = dealId,
            setId = setId,
            setTitle = setTitle,
            buyerId = buyerId,
            buyerName = buyerName,
            sellerId = sellerId,
            sellerName = sellerName,
            finalPrice = finalPrice,
            location = location,
            date = date,
            time = time,
            status = status.name,
            createdAt = createdAt
        )
    }

    private fun NotificationEntity.toDomain(): AppNotification {
        return AppNotification(
            id = id,
            type = type,
            title = title,
            message = message,
            timeAgo = timeAgo,
            isRead = isRead,
            relatedSetId = relatedSetId,
            relatedDealId = relatedDealId
        )
    }

    private fun AppNotification.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            type = type,
            title = title,
            message = message,
            timeAgo = timeAgo,
            isRead = isRead,
            relatedSetId = relatedSetId,
            relatedDealId = relatedDealId
        )
    }
}
