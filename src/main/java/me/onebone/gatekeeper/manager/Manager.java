package me.onebone.gatekeeper.manager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;

public abstract class Manager implements Listener{
	/**
	 * Registers player
	 * @param player
	 * @param password
	 * @return `true` if player is succeed, `false` if not
	 */
	public abstract boolean registerPlayer(Player player, String password);
	
	/**
	 * Returns if player is authenticated
	 * 
	 * @param player
	 * @return `true` if player is authenticated, `false` if not
	 */
	public abstract boolean isAuthenticated(Player player);
	
	/**
	 * Returns if player is registered
	 * @param string
	 * @return `true` if registered, `false` if not
	 */
	public abstract boolean isRegistered(String string);
	
	/**
	 * Tries to authenticate player when player joins server. This could be used to implement auto authentification.
	 * @param player
	 * @return `true` if succeed, `false` if not
	 */
	public abstract boolean tryAuthenticate(Player player);
	
	/**
	 * Authenticates player with password
	 * @param player
	 * @param password
	 * @return `true` if succeed, `false` if not
	 */
	public abstract boolean authenticate(Player player, String password);
	
	/**
	 * Authenticates player
	 * @param player
	 * @return `true` if authentication is not required, `false` if required
	 */
	public abstract boolean setAuthenticated(Player player);
	
	/**
	 * Deauthenticates player
	 * @param player
	 * @param password
	 * @return `true` if succeed, `false` if not
	 */
	public abstract boolean deauthenticate(Player player);
	
	/**
	 * Updates player information
	 * @param player
	 */
	public abstract void updatePlayer(Player player);
	
	/**
	 * Changes password of player
	 * @param player
	 * @param password
	 * @return `true` if succeed, `false` if not
	 */
	public abstract boolean changePassword(String player, String password);
	
	public boolean changePassword(Player player, String password){
		return this.changePassword(player.getName(), password);
	}
	
	public String hash(Player player, String password){
		return hash(player.getName(), password);
	}
	
	/**
	 * Encrypts password
	 * @param player
	 * @param password
	 * @return encrypted password
	 */
	public String hash(String player, String password){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update((player.toLowerCase() + password).getBytes());
			byte[] result = md.digest();
			
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < result.length; i++){
				String str = Integer.toHexString(new Byte(result[i]) & 0xff);
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
}
