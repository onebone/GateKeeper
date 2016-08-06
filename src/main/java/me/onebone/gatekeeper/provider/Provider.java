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

import java.util.Map;

import cn.nukkit.Player;

public interface Provider{
	public void addPlayer(Player player, String hash);
	public boolean playerExists(String player);
	public void setPlayer(String player, String hash);
	public void setPlayer(Player player, String hash);
	public void setPlayer(Player player);
	public Map<String, Object> getPlayer(Player player);
	public void removePlayer(Player player);
	public void save();
	public void close();
}
