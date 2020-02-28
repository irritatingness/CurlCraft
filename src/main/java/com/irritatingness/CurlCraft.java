package com.irritatingness;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static spark.Spark.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CurlCraft extends JavaPlugin {
	
	// TODO:
	// - Add logging
	// - Refactor and break conditional logic into methods, reduce cognitive complexity
	
	private static boolean customCommands = false;
	private static int port = 4567;
	private static String password = "test";

	public void onEnable() {
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
		
		// Register the API route for commands
		post("/curlcraft", (req, res) -> {
			if (req.body() != null && !req.body().isEmpty()) {
				JSONParser parser = new JSONParser();
				JSONObject body = (JSONObject) parser.parse(req.body());
				if (body.containsKey("password")) {
					// Verify password is correct
					if (body.get("password").toString().contentEquals(password)) {
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
														m.invoke(this, target);
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
													m.invoke(this);
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
						// Incorrect password
						res.status(401);
						return("Unauthorized.");
					}
				} else {
					// Password key not found in JSON
					res.status(401);
					return("JSON key 'password' required!");
				}
				
			} else {
				// JSON body not found
				res.status(400);
				return("POST body required.");
			}
			
			// Default return (something went wrong)
			// TODO: Check for every branch having a return?
			res.status(400);
			return("Hit default return... something is wrong.");
		});
	}
	
	public void onDisable() {
		
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
		
	}
}