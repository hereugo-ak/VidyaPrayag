package com.littlebridge.enrollplus.feature.idcard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Logger

/**
 * Daily job that checks for ID cards expiring within 30 days.
 * Logs warnings for admin follow-up.
 *
 * Per ID_CARD_GENERATION_SPEC.md §14: IdCardExpiryCheckJob — Daily.
 */
class IdCardExpiryCheckJob(
    private val service: IdCardService = IdCardService(),
) {
    private val logger = Logger.getLogger("IdCardExpiryCheckJob")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            delay(5 * 60 * 1000L) // 5-minute initial delay — let server warm up
            while (true) {
                try {
                    checkExpiringCards()
                } catch (e: Exception) {
                    logger.warning("Expiry check failed: ${e.message}")
                }
                // Run every 24 hours
                delay(24 * 60 * 60 * 1000L)
            }
        }
    }

    private suspend fun checkExpiringCards() {
        val expiring = service.getExpiringCards(withinDays = 30)
        if (expiring.isEmpty()) return

        // Group by school for targeted notifications
        val bySchool = expiring.groupBy { it.schoolId ?: "unknown" }

        logger.info("ID Card Expiry Check: ${expiring.size} cards expiring within 30 days across ${bySchool.size} schools")

        for ((schoolId, cards) in bySchool) {
            logger.info("School $schoolId: ${cards.size} cards expiring")
            for (card in cards) {
                logger.info(
                    "  ID Card expiring: ${card.personName} (${card.personType}) — valid till: ${card.validTill}"
                )
            }
        }
    }
}
