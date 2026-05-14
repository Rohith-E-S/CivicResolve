import User from "../models/user.model.js";

const POINTS = {
    report_resolved:   10,
    report_upvoted:     3,
    dispute_accepted:  20,
    verified_issue:     5,
};

export const awardPoints = async (userId, action, io) => {
    try {
        const pts = POINTS[action];
        if (!pts) return null;

        const user = await User.findById(userId).select("civicPoints rank");
        if (!user) return null;

        const oldRank = user.rank;

        const updatedUser = await User.findByIdAndUpdate(userId, {
            $inc: { civicPoints: pts }
        }, { new: true });

        const newRank = await updateRank(userId);

        if (io) {
            // Notify about points
            io.to(userId.toString()).emit("points_earned", {
                points: pts,
                action: action,
            });

            // Notify if rank changed
            if (oldRank !== newRank) {
                io.to(userId.toString()).emit("rank_up", {
                    newRank: newRank,
                });
            }
        }

        return pts;
    } catch (error) {
        console.error("Error awarding points:", error);
        return null;
    }
};

export const updateRank = async (userId) => {
    try {
        const user = await User.findById(userId).select("civicPoints");
        if (!user) return null;

        const { civicPoints } = user;

        const rank =
            civicPoints >= 700 ? "civic_champion"    :
            civicPoints >= 350 ? "district_guardian" :
            civicPoints >= 150 ? "community_hero"    :
            civicPoints >= 50  ? "neighborhood_watch":
                                 "citizen";

        await User.findByIdAndUpdate(userId, { rank });
        return rank;
    } catch (error) {
        console.error("Error updating rank:", error);
        return null;
    }
};
