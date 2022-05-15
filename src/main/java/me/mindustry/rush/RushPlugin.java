package me.mindustry.rush;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.mod.Plugin;
import mindustry.net.Administration;


@SuppressWarnings("unused")
public class RushPlugin extends Plugin {

    @Override
    public void init() {
        Vars.netServer.admins.addActionFilter(action -> {
            if (action.type == Administration.ActionType.breakBlock) {
                return action.block != Blocks.itemSource
                        && action.block != Blocks.liquidSource
                        && action.block != Blocks.powerSource;
            }
            return true;
        });
    }
}
