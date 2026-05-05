package com.example.complaintportal.data.model

data class AnalyticsResponse(
    val success: Boolean,
    val data: AnalyticsDataDto
)

data class AnalyticsDataDto(
    val totalReports: Int,
    val resolvedCount: Int,
    val activeCount: Int,
    val newCount: Int,
    val communityRankPct: Int,
    val location: String,
    val period: String,
    val weeklyTrend: List<Int>,
    val weekLabels: List<String>,
    val categoryBreakdown: List<CategoryStatDto>
)

data class CategoryStatDto(
    val label: String,
    val count: Int
)

data class AdminAnalyticsResponse(
    val success: Boolean,
    val data: AdminAnalyticsDataDto
)

data class AdminAnalyticsDataDto(
    val jurisdiction: String,
    val period: String,
    val totalComplaints: Int,
    val resolvedCount: Int,
    val activeCount: Int,
    val newCount: Int,
    val avgResolutionDays: Float,
    val resolutionRatePct: Int,
    val weeklyIncoming: List<Int>,
    val weeklyResolved: List<Int>,
    val weekLabels: List<String>,
    val categoryBreakdown: List<CategoryStatDto>,
    val districtBreakdown: List<CategoryStatDto>,
    val topReporters: List<ReporterDto>,
    val resolutionTimeBuckets: List<CategoryStatDto>
)

data class ReporterDto(
    val name: String,
    val count: Int
)
