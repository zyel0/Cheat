# Cheat - Elytra Anticheat Plugin

A comprehensive Minecraft Spigot/Bukkit plugin that provides elytra login checks and anticheat functionality to prevent abuse and detect irregular movement patterns.

## Features

### 1. Elytra Login Check
- Detects when players log in with an elytra equipped
- Configurable notifications to admins
- Console logging support

### 2. Elytra Anticheat System
The plugin includes multiple checks to prevent elytra abuse:

#### Speed Check
- Monitors overall flight speed
- Tracks horizontal speed separately
- Configurable thresholds

#### Vertical Movement Check
- Detects impossible upward movements
- Prevents vertical speed hacks

#### Flight Duration Check
- Monitors how long players fly continuously
- Configurable maximum duration
- Warning system for approaching limits

#### Acceleration Check
- Detects sudden changes in velocity
- Prevents acceleration hacks

## Installation

1. Download the latest release JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/Cheat/config.yml`

## Building from Source

Requirements:
- Java 8 or higher
- Maven

```bash
mvn clean package
```

The compiled JAR will be in the `target` folder.

## Commands

- `/elytraac reload` - Reload the configuration
- `/elytraac status` - Check plugin status and enabled checks
- `/elytraac violations` - View all active violations
- `/elytraac reset <player>` - Reset violations for a specific player

Aliases: `/eac`

## Permissions

- `elytraac.admin` - Access to all commands (default: op)
- `elytraac.bypass` - Bypass all anticheat checks (default: false)
- `elytraac.notify` - Receive violation notifications (default: false)

## Configuration

The plugin is highly configurable through `config.yml`:

- Enable/disable individual checks
- Adjust speed thresholds
- Configure violation thresholds
- Customize actions (kick, warn, commands)
- Set notification preferences

See the default `config.yml` for detailed options.

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please visit the GitHub repository.
