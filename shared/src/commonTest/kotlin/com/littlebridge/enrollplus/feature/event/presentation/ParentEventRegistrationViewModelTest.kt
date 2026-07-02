package com.littlebridge.enrollplus.feature.event.presentation

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.event.domain.model.*
import com.littlebridge.enrollplus.feature.event.domain.repository.EventRegistrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

class ParentEventRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers loadEvents`() = runTest(testDispatcher) {
        val fakePrefs = FakePreferenceRepository(token = "test-token")
        val fakeRepo = FakeEventRepository()

        val viewModel = ParentEventRegistrationViewModel(fakeRepo, fakePrefs)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading, "Loading should be false after init")
        assertEquals(2, state.events.size, "Should have loaded 2 events")
    }

    @Test
    fun `loadEvents sets empty state on null token`() = runTest(testDispatcher) {
        val fakePrefs = FakePreferenceRepository(token = null)
        val fakeRepo = FakeEventRepository()

        val viewModel = ParentEventRegistrationViewModel(fakeRepo, fakePrefs)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading)
        assertTrue(state.events.isEmpty(), "Events should be empty when no token")
    }

    @Test
    fun `loadEvents handles network error`() = runTest(testDispatcher) {
        val fakePrefs = FakePreferenceRepository(token = "test-token")
        val fakeRepo = FakeEventRepository(shouldFail = true)

        val viewModel = ParentEventRegistrationViewModel(fakeRepo, fakePrefs)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage, "Should have error message on network failure")
    }

    @Test
    fun `cancelRegistration passes studentId to repository`() = runTest(testDispatcher) {
        val fakePrefs = FakePreferenceRepository(token = "test-token")
        val fakeRepo = FakeEventRepository()

        val viewModel = ParentEventRegistrationViewModel(fakeRepo, fakePrefs)

        viewModel.cancelRegistration(eventId = "evt-1", studentId = "student-123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("student-123", fakeRepo.lastCancelStudentId, "Should pass studentId to repository")
    }

    @Test
    fun `cancelRegistration passes null studentId when not provided`() = runTest(testDispatcher) {
        val fakePrefs = FakePreferenceRepository(token = "test-token")
        val fakeRepo = FakeEventRepository()

        val viewModel = ParentEventRegistrationViewModel(fakeRepo, fakePrefs)

        viewModel.cancelRegistration(eventId = "evt-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeRepo.lastCancelStudentId, "studentId should be null when not provided")
    }
}

private class FakePreferenceRepository(
    private val token: String? = "test-token",
) : PreferenceRepository {
    override fun getThemeName(): Flow<String> = MutableStateFlow("light")
    override suspend fun setThemeName(name: String) {}
    override fun getThemeMode(): Flow<String> = MutableStateFlow("light")
    override suspend fun setThemeMode(mode: String) {}
    override fun getCustomThemeId(): Flow<String?> = MutableStateFlow(null)
    override suspend fun setCustomThemeId(id: String?) {}
    override fun getUserRole(): Flow<String> = MutableStateFlow("PARENT")
    override suspend fun setUserRole(role: String) {}
    override fun getUserToken(): Flow<String?> = MutableStateFlow(token)
    override suspend fun setUserToken(token: String?) {}
    override fun getUserId(): Flow<String?> = MutableStateFlow("user-1")
    override suspend fun setUserId(userId: String?) {}
    override fun getRefreshToken(): Flow<String?> = MutableStateFlow("refresh-token")
    override suspend fun setRefreshToken(token: String?) {}
    override fun getProfileCompleted(): Flow<Boolean?> = MutableStateFlow(true)
    override suspend fun setProfileCompleted(completed: Boolean?) {}
    override fun getUserName(): Flow<String?> = MutableStateFlow("Test User")
    override suspend fun setUserName(name: String?) {}
    override fun getFcmToken(): Flow<String?> = MutableStateFlow(null)
    override suspend fun setFcmToken(token: String?) {}
    override fun getNotificationsDeclined(): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun setNotificationsDeclined(declined: Boolean) {}
    override suspend fun clearSession() {}
}

private class FakeEventRepository(
    private val shouldFail: Boolean = false,
) : EventRegistrationRepository {
    var lastCancelStudentId: String? = "UNSET"

    override suspend fun listParentEvents(token: String): NetworkResult<ApiResponse<ParentEventListResponse>> {
        if (shouldFail) return NetworkResult.Error("Network error")
        return NetworkResult.Success(ApiResponse(
            success = true,
            data = ParentEventListResponse(events = listOf(
                ParentEventDto(id = "evt-1", title = "PTM", registrationEnabled = true),
                ParentEventDto(id = "evt-2", title = "Annual Day", registrationEnabled = true),
            ))
        ))
    }

    override suspend fun getParentEventDetail(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = ParentEventDetailResponse(
            event = ParentEventDto(id = eventId, title = "Test Event"),
            slots = emptyList()
        )))

    override suspend fun register(token: String, eventId: String, request: RegisterRequest, clientRequestId: String?) =
        NetworkResult.Success(ApiResponse(success = true, data = RegistrationDto(
            id = "reg-1", eventId = eventId, status = "REGISTERED"
        )))

    override suspend fun cancelRegistration(token: String, eventId: String, request: CancelRegistrationRequest): NetworkResult<ApiResponse<Unit>> {
        lastCancelStudentId = request.studentId
        return NetworkResult.Success(ApiResponse(success = true, data = Unit))
    }

    override suspend fun listMyRegistrations(token: String) =
        NetworkResult.Success(ApiResponse(success = true, data = RegistrationListResponse()))

    override suspend fun reschedule(token: String, eventId: String, request: RescheduleRequest) =
        NetworkResult.Success(ApiResponse(success = true, data = RegistrationDto(
            id = "reg-1", eventId = eventId, status = "REGISTERED"
        )))

    override suspend fun getTeacherPtmEvents(token: String) =
        NetworkResult.Success(ApiResponse(success = true, data = TeacherPtmListResponse()))

    override suspend fun getTeacherPtmDetail(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = TeacherPtmEventDto(id = eventId)))

    override suspend fun getTeacherPtmSlots(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = emptyList<TeacherSlotDto>()))

    override suspend fun checkinParent(token: String, eventId: String, registrationId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = Unit))

    override suspend fun listAdminEvents(token: String) =
        NetworkResult.Success(ApiResponse(success = true, data = AdminEventListResponse()))

    override suspend fun listEventSlots(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = emptyList<SlotResponse>()))

    override suspend fun listAllRegistrations(token: String, status: String?, eventId: String?) =
        NetworkResult.Success(ApiResponse(success = true, data = AdminRegistrationListResponse()))

    override suspend fun listEventRegistrations(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = AdminRegistrationListResponse()))

    override suspend fun createSlot(token: String, eventId: String, request: CreateSlotRequest) =
        NetworkResult.Success(ApiResponse(success = true, data = SlotResponse(
            id = "slot-1", startTime = request.startTime, endTime = request.endTime, capacity = request.capacity
        )))

    override suspend fun autoGenerateSlots(token: String, eventId: String, request: AutoGenerateSlotsRequest) =
        NetworkResult.Success(ApiResponse(success = true, data = AutoGenerateSlotsResponse()))

    override suspend fun updateSlot(token: String, eventId: String, slotId: String, request: CreateSlotRequest) =
        NetworkResult.Success(ApiResponse(success = true, data = SlotResponse(
            id = slotId, startTime = request.startTime, endTime = request.endTime, capacity = request.capacity
        )))

    override suspend fun deleteSlot(token: String, eventId: String, slotId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = Unit))

    override suspend fun updateRegistrationConfig(token: String, eventId: String, request: UpdateRegistrationConfigRequest) =
        NetworkResult.Success(ApiResponse(success = true, data = Unit))

    override suspend fun cancelEvent(token: String, eventId: String) =
        NetworkResult.Success(ApiResponse(success = true, data = Unit))

    override suspend fun exportRegistrationsCsv(token: String, eventId: String) =
        NetworkResult.Success("Registration ID,Event Title\nreg-1,PTM")
}
