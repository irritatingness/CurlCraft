package com.irritatingness;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static spark.Spark.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

public class CurlCraft extends JavaPlugin {
	
	// TODO: Refactor and break conditional logic into methods, reduce cognitive complexity.
	// Maybe split actions out into their own classes, figure out a way to reflect against a package?
	// Otherwise, drop the reflection approach and register new actions via some manager.
	
	private static boolean customCommands = false;
	private static int port = 4567;
	private static String password = "test";
	private static Plugin plugin;

	public void onEnable() {
		
		// Save reference to this plugin
		plugin = this;
		
		// Save the default config if it doesn't already exist
		this.saveDefaultConfig();
		
		// Determine if custom commands should be allowed.
		customCommands = this.getConfig().getBoolean("customCommands");
		
		// Get the port for Spark
		port = this.getConfig().getInt("port");
		
		// Get the password for API calls
		password = this.getConfig().getString("password");
		
		// Define Spark port
		port(port);
		
		// Register get request for targets
		registerGetTargets();
		
		// Register post request endpoint
		registerPostAction();
		
	}
	
	@SuppressWarnings("unchecked")
	public void registerGetTargets() {
		// Register route for getting targets
		get("/curlcraft", (req, res) -> {
			JSONObject resp = new JSONObject();
			JSONArray players = new JSONArray();
			for (Player p : Bukkit.getOnlinePlayers()) {
				players.add(p.getName());
			}
			resp.put("players", players);
			return resp;
		});
	}
	
	public void registerPostAction() {
		// Register the API route for commands
		post("/curlcraft", (req, res) -> {
			if (req.body() != null && !req.body().isEmpty()) {
				JSONParser parser = new JSONParser();
				JSONObject body = (JSONObject) parser.parse(req.body());			
				// Verify password is present and correct
				if (verifyPassword(body)) {
					// First check if the customCommand key is present, if so, execute that command
					if (body.containsKey("customCommand")) {
						// Check if we're allowing custom commands before proceeding
						if (!customCommands) {
							res.status(401);
							return("Custom commands have been disabled in the CurlCraft config.");
						}
						String command = body.get("customCommand").toString();
						if (command != null) {
							// Run the command on the server thread
							Bukkit.getScheduler().runTask(this, new Runnable() {
								@Override
								public void run() {
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
								}
							});
							res.status(200);
							return("");
						}
					} else {
						// Check for the action and target keys
						if (body.containsKey("action")) {
							final String action = body.get("action").toString();
							// Check to see if there's a target involved
							if (body.containsKey("target")) {
								// Get target string
								final String target = body.get("target").toString();
								// Try to find player matching target
								Player p = this.getServer().getPlayerExact(target);
								if (p != null) {
									try {
										// Execute action with a target
										Method m = this.getClass().getMethod(action, String.class);
										
										// Runnable to execute actual method on with target
										Bukkit.getScheduler().runTask(this, new Runnable() {
											@Override
											public void run() {
												try {
													m.invoke(plugin, target);
												} catch (IllegalAccessException | IllegalArgumentException
														| InvocationTargetException e) {
													e.printStackTrace();
												}
											}
										});
										res.status(200);
										return("");
									} catch(NoSuchMethodException e) {
										res.status(400);
										return("Bad request, action was not an available action.");
									}
								} else {
									res.status(400);
									return("Target not online or not found.");
								}
							} else {
								try {
									// Execute action without a target
									Method m = this.getClass().getMethod(action);
									
									// Runnable to execute actual method on
									Bukkit.getScheduler().runTask(this, new Runnable() {
										@Override
										public void run() {
											try {
												m.invoke(plugin);
											} catch (IllegalAccessException | IllegalArgumentException
													| InvocationTargetException e) {
												e.printStackTrace();
											}
										}
									});
									res.status(200);
									return("");
								} catch(NoSuchMethodException e) {
									res.status(400);
									return("Bad request, action was not an available action.");
								}
							}
						} else {
							// Neither action nor customCommand was specified
							res.status(400);
							return("Bad request, both customCommand and action keys missing.");
						}
					}
				} else {
					// Password key not found in JSON, or incorrect
					res.status(401);
					return("JSON key 'password' is required to be present and correct!");
				}
				
			} else {
				// JSON body not found
				res.status(400);
				return("POST body required.");
			}
			
			// Default return (something went wrong)
			res.status(400);
			return("Hit default return... something is wrong.");
		});
	}
	
	// Function to verify password, returns true if password matches configuration
	public boolean verifyPassword(JSONObject body) {
		if (body.containsKey("password")) {
			if (body.get("password").toString().equals(password)) {
				return true;
			}
		}
		return false;
	}
	
	// Define actions below
	
	public void heal(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.setHealth(20.0);
		}
	}
	
	public void feed(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.setFoodLevel(20);
		}
	}
	
	public void burn(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.setFireTicks(200);
		}
	}
	
	public void smite(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.getWorld().strikeLightning(p.getLocation());
		}
	}
	
	public void explode(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.getWorld().createExplosion(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), 4.0F);
		}
	}
	
	public void explodeNoBlockDamage(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			p.getWorld().createExplosion(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), 4.0F, false, false);
		}
	}
	
	public void spawnCreeper(String target) {
		Player p = Bukkit.getServer().getPlayerExact(target);
		if (p != null) {
			Random r  = new Random();
			Location loc = p.getLocation();
			Double x = -10 + (10 + 10) * r.nextDouble();
			Double z = -10 + (10 + 10) * r.nextDouble(); 
			loc.add(x, 0, z);
			// Make sure the location is safe (no wasting creepers)
			loc.setY(p.getWorld().getHighestBlockYAt(loc));
			p.getWorld().spawnEntity(loc, EntityType.CREEPER);
		}
	}
}