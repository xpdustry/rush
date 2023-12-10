/*
 * This file is part of RushPlugin. A simple plugin for the Rush gamemode.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xpdustry.rush;

import arc.Events;
import com.xpdustry.leaderboard.LeaderboardPlugin;
import com.xpdustry.leaderboard.LeaderboardPoints;
import com.xpdustry.leaderboard.PointsRegistry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.world.blocks.storage.CoreBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

final class RushPointsRegistry implements PointsRegistry {

    private static final LeaderboardPoints
            RUSH_VICTORY_POINTS = LeaderboardPoints.of("Victory", "Win a game of Rush", +100),
            RUSH_DEFEAT_POINTS = LeaderboardPoints.of("Defeat", "Loose a game of Rush", -100);

    /** Contains the players which already have been points granted during the current game. */
    private final Set<String> grants = new HashSet<>();

    RushPointsRegistry() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            // Checks if the last core of a team got destroyed...
            if (RushPlugin.isActive()
                    && e.tile.block() instanceof CoreBlock
                    && e.tile.team().cores().size == 1) {
                Groups.player.each(p -> p.team() == e.tile.team() && !grants.contains(p.uuid()), p -> {
                    grants.add(p.uuid());
                    LeaderboardPlugin.getLeaderboardService().grantPoints(p, RUSH_DEFEAT_POINTS);
                });
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            if (RushPlugin.isActive()) {
                Groups.player.each(p -> p.team() == e.winner && !grants.contains(p.uuid()), p -> {
                    grants.add(p.uuid());
                    LeaderboardPlugin.getLeaderboardService().grantPoints(p, RUSH_VICTORY_POINTS);
                });
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> grants.clear());
    }

    @Override
    public @NonNull Collection<LeaderboardPoints> getLeaderboardPoints() {
        return List.of(RUSH_VICTORY_POINTS, RUSH_DEFEAT_POINTS);
    }
}
