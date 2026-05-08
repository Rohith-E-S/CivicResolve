import Complaint from "../models/complaint.model.js";
import User from "../models/user.model.js";
import mongoose from "mongoose";

export const getUserAnalytics = async (req, res) => {
  try {
    const userId = req.user._id;
    const user = await User.findById(userId);
    const { period = "This Month" } = req.query;

    // Calculate date filter
    let dateFilter = { user: userId };
    const now = new Date();
    
    if (period === "This Week") {
      const weekAgo = new Date(now);
      weekAgo.setDate(weekAgo.getDate() - 7);
      dateFilter.createdAt = { $gte: weekAgo };
    } else if (period === "This Month") {
      const monthAgo = new Date(now);
      monthAgo.setMonth(monthAgo.getMonth() - 1);
      dateFilter.createdAt = { $gte: monthAgo };
    } else if (period === "Last 3 Months") {
      const threeMonthsAgo = new Date(now);
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
      dateFilter.createdAt = { $gte: threeMonthsAgo };
    }

    // 1. Basic Stats
    const complaints = await Complaint.find(dateFilter);
    const totalReports = complaints.length;
    const resolvedCount = complaints.filter(c => c.status.toLowerCase() === "resolved").length;
    const activeCount = complaints.filter(c => c.status.toLowerCase() === "in_progress").length;
    const newCount = complaints.filter(c => c.status.toLowerCase() === "new").length;

    // 2. Weekly Trend (Last 7 days)
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
    sevenDaysAgo.setHours(0, 0, 0, 0);

    const weeklyTrendData = await Complaint.aggregate([
      {
        $match: {
          user: userId,
          createdAt: { $gte: sevenDaysAgo }
        }
      },
      {
        $group: {
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
          count: { $sum: 1 }
        }
      },
      { $sort: { _id: 1 } }
    ]);

    // Fill in missing days
    const weeklyTrend = [];
    const weekLabels = [];
    const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    for (let i = 0; i < 7; i++) {
      const d = new Date(sevenDaysAgo);
      d.setDate(d.getDate() + i);
      const dateStr = d.toISOString().split("T")[0];
      const match = weeklyTrendData.find(item => item._id === dateStr);
      weeklyTrend.push(match ? match.count : 0);
      weekLabels.push(days[d.getDay()]);
    }

    // 3. Category Breakdown
    const categoryBreakdown = await Complaint.aggregate([
      { $match: { user: userId } },
      {
        $group: {
          _id: "$category",
          count: { $sum: 1 }
        }
      },
      { $sort: { count: -1 } }
    ]);

    const categories = categoryBreakdown.map(c => ({
      label: c._id,
      count: c.count
    }));

    // 4. Community Rank (based on resolved count in the same city/district)
    const location = user.homeDistrict || complaints[0]?.city || "Global";
    
    // Compare resolved counts of all users in the same district
    const rankingData = await Complaint.aggregate([
      { $match: { status: "resolved", city: new RegExp(location, "i") } },
      {
        $group: {
          _id: "$user",
          resolvedCount: { $sum: 1 }
        }
      },
      { $sort: { resolvedCount: -1 } }
    ]);

    const totalParticipants = rankingData.length || 1;
    const userRankIndex = rankingData.findIndex(r => r._id.toString() === userId.toString());
    
    // Calculate percentile rank (e.g., top 10%)
    let communityRankPct = 100;
    if (userRankIndex !== -1) {
      communityRankPct = Math.max(1, Math.round(((userRankIndex + 1) / totalParticipants) * 100));
    } else if (resolvedCount > 0) {
        // If they have resolved something but weren't in the list for some reason
        communityRankPct = 50;
    }

    res.status(200).json({
      success: true,
      data: {
        totalReports,
        resolvedCount,
        activeCount,
        newCount,
        communityRankPct,
        location,
        period: period,
        weeklyTrend,
        weekLabels,
        categoryBreakdown: categories
      }
    });

  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Error fetching analytics: " + error.message
    });
  }
};

