package me.onebone.gatekeeper.provider;

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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import me.onebone.gatekeeper.GateKeeper;
import cn.nukkit.Player;
import cn.nukkit.utils.Config;

public class YamlProvider implements Provider{
	private File baseFolder;
	
	public YamlProvider(GateKeeper plugin){
		this.baseFolder = new File(plugin.getDataFolder(), "players");
		if(!baseFolder.exists()){
			baseFolder.mkdir();
		}
	}
	
	@SuppressWarnings("serial")
	@Override
	public void addPlayer(Player player, String hash){
		new Config(new File(this.baseFolder, player.getName().toLowerCase() + ".yml"), Config.YAML, new LinkedHashMap<String, Object>(){
			{
				put("registered", System.currentTimeMillis());
				put("lastJoined", System.currentTimeMillis());
				put("lastIP", player.getAddress());
				put("hash", hash);
			}
		});
	}
	
	@Override
	public void setPlayer(Player player, String hash){
		// TODO
	}
	
	@Override
	public Map<String, Object> getPlayer(Player player){
		File file = new File(this.baseFolder, player.getName().toLowerCase() + ".yml");
		if(file.isFile()){
			return new Config(file, Config.YAML).getAll();
		}
		return null;
	}

	@Override
	public void removePlayer(Player player){
		
	}

	@Override
	public void save(){
	}

	@Override
	public void close(){
	}

	@Override
	public boolean playerExists(Player player){
		File file = new File(this.baseFolder, player.getName().toLowerCase() + ".yml");
		
		return file.isFile();
	}
}
