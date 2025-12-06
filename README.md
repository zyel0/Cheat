# Cheat - ML-Based Movement Anticheat Plugin

An advanced Minecraft Spigot/Bukkit plugin (1.21.1+) that uses machine learning to detect abnormal player movement patterns. The system learns what normal movement looks like during gameplay and flags suspicious behavior.

## Features

### 1. ML-Based Anomaly Detection
- **Self-Learning System**: Automatically learns normal movement patterns during a training period (~7 days)
- **Real-Time Analysis**: Continuously monitors all player movement during gameplay and PVP
- **Comprehensive Tracking**: Analyzes speed, acceleration, vertical movement, player state (gliding, flying, sprinting, etc.)
- **Smart Flagging**: Only flags players showing multiple anomalies, ignoring single unusual movements
- **Zero Configuration**: Works out of the box - just install and let it learn

### 2. Advanced Movement Tracking
The ML system tracks 16+ features for each movement:
- 3D speed and horizontal/vertical components
- Acceleration patterns
- Pitch and yaw changes
- Player state (gliding, flying, sneaking, sprinting, on ground, in water/lava)
- Movement timing and consistency

### 3. Flag System
- **Detailed Flags**: Each flag contains up to 100 words of detailed information including position, direction, speed, acceleration, and player state
- **Clickable Flags**: Click any flag to copy full details to clipboard
- **Toggle Notifications**: Use `/ml flags` to enable/disable real-time flag notifications
- **Admin Review**: View all flags with `/ml flags` command

## How It Works

### Training Period (Week 1)
During the first ~7 days, the plugin:
1. Collects movement data from all players (default: 20+ average)
2. Tracks every movement packet with position, velocity, player state
3. Builds statistical models for different movement contexts (gliding, sprinting, normal, etc.)
4. Learns what "normal" looks like for your server's gameplay style

### Detection Phase (After Training)
Once trained, the system:
1. Compares each movement against learned patterns
2. Calculates an anomaly score based on statistical deviation
3. Ignores single anomalies (could be lag or legitimate unusual movement)
4. Flags players only when showing 2+ anomalies in a short timeframe
5. Provides detailed information for each flag for admin review

## Installation

1. Download the latest release JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. The plugin will automatically start its training period
5. After 7 days, it will begin detecting anomalies

**Note**: The system needs at least 1000 movement samples to train. With 20+ players, this typically happens within the first few hours.

## Building from Source

Requirements:
- Java 8 or higher
- Maven

```bash
mvn clean package
```

The compiled JAR will be in the `target` folder.

## Commands

### ML System Commands
- `/ml flags` - Toggle flag notifications and view current flags
- `/ml status` - View ML system status, training progress, and statistics
- `/ml clear <player|all>` - Clear flags for a player or all players
- `/ml save` - Manually save the ML model to disk

Aliases: `/mlac`

### Legacy Commands (for backward compatibility)
- `/elytraac reload` - Reload the configuration
- `/elytraac status` - Check plugin status

Aliases: `/eac`

## Permissions

- `cheat.ml.admin` - Access to ML commands (default: op)
- `cheat.ml.bypass` - Bypass ML movement tracking (default: false)
- `elytraac.admin` - Access to legacy commands (default: op)
- `elytraac.bypass` - Bypass legacy checks (default: false)

## Configuration

The plugin is configurable through `config.yml`:

### ML Settings
- `training-period-days` - How long to train (default: 7 days)
- `min-training-samples` - Minimum samples needed (default: 1000)
- `anomaly-threshold` - Sensitivity (default: 2.5 std deviations)
- `flag-threshold` - Anomalies needed to flag (default: 2)
- `auto-save-interval` - How often to save model (default: 5 minutes)
- `debug` - Enable detailed logging (default: false)

### Data Storage
- ML models are saved in `plugins/Cheat/ml-data/model.json`
- Automatically saves every 5 minutes and on shutdown
- Persists across server restarts

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please visit the GitHub repository.