export const getAdminAnalytics = async (req, res) => {
  try {
    const { period = "This Month" } = req.query;
    
    // Calculate date filter based on period
    let dateFilter = {};
    const now = new Date();
    
    if (period === "This Week") {
      const weekAgo = new Date(now);
      weekAgo.setDate(weekAgo.getDate() - 7);
      dateFilter = { createdAt: { $gte: weekAgo } };
    } else if (period === "This Month") {
      const monthAgo = new Date(now);
      monthAgo.setMonth(monthAgo.getMonth() - 1);
      dateFilter = { createdAt: { $gte: monthAgo } };
    } else if (period === "Last 3 Months") {
      const threeMonthsAgo = new Date(now);
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
      dateFilter = { createdAt: { $gte: threeMonthsAgo } };
    }
    // "All Time" means empty filter

    // 1. Stats based on period
    const allComplaints = await Complaint.find(dateFilter);
    const totalComplaints = allComplaints.length;
    const resolvedCount = allComplaints.filter(c => c.status.toLowerCase() === "resolved").length;
    const activeCount = allComplaints.filter(c => c.status.toLowerCase() === "in_progress").length;
    const newCount = allComplaints.filter(c => c.status.toLowerCase() === "new").length;

    // 2. Resolution Rate & Avg Time
    const resolvedComplaints = allComplaints.filter(c => c.status.toLowerCase() === "resolved" && c.updatedAt);
    let totalDays = 0;
    resolvedComplaints.forEach(c => {
      const created = new Date(c.createdAt);
      const updated = new Date(c.updatedAt);
      const diff = (updated - created) / (1000 * 60 * 60 * 24);
      totalDays += diff;
    });

    const avgResolutionDays = resolvedComplaints.length > 0 ? (totalDays / resolvedComplaints.length) : 0;
    const resolutionRatePct = totalComplaints > 0 ? Math.round((resolvedCount / totalComplaints) * 100) : 0;

    // 3. Weekly Trends (Always 7 days for the chart labels, but we can respect period if needed)
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
    sevenDaysAgo.setHours(0, 0, 0, 0);

    const weeklyIncomingData = await Complaint.aggregate([
      { $match: { createdAt: { $gte: sevenDaysAgo } } },
      { 
        $group: { 
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } }, 
          count: { $sum: 1 } 
        } 
      },
      { $sort: { _id: 1 } }
    ]);

    const weeklyResolvedData = await Complaint.aggregate([
      { $match: { status: "resolved", updatedAt: { $gte: sevenDaysAgo } } },
      { 
        $group: { 
          _id: { $dateToString: { format: "%Y-%m-%d", date: "$updatedAt" } }, 
          count: { $sum: 1 } 
        } 
      },
      { $sort: { _id: 1 } }
    ]);

    const weeklyIncoming = [];
    const weeklyResolved = [];
    const weekLabels = [];
    const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    for (let i = 0; i < 7; i++) {
      const d = new Date(sevenDaysAgo);
      d.setDate(d.getDate() + i);
      const dateStr = d.toISOString().split("T")[0];
      
      const inMatch = weeklyIncomingData.find(item => item._id === dateStr);
      const resMatch = weeklyResolvedData.find(item => item._id === dateStr);
      
      weeklyIncoming.push(inMatch ? inMatch.count : 0);
      weeklyResolved.push(resMatch ? resMatch.count : 0);
      weekLabels.push(days[d.getDay()]);
    }

    // 4. Category Breakdown
    const categoryStats = await Complaint.aggregate([
      { $group: { _id: "$category", count: { $sum: 1 } } },
      { $sort: { count: -1 } }
    ]);

    // 5. District Breakdown
    const districtStats = await Complaint.aggregate([
      { $group: { _id: "$city", count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 10 }
    ]);

    // 6. Top Reporters
    const topReportersData = await Complaint.aggregate([
      { $group: { _id: "$user", count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 5 }
    ]);

    const topReporters = await Promise.all(topReportersData.map(async (r) => {
      const user = await User.findById(r._id);
      return { name: user ? user.fullName : "Unknown", count: r.count };
    }));

    // 7. Resolution Time Buckets
    const buckets = [
      { label: "<1 day", count: 0 },
      { label: "1-3 days", count: 0 },
      { label: "3-7 days", count: 0 },
      { label: "7+ days", count: 0 }
    ];

    resolvedComplaints.forEach(c => {
      const created = new Date(c.createdAt);
      const updated = new Date(c.updatedAt);
      const days = (updated - created) / (1000 * 60 * 60 * 24);
      if (days < 1) buckets[0].count++;
      else if (days < 3) buckets[1].count++;
      else if (days < 7) buckets[2].count++;
      else buckets[3].count++;
    });

    res.status(200).json({
      success: true,
      data: {
        jurisdiction: "Santa Clara County", // Hardcoded or from admin profile
        period: period,
        totalComplaints,
        resolvedCount,
        activeCount,
        newCount,
        avgResolutionDays,
        resolutionRatePct,
        weeklyIncoming,
        weeklyResolved,
        weekLabels,
        categoryBreakdown: categoryStats.map(s => ({ label: s._id, count: s.count })),
        districtBreakdown: districtStats.map(s => ({ label: s._id, count: s.count })),
        topReporters,
        resolutionTimeBuckets: buckets
      }
    });

  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Error fetching admin analytics: " + error.message
    });
  }
};
