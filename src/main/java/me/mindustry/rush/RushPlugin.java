package me.mindustry.rush;

import arc.Events;
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
import mindustry.net.Administration;
import mindustry.world.Block;


@SuppressWarnings("unused")
public class RushPlugin extends Plugin {

    private static final float GAME_DURATION = 20.0F;
    private static final int UPDATE_TIMER = 0;
    private static final int COUNTDOWN_TIMER = 1;

    private final Interval timers = new Interval(2);

    @Override
    public void init() {
        // Makes the player unable to destroy sources blocks
        Vars.netServer.admins.addActionFilter(action -> {
            return !isActive() || action.type != Administration.ActionType.breakBlock || !isSourceBlock(action.block);
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            if (isActive()) {
                Call.infoMessage(e.player.con(), ""
                        + "[gold]Welcome to RUSH, [green]your goal as a team is to kill other teams."
                        + "If the timer on the bottom runs out, you all Lose!"
                );
            }
        });

        Events.on(EventType.PlayEvent.class, e -> {
            if (isActive()) {
                timers.reset(COUNTDOWN_TIMER, 0);
                Vars.state.rules = createRushRules();
                Call.setRules(Vars.state.rules);
                Vars.world.tiles.forEach(tile -> {
                    // No way a player can destroy this in regular circumstances...
                    if (isSourceBlock(tile.block())) tile.build.health = Float.MAX_VALUE;
                });
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            if (isActive() && timers.get(UPDATE_TIMER, Time.toSeconds)) {
                Call.infoPopup("[cyan]Timer: " + Strings.formatMillis(getRemainingTime()), 1, 4, 0, 0, 0, 0);
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

    private boolean isActive() {
        return Vars.state.rules.pvp;
    }

    private boolean isSourceBlock(final Block block) {
        return block == Blocks.itemSource || block == Blocks.powerSource || block == Blocks.liquidSource;
    }

    private Rules createRushRules() {
        final var rules = new Rules();
        Gamemode.pvp.apply(rules);
        rules.bannedBlocks.add(Blocks.foreshadow);
        rules.logicUnitBuild = false;
        return rules;
    }
}
