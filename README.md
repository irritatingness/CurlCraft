# CurlCraft

CurlCraft is a Bukkit/Spigot/Paper plugin to enable RESTful calls to your Minecraft server to execute actions.

Actions range from simple pre-defined ones such as healing a player to passthrough commands to be executed as the console allowing for support of any executable command on your server whether it be a spigot built-in or another plugin command that supports console execution.

## Endpoints
Curl craft provides only two RESTful endpoints, one of which may be disabled.

**GET <your_server_ip>:<configurable_port>/curlcraft**

This endpoint simply returns a list of players (or as you'll see in the second endpoint, potential 'targets'). There is a configuration option to disable this endpoint as it does not require a password to be executed.

**POST <your_server_ip_>:<configurable_port>/curlcraft**

This endpoint serves as the sole entrypoint for executing actions via CurlCraft. The POST request must contain a body with a JSON including a `password` key as well as a `action` OR `customCommand` key. The body may also optionally include a `target` key to specify the target of an `action` that requires a target.

Example POST body:
`{
	"password": "test",
	"action": "heal",
	"target": "irritatingness"
}`

In the above example, the "heal" `action` will be executed on the `target` of irritatingness.

Alterntive example POST body:
`{ "password": "irritatingnessIsAwesome", "customCommand": "say irritatingness is awesome" }`

In this alternative example, the server `say` command will be executed with the string "irritatingness is awesome". Notice that there is no need for a forward slash ( / ) before the command to execute as the console, just as you might find from many default Minecraft server consoles.

## Configuration options
`port`: This is the port that the CurlCraft endpoints will bind to on your server. By default this is 4567, but can be changed to any available port you have.

`password`: This is pretty self explanatory, but is the password that is required in your POST request bodies. Sending the incorrect password will return a 401 Unauthorized and prevent anyone with the server IP from sending whatever they want.

`customCommands`: This option allows the disabling of executing custom commands from the CurlCraft POST endpoint. This may be useful if you want users to be able to execute pre-defined CurlCraft actions but not to execute ANY command (such as `op irritatingness`) on your server. In the future there may be an actions enabled or whitelist configuration option, but we'll see (if it's requested).
