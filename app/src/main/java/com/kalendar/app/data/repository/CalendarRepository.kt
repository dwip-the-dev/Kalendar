package com.kalendar.app.data.repository

import com.kalendar.app.data.local.dao.AccountDao
import com.kalendar.app.data.local.dao.CalendarDao
import com.kalendar.app.data.local.entity.AccountEntity
import com.kalendar.app.data.local.entity.CalendarEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for calendar and account management.
 */
class CalendarRepository(
    private val calendarDao: CalendarDao,
    private val accountDao: AccountDao
) {
    // Calendar operations
    fun getAllCalendars(): Flow<List<CalendarEntity>> = calendarDao.getAllCalendars()

    fun getVisibleCalendars(): Flow<List<CalendarEntity>> = calendarDao.getVisibleCalendars()

    fun getCalendarsByAccount(accountId: Long): Flow<List<CalendarEntity>> =
        calendarDao.getCalendarsByAccount(accountId)

    suspend fun getCalendarById(id: Long): CalendarEntity? = calendarDao.getCalendarById(id)

    suspend fun toggleCalendarVisibility(id: Long, isVisible: Boolean) {
        calendarDao.setVisibility(id, isVisible)
    }

    suspend fun createCalendar(calendar: CalendarEntity): Long = calendarDao.insert(calendar)

    suspend fun getCalendarCount(): Int = calendarDao.getCount()

    // Account operations
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getEnabledAccounts(): Flow<List<AccountEntity>> = accountDao.getEnabledAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun getAccountByEmail(email: String): AccountEntity? = accountDao.getAccountByEmail(email)

    suspend fun addAccount(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun createAccount(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun removeAccount(account: AccountEntity) = accountDao.delete(account)

    suspend fun toggleAccountEnabled(id: Long, isEnabled: Boolean) {
        accountDao.setEnabled(id, isEnabled)
    }

    suspend fun updateSyncInfo(id: Long, time: Long, token: String?) {
        accountDao.updateSyncInfo(id, time, token)
    }
}
