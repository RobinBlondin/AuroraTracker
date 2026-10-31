package com.example.auroratracker.service

import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.mapper.SubscriptionMapper
import com.example.auroratracker.repository.SubscriptionRepository
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mapstruct.factory.Mappers
import kotlin.test.*

class InvalidPushSubscriptionTest {
    private val repository = mockk<SubscriptionRepository>()
    private val service = SubscriptionService(repository, mockk(), Mappers.getMapper(SubscriptionMapper::class.java), mockk())

    private fun subscription(email: String?) = SubscriptionDto(id = 42, userId = "owner", email = email,
        endpoint = "expired-endpoint", auth = "auth", p256dh = "key", firebaseToken = "expired-token",
        lat = 60.0, lon = 18.0, enabled = true)

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "   "])
    fun `push only subscriptions are deleted`(email: String?) {
        every { repository.existsByUserId("owner") } returns true
        every { repository.deleteByUserId("owner") } just Runs
        service.handleInvalidPushSubscription(subscription(email))
        verify(exactly = 1) { repository.deleteByUserId("owner") }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `email subscription is retained and all push credentials are cleared`() {
        val sub = subscription("owner@example.com")
        val saved = slot<com.example.auroratracker.entity.Subscription>()
        every { repository.save(capture(saved)) } answers { firstArg() }
        service.handleInvalidPushSubscription(sub)
        assertEquals("owner@example.com", saved.captured.email)
        assertEquals(42L, saved.captured.id)
        assertEquals("owner", saved.captured.userId)
        assertNull(saved.captured.auth)
        assertNull(saved.captured.p256dh)
        assertNull(saved.captured.firebaseToken)
        assertNull(saved.captured.endpoint, "Expired Web Push endpoint must also be removed")
        verify(exactly = 0) { repository.deleteByUserId(any()) }
    }

    @Test
    fun `cleanup opens a transaction before executing a derived delete`() {
        val transactionManager = object : org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()
            override fun doBegin(transaction: Any, definition: org.springframework.transaction.TransactionDefinition) {}
            override fun doCommit(status: org.springframework.transaction.support.DefaultTransactionStatus) {}
            override fun doRollback(status: org.springframework.transaction.support.DefaultTransactionStatus) {}
        }
        val factory = org.springframework.aop.framework.ProxyFactory(service)
        factory.addAdvice(org.springframework.transaction.interceptor.TransactionInterceptor(
            transactionManager, org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()
        ))
        val proxiedService = factory.proxy as SubscriptionService
        every { repository.existsByUserId("owner") } returns true
        every { repository.deleteByUserId("owner") } answers {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                "Derived JPA delete requires a transaction; internal calls bypass the service proxy")
        }
        proxiedService.handleInvalidPushSubscription(subscription(null))
    }

    @Test
    fun `already deleted push subscription is handled without saving it again`() {
        every { repository.existsByUserId("owner") } returns false
        service.handleInvalidPushSubscription(subscription(null))
        verify(exactly = 0) { repository.deleteByUserId(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }
}
