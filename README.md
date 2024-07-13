EternalHarvest is a versatile Bukkit/Spigot Minecraft plugin designed to simulate the growth of plants (crops, saplings) and manage their growth even when the server is offline or the chunk is unloaded. 
It achieves this by persisting plant growth data in an SQLite database (plant_growth.db). The plugin ensures that plants continue to grow based on real-time elapsed since their last update, mimicking natural growth cycles, for server administrators and players who wish to enhance their agricultural and environmental gameplay. 
This plugin introduces automated plant growth tracking and management, enabling dynamic growth of plants such as wheat, carrots, potatoes, and saplings into trees. It integrates seamlessly with the server environment and provides robust features to manage plant growth cycles and environmental interactions.

Key Features:

Automatic Growth Tracking: EternalHarvest tracks the growth of planted crops and saplings in real-time using a database backend. This ensures that plants grow naturally and can be managed efficiently.

Database Integration: Utilizing SQLite, EternalHarvest stores and manages plant data, including growth progress, timestamps, and locations, ensuring persistence across server restarts.

Dynamic Tree Growth: Saplings planted with EternalHarvest have a chance to grow into trees based on configured growth times. Upon maturity, trees are generated in-game, enhancing the natural environment.

Bee Hive Integration: With configurable settings, trees grown from saplings can spawn bee hives, adding a touch of realism and interaction with Minecraft's ecosystem.

Configurable Settings: Server administrators can customize growth intervals, bee hive chances, and even enable debug mode for detailed operational insights.

Event-driven Architecture: Leveraging Bukkit's event system, EternalHarvest reacts to player actions like planting or breaking blocks, ensuring seamless integration with existing gameplay mechanics.

Usage Scenario:

Players can plant various crops knowing they will grow autonomously over time, enhancing the farming experience.
Server administrators can fine-tune growth parameters and observe growth statistics through the plugin's debug mode, ensuring optimal server performance.

Technical Details:

Java Plugin: Developed in Java using the Bukkit API, ensuring compatibility and performance with Minecraft servers.
Asynchronous Tasks: Utilizes Bukkit's scheduler for non-blocking tasks like database operations, ensuring minimal impact on server performance.
Compatibility: Compatible with Spigot and Bukkit Minecraft server platforms.
