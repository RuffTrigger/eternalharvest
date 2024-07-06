EternalHarvest is a Bukkit/Spigot plugin designed to simulate the growth of plants (crops, saplings) and manage their growth even when the server is offline or the chunk is unloaded. It achieves this by persisting plant growth data in an SQLite database (plant_growth.db). The plugin ensures that plants continue to grow based on real-time elapsed since their last update, mimicking natural growth cycles.

Key Features:

Persistent Plant Growth:

Plants (crops and saplings) continue to grow even when the server is offline or the chunk is unloaded.
Growth stages are stored in an SQLite database (plant_growth.db) located in the plugin's data folder.
Customizable Growth Times:

Different plant types have customizable growth times, defined within the plugin's code (PlantGrowthManager.java).
Automatic Database Handling:

On plugin enable (onEnable()), checks and creates plant_growth.db if it does not exist.
Establishes a connection to the SQLite database using JDBC (DriverManager.getConnection()).
Event Handling:

Registers event listeners (PlantEventListener and ChunkEventListener) to monitor plant growth events and chunk load/unload events.
Updates plant growth stages based on elapsed time since the chunk was last loaded.
Data Integrity:

Ensures that existing plant_growth.db files are not overwritten on plugin startup.
Clean Shutdown:

Saves all plant growth data (onDisable()) before shutting down the plugin.
Closes the database connection cleanly.
Implementation Details:

Main Class (Main.java):

Initializes the plugin, establishes the database connection, and manages plugin lifecycle events (onEnable() and onDisable()).
Dynamically locates and manages plant_growth.db within the plugin's data folder (getDataFolder()).
PlantGrowthManager (PlantGrowthManager.java):

Singleton class responsible for managing plant growth data.
Initializes the database schema (createDatabaseTable()) if it does not exist.
Loads and saves plant data (loadAllPlantData(), saveAllPlantData()).
Updates plant growth stages based on elapsed time since the chunk was last loaded (updatePlantGrowth()).
Event Listeners:

PlantEventListener (PlantEventListener.java):
Listens for plant growth related events and triggers updates to PlantGrowthManager accordingly.
ChunkEventListener (ChunkEventListener.java):
Listens for chunk load/unload events and triggers updates to PlantGrowthManager for plants within those chunks.
