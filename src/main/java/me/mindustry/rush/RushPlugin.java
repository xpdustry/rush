package me.mindustry.rush;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Interval;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.mod.Plugin;
import mindustry.world.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class RushPlugin extends Plugin {

    private static final float GAME_DURATION = 20.0F;
    private static final int UPDATE_TIMER = 0;
    private static final int COUNTDOWN_TIMER = 1;

    private final RushPointsRegistry registry = new RushPointsRegistry();
    private final Interval timers = new Interval(2);

    public static boolean isActive() {
        return Vars.state.rules.pvp;
    }

    @Override
    public void init() {
        // Register the points
        registry.register();

        // Makes the player unable to destroy sources blocks
        Vars.netServer.admins.addActionFilter(action -> {
            return !isActive() || switch (action.type) {
                case placeBlock -> action.tile
                        .getLinkedTilesAs(action.block, new Seq<>())
                        .find(t -> isSourceBlock(t.block())) == null;
                case breakBlock -> !isSourceBlock(action.block);
                default -> true;
            };
        });

        // Source block dupe
        Events.on(EventType.PickupEvent.class, e -> {
            if (isActive() && e.build != null && isSourceBlock(e.build.block())) {
                Core.app.post(() -> Call.setTile(e.build.tile(), e.build.block(), e.build.team(), e.build.rotation()));
            }
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            if (isActive()) {
                Call.infoMessage(e.player.con(), """
                        [gold]Welcome to RUSH, [green]your goal as a team is to kill other teams.
                        If the timer on the bottom runs out, you all Lose!
                        """
                );
            }
        });

        Events.on(EventType.PlayEvent.class, e -> {
            if (isActive()) {
                timers.reset(COUNTDOWN_TIMER, 0);
                applyRushRules(Vars.state.rules);
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            if (isActive() && timers.get(UPDATE_TIMER, Time.toSeconds)) {
                Call.infoPopup("[cyan]Timer: " + Strings.formatMillis(getRemainingTime()), 1, Align.bottom | Align.center, 0, 0, 0, 0);
            }

            if (isActive() && timers.get(COUNTDOWN_TIMER, GAME_DURATION * Time.toMinutes)) {
                Events.fire(new EventType.GameOverEvent(Team.derelict));
                Call.infoMessage("Too bad, [red]everyone lost.[]");
            }
        });
    }

    private long getRemainingTime() {
        return Math.max((long) (((GAME_DURATION * Time.toMinutes) - timers.getTime(COUNTDOWN_TIMER)) / Time.toSeconds * 1000L), 0L);
    }

    private boolean isSourceBlock(final @Nullable Block block) {
        return block == Blocks.itemSource || block == Blocks.powerSource || block == Blocks.liquidSource;
    }

    private void applyRushRules(final @NotNull Rules rules) {
        rules.bannedBlocks.add(Blocks.foreshadow);
        rules.logicUnitBuild = false;
        rules.damageExplosions = false;
    }
}
