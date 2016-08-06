package me.onebone.gatekeeper.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import me.onebone.gatekeeper.PermissionComparator;
import me.onebone.gatekeeper.SendTipTask;
import me.onebone.gatekeeper.provider.Provider;
import me.onebone.gatekeeper.provider.YamlProvider;
import cn.nukkit.Player;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.plugin.PluginBase;

public class DefaultManager extends Manager{
	private PluginBase plugin;
	private Provider provider;
	private Map<Player, PermissionAttachment> attachments;
	
	// TODO
	//private SendTipTask task;
	
	public DefaultManager(PluginBase plugin){
		this.plugin = plugin;
		
		attachments = new HashMap<>();
		this.provider = new YamlProvider(plugin);
		
		//this.plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(task = new SendTipTask(plugin), 10, 10);
	}

	@Override
	public boolean registerPlayer(Player player, String password) {
		if(!this.isRegistered(player.getName())){
			this.provider.addPlayer(player, hash(player, password));
			return true;
		}
		return false;
	}

	@Override
	public boolean isAuthenticated(Player player) {
		return !this.attachments.containsKey(player);
	}

	@Override
	public boolean isRegistered(String player) {
		return this.provider.playerExists(player);
	}

	@Override
	public boolean tryAuthenticate(Player player) {
		Map<String, Object> map = this.provider.getPlayer(player);
		if(map.getOrDefault("lastIP", "127.0.0.1").equals(player.getAddress())
			&& player.getUniqueId().toString().equals(map.get("uuid"))){
			this.setAuthenticated(player);
			return true;
		}
		
		return false;
	}

	@Override
	public boolean authenticate(Player player, String password) {
		if(!this.isAuthenticated(player)){
			Map<String, Object> map = this.provider.getPlayer(player);
			if(map.getOrDefault("hash", "").equals(this.hash(player, password))){
				this.setAuthenticated(player);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean setAuthenticated(Player player){
		player.removeAttachment(this.attachments.get(player));
		this.attachments.remove(player);
		
		this.updatePlayer(player);
		return false;
	}

	@Override
	public boolean deauthenticate(Player player) {
		this.removePermissions(player);
		return true;
	}

	@Override
	public void updatePlayer(Player player) {
		this.provider.setPlayer(player);
	}

	@Override
	public boolean changePassword(String player, String password) {
		if(this.isRegistered(player)){
			this.provider.setPlayer(player, this.hash(player.toLowerCase(), password));
			return true;
		}
		return false;
	}
	
	public void removePermissions(Player player){
		PermissionAttachment attachment = player.addAttachment(this.plugin);
		
		TreeMap<String, Boolean> permissions = new TreeMap<String, Boolean>(new PermissionComparator());
		this.plugin.getServer().getPluginManager().getPermissions().forEach((k, v) -> {
			permissions.put(k, false);
		});
		permissions.put("nukkit.command.help", true);
		
		permissions.put("gatekeeper.command.login", true);
		permissions.put("gatekeeper.command.register", true);
		
		attachment.setPermissions(permissions);
		
		this.attachments.put(player, attachment);
	}
}
