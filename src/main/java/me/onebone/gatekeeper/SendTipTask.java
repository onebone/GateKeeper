package me.onebone.gatekeeper;

/*
 * GateKeeper: A typical user login system for Nukkit
 * Copyright (C) 2016 onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import cn.nukkit.Player;
import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.utils.TextFormat;

public class SendTipTask extends PluginTask<GateKeeper>{
	private List<Player> players;
	
	public SendTipTask(GateKeeper owner){
		super(owner);
		
		players = new ArrayList<>();
	}
	
	public void addPlayer(Player player){
		if(!this.players.contains(player)){
			this.players.add(player);
		}
	}
	
	public void removePlayer(Player player){
		this.players.remove(player);
	}

	@Override
	public void onRun(int arg0){
		this.players.forEach((player) -> player.sendTip(TextFormat.RED + "Please login first."));
	}
}
