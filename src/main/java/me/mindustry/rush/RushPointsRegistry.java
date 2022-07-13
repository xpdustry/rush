package me.mindustry.rush;

import arc.Events;
import me.mindustry.leaderboard.LeaderboardPlugin;
import me.mindustry.leaderboard.model.LeaderboardPoints;
import me.mindustry.leaderboard.repository.PointsRegistry;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.world.blocks.storage.CoreBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RushPointsRegistry implements PointsRegistry {

    private static final LeaderboardPoints
            RUSH_VICTORY_POINTS = LeaderboardPoints.of("Victory", "Win a game of Rush", +100),
            RUSH_DEFEAT_POINTS = LeaderboardPoints.of("Defeat", "Loose a game of Rush", -100);

    /** Contains the players which already have been points granted during the current game. */
    private final Set<String> grants = new HashSet<>();

    RushPointsRegistry() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            // Checks if the last core of a team got destroyed...
            if (RushPlugin.isActive() && e.tile.block() instanceof CoreBlock && e.tile.team().cores().size == 1) {
                Groups.player.each(
                        p -> p.team() == e.tile.team() && !grants.contains(p.uuid()),
                        p -> {
                            grants.add(p.uuid());
                            LeaderboardPlugin.getLeaderboardService().grantPoints(p, RUSH_DEFEAT_POINTS);
                        }
                );
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            if (RushPlugin.isActive()) {
                Groups.player.each(
                        p -> p.team() == e.winner && !grants.contains(p.uuid()),
                        p -> {
                            grants.add(p.uuid());
                            LeaderboardPlugin.getLeaderboardService().grantPoints(p, RUSH_VICTORY_POINTS);
                        }
                );
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> grants.clear());
    }

    @Override
    public @NotNull Collection<LeaderboardPoints> getLeaderboardPoints() {
        return List.of(RUSH_VICTORY_POINTS, RUSH_DEFEAT_POINTS);
    }
}
