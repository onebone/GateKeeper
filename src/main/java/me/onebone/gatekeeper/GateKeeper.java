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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import me.onebone.gatekeeper.provider.Provider;
import me.onebone.gatekeeper.provider.YamlProvider;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
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
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.event.player.PlayerToggleSprintEvent;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.utils.TextFormat;

public class GateKeeper extends PluginBase implements Listener{
	private HashMap<Player, PermissionAttachment> attachments;
	
	private Provider provider;
	private SendTipTask task;
	
	public void onEnable(){
		this.saveDefaultConfig();
		
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
				sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
				return true;
			}

			if(args.length < 1){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			Player player = (Player) sender;
			
			if(this.isLoggedIn(player)){
				sender.sendMessage(TextFormat.RED + "You are already logged in.");
				return true;
			}
			Map<String, Object> data;
			if((data = this.provider.getPlayer(player)) == null){
				sender.sendMessage(TextFormat.RED + "Please register to the server using /register <password> <password>");
				return true;
			}
			
			if(data.get("hash").equals(hash(player, args[0]))){
				this.setLoggedIn(player);
				sender.sendMessage(TextFormat.GREEN + "You have logged in successfully.");
			}else{
				sender.sendMessage(TextFormat.RED + "Incorrect password was given.");
			}
			return true;
		}else if(command.getName().equals("register")){
			if(!(sender instanceof Player)){
				sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
				return true;
			}
			
			if(args.length < 2){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			Player player = (Player) sender;
			
			if(this.provider.playerExists(player)){
				sender.sendMessage(TextFormat.RED + "You have already registered.");
				return true;
			}
			
			if(!args[0].equals(args[1])){
				sender.sendMessage(TextFormat.RED + "Please confirm your password correctly.");
				return true;
			}
			
			String password = args[0];
			if(password.length() < this.getConfig().get("password.min-length", 4)){
				sender.sendMessage(TextFormat.RED + "Your password is too short.");
				return true;
			}
			if(password.length() > this.getConfig().get("password.max-length", 20)){
				sender.sendMessage(TextFormat.RED + "You password is too long.");
				return true;
			}
			
			this.provider.addPlayer(player, hash(player, password));
			
			sender.sendMessage(TextFormat.GREEN + "You have successfully registered to the server.");
			this.setLoggedIn(player);
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
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		
		this.setLoggedOut(player);
		
		if(this.provider.playerExists(player)){
			if(this.getConfig().getBoolean("auto.login", true) && this.tryLogin(player)){
				player.sendMessage(TextFormat.GREEN + "You have logged in successfully.");
			}else{
				player.sendMessage(TextFormat.YELLOW + "Please login to server using /login <password>");
			}
		}else{
			player.sendMessage(TextFormat.YELLOW + "Please register to server using /register <password> <password>");
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
			event.getPlayer().sendMessage(TextFormat.RED + "Please login first.");
		}
	}
	
	@EventHandler
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event){
		String message = event.getMessage();
		if(!this.isLoggedIn(event.getPlayer()) && !message.startsWith("/login") && !message.startsWith("/register") && !message.startsWith(("/help"))){
			event.setCancelled();
			event.getPlayer().sendMessage(TextFormat.RED + "Please login first.");
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
			event.getPlayer().sendMessage(TextFormat.RED + "Please login first.");
		}
	}
}
