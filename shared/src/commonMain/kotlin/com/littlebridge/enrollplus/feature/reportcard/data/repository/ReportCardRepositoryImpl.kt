/*
 * File: ReportCardRepositoryImpl.kt
 * Module: feature.reportcard.data.repository
 */
package com.littlebridge.enrollplus.feature.reportcard.data.repository

import com.littlebridge.enrollplus.feature.reportcard.data.remote.ReportCardApi
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import com.littlebridge.enrollplus.feature.reportcard.domain.repository.ReportCardRepository

class ReportCardRepositoryImpl(
    private val api: ReportCardApi,
) : ReportCardRepository(api)
