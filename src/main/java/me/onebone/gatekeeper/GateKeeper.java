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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.onebone.gatekeeper.provider.Provider;
import me.onebone.gatekeeper.provider.YamlProvider;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.TranslationContainer;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.CraftItemEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryPickupArrowEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.player.PlayerDropItemEvent;
import cn.nukkit.event.player.PlayerFoodLevelChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemConsumeEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerPreLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.event.player.PlayerToggleSprintEvent;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class GateKeeper extends PluginBase implements Listener{
	private HashMap<Player, PermissionAttachment> attachments;
	
	private Provider provider;
	private SendTipTask task;
	
	private Map<String, String> lang;
	
	public String getMessage(String key, Object... params){
		if(this.lang.containsKey(key)){
			return replaceMessage(this.lang.get(key), params);
		}
		return "Could not find message with " + key;
	}
	
	private String replaceMessage(String lang, Object[] params){
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < lang.length(); i++){
			char c = lang.charAt(i);
			if(c == '{'){
				int index;
				if((index = lang.indexOf('}', i)) != -1){
					try{
						String p = lang.substring(i + 1, index);
						int param = Integer.parseInt(p);
						
						if(params.length > param){
							i = index;
							
							builder.append(params[param]);
							continue;
						}
					}catch(NumberFormatException e){}
				}
			}
			
			builder.append(c);
		}
		
		return TextFormat.colorize(builder.toString());
	}
	
	public void onEnable(){
		this.saveDefaultConfig();
		
		String name = this.getConfig().get("data.language", "eng");
		InputStream is = this.getResource("lang_" + name + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_eng.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		if(!name.equals("eng")){
			try{
				LinkedHashMap<String, String> temp = new GsonBuilder().create().fromJson(Utils.readFile(this.getResource("lang_eng.json")), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
				temp.forEach((k, v) -> {
					if(!lang.containsKey(k)){
						lang.put(k, v);
					}
				});
			}catch(IOException e){
				this.getLogger().critical(e.getMessage());
			}
		}
		
		attachments = new HashMap<>();
		
		this.provider = new YamlProvider(this);
		
		this.getServer().getScheduler().scheduleDelayedRepeatingTask(task = new SendTipTask(this), 10, 10);	
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void removePermisions(Player player){
		PermissionAttachment attachment = player.addAttachment(this);
		
		TreeMap<String, Boolean> permissions = new TreeMap<String, Boolean>(new PermissionComparator());
		this.getServer().getPluginManager().getPermissions().forEach((k, v) -> {
			permissions.put(k, false);
		});
		permissions.put("nukkit.command.help", true);
		
		permissions.put("gatekeeper.command.login", true);
		permissions.put("gatekeeper.command.register", true);
		
		attachment.setPermissions(permissions);
		
		this.attachments.put(player, attachment);
	}
	
	public void setLoggedOut(Player player){
		this.removePermisions(player);
		
		task.addPlayer(player);
	}
	
	public void setLoggedIn(Player player){
		player.removeAttachment(this.attachments.get(player));
		
		this.provider.setPlayer(player);
		
		this.attachments.remove(player);
		task.removePlayer(player);
	}
	
	public boolean isLoggedIn(Player player){
		return !attachments.containsKey(player);
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("login")){
			if(!(sender instanceof Player)){
				sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.ingame"));
				return true;
			}

			if(args.length < 1){
				sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.usage", command.getUsage()));
				return true;
			}
			Player player = (Player) sender;
			
			if(this.isLoggedIn(player)){
				sender.sendMessage(this.getMessage("already-logged-in"));
				return true;
			}
			Map<String, Object> data;
			if((data = this.provider.getPlayer(player)) == null){
				sender.sendMessage(this.getMessage("register-to-server"));
				return true;
			}
			
			if(data.get("hash").equals(hash(player, args[0]))){
				this.setLoggedIn(player);
				sender.sendMessage(this.getMessage("logged-in"));
			}else{
				sender.sendMessage(this.getMessage("incorrect-password"));
			}
			return true;
		}else if(command.getName().equals("register")){
			if(!(sender instanceof Player)){
				sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.ingame"));
				return true;
			}
			
			if(args.length < 2){
				sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.usage", command.getUsage()));
				return true;
			}
			Player player = (Player) sender;
			
			if(this.provider.playerExists(player)){
				sender.sendMessage(this.getMessage("already-registered"));
				return true;
			}
			
			if(!args[0].equals(args[1])){
				sender.sendMessage(this.getMessage("confirm-password"));
				return true;
			}
			
			String password = args[0];
			if(password.length() < this.getConfig().get("password.min-length", 4)){
				sender.sendMessage(this.getMessage("password-too-short"));
				return true;
			}
			if(password.length() > this.getConfig().get("password.max-length", 20)){
				sender.sendMessage(this.getMessage("password-too-long"));
				return true;
			}
			
			this.provider.addPlayer(player, hash(player, password));
			
			sender.sendMessage(this.getMessage("register-complete"));
			this.setLoggedIn(player);
			return true;
		}else if(command.getName().equals("logout")){
			if(!(sender instanceof Player)){
				sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.ingame"));
				return true;
			}
			
			Player player = (Player) sender;
			if(!this.isLoggedIn(player)){
				player.sendMessage(this.getMessage("please-login"));
				return true;
			}
			
			this.setLoggedOut(player);
			player.sendMessage(this.getMessage("logged-out"));
			return true;
		}
		return false;
	}
	
	public String hash(Player player, String password){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update((player.getName().toLowerCase() + password).getBytes());
			byte[] result = md.digest();
			
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < result.length; i++){
				String str;
				if(!this.getConfig().getBoolean("data.old", false)){
					str = Integer.toHexString(new Byte(result[i]) & 0xff);
				}else{
					str = Integer.toHexString(new Byte(result[i]));
				}
				if(str.length() < 2){
					str = "0" + str;
				}
				builder.append(str);
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean tryLogin(Player player){
		Map<String, Object> map = this.provider.getPlayer(player);
		if(map.getOrDefault("lastIP", "127.0.0.1").equals(player.getAddress())
			&& player.getUniqueId().toString().equals(map.get("uuid"))){
			this.setLoggedIn(player);
			
			return true;
		}
		
		return false;
	}
	
	@EventHandler
	public void onPreLogin(PlayerPreLoginEvent event){
		if(!this.getConfig().get("session.single", true)) return; 
		
		Player player = event.getPlayer();
		
		for(Player online : this.getServer().getOnlinePlayers().values()){
			if(online != player && online.getName().toLowerCase().equals(player.getName().toLowerCase()) && this.isLoggedIn(online)){
				event.setCancelled();
				player.kick("already logged in");
				return;
			}
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		
		this.setLoggedOut(player);
		
		if(this.provider.playerExists(player)){
			if(this.getConfig().getBoolean("auto.login", true) && this.tryLogin(player)){
				player.sendMessage(this.getMessage("logged-in"));
			}else{
				player.sendMessage(this.getMessage("login-to-server"));
			}
		}else{
			player.sendMessage(this.getMessage("register-to-server"));
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		this.attachments.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onChat(PlayerChatEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
			event.getPlayer().sendMessage(this.getMessage("please-login"));
		}
	}
	
	@SuppressWarnings("serial")
	@EventHandler
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event){
		String message = event.getMessage();
		if(!this.isLoggedIn(event.getPlayer()) && !this.getConfig().getList("allow.commands", new ArrayList<String>(){
			{add("login"); add("register"); add("help");}
		}).contains(message.split(" ")[0].substring(1))){
			event.setCancelled();
			event.getPlayer().sendMessage(this.getMessage("please-login"));
		}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onFoodLevelChange(PlayerFoodLevelChangeEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onItemConsume(PlayerItemConsumeEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onItemHold(PlayerItemHeldEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			if(!this.getConfig().get("movement.allow-move", false)){
				event.setCancelled();
			}
		}
	}
	
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event){
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			if(!this.isLoggedIn(player)){
				event.setCancelled();
			}
		}
	}
	
	@EventHandler
	public void onPickup(InventoryPickupItemEvent event){
		InventoryHolder holder = event.getInventory().getHolder();
		if(holder instanceof Player){
			Player player = (Player)holder;
			if(!this.isLoggedIn(player)){
				event.setCancelled();
			}
		}
	}
	
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onPickupArrow(InventoryPickupArrowEvent event){
		InventoryHolder holder = event.getInventory().getHolder();
		if(holder instanceof Player){
			Player player = (Player)holder;
			if(!this.isLoggedIn(player)){
				event.setCancelled();
			}
		}
	}
	
	@EventHandler
	public void onInventoryOpen(CraftItemEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event){
		if(!this.isLoggedIn(event.getPlayer())){
			event.setCancelled();
			event.getPlayer().sendMessage(this.getMessage("please-login"));
		}
	}
}
