# SongPlayer
A Fabric mod for Minecraft that plays songs with noteblocks.
The current version is for Minecraft 1.20-1.20.1

# How to install
You can grab the mod jar from releases section.
This mod requires fabric api.

# Adding songs
You can put midis or NoteBlockStudio files in the `.minecraft/songs` folder.
SongPlayer supports any valid midi and all versions of NBS files.

# Using the client
To get started, add some midis or nbs files to your songs folder, and use `$play <filename>` in an open area.
Spaces are not supported in filenames or filepaths.
If you have provided it a valid midi or nbs file, it will try to set your gamemode to creative, place the required noteblocks for the song, try to switch you to survival, then start playing.

You can also organize songs into subdirectories in your songs folder. Tab completion will make it easy to navigate. Symlinked directories are supported too.

# Commands
All the commands are case insensitive.

### $help
### $help \<command>
If no arguments are given, lists all SongPlayer commands.
Otherwise, explains the specified command and shows its syntax.

### $play \<filename or url>
Plays a particular midi from the .minecraft/songs folder, or, if a url is specified, downloads the song at that url and tries to play it.

If there is a song already playing, the new song will be added to the queue.

### $stop
Stops playing/building and clears the queue.

### $skip
Skips the current song and goes to the next one.

### $goto \<mm:ss>
Goes to a specific time in the song.

### $loop
Toggles song looping.

### $status
*aliases: `$current`*

Gets the status of the current song that is playing.

### $queue
*aliases: `$showqueue`*

Shows all the songs in the queue.

### $playlist play \<playlist>
### $playlist create \<playlist>
### $playlist list \[\<playlist>]
### $playlist delete \<playlist> \<song>
### $playlist addSong \<playlist> \<song>
### $playlist removeSong \<playlist> \<song>
### $playlist renameSong \<playlist> \<old name> \<new name>
### $playlist loop
### $playlist shuffle

Create, edit, delete, or play playlists. You can also toggle looping or shuffling.

### $songs
### $songs \<subdirectory>
*aliases: `$list`*

If no arguments are given, lists songs in the `songs` folder. Otherwise, lists songs in the specified subdirectory.

### $setCreativeCommand \<command>
*aliases: `$sc`*

By default, the client uses /gmc to go into creative mode.
However, /gmc does not work on all servers.
If the creative command is different, set it with this command.
For example, if the server uses vanilla commands, do `$setCreativeCommand /gamemode creative`.

### $setSurvivalCommand \<command>
*aliases: `$ss`*

By default, the client uses /gms to go into survival mode.
However, /gms does not work on all servers.
If the survival command is different, set it with this command.
For example, if the server uses vanilla commands, do `$setSurvivalCommand /gamemode survival`.

### $useVanillaCommands
*aliases: `$essentials`, `$useEssentials`, `$essentialsCommands`*

Switch to using Essentials gamemode commands.

Equivalent to `$setCreativeCommand /gmc` and `$setSurvivalCommand /gms`

### $useVanillaCommands
*$aliases: `$vanilla`, `$useVanilla`, `$vanillaCommands`*

Switch to using vanilla gamemode commands.

Equivalent to `$setCreativeCommand /gamemode creative` and `$setSurvivalCommand /gamemode survival`

### $toggleFakePlayer
*aliases: `$fakePlayer`, `$fp`*

Toggles whether a fake player will show up to represent your true position while playing a song. When playing a song, since it automatically enables freecam, your true position will be different from your apparent position. The fake player will show where you actually are. By default, this is disabled.

### $setStageType \<DEFAULT | WIDE | SPHERICAL>
*aliases: `$setStage`, `$stageType`*

Sets the type of noteblock stage to build. Thanks Sk8kman and Lizard16 for the spherical stage design!
- Default: A square shaped stage with a maximum of 300 noteblocks
- Wide: A cylindrical stage with a maximum of 360 noteblocks
- Spherical: A densely packed spherical stage that can contain all 400 possible noteblocks

### $toggleMovement <swing | rotate>
*aliases: `$movement`*

Toggles whether you swing your arm when hitting a noteblock and rotate to look at the noteblocks you are hitting.

### $songItem create \<song or url>
### $songItem setSongName \<name>
*aliases: `$item`*

Encodes song data into an item. When you right click on such an item, SongPlayer will automatically detect that it is a song item and ask if you want to play it. These items, once created, can be used by anyone that is using the necessary version of SongPlayer.

It will automatically generate custom item names and lore, but these can be modified or deleted without affecting the song data, so feel free to edit the items as you wish. SongPlayer only looks at the `SongItemData` tag.

### $testSong
A command I used for testing during development.
It plays all 400 possible noteblock sounds in order.

# Mechanism
SongPlayer places noteblocks with nbt and instrument data already in them, so the noteblocks do not need to be individually tuned.

The client will automatically detect what noteblocks are needed and place them automatically before each song is played, which makes playing songs quite easy. The only drawback is that you need to be able to switch between creative and survival mode, which my client will attempt to do automatically.

When playing a song, freecam is enabled. You will be able to move around freely, but in reality you are only moving your camera while your player stays at the center of the noteblocks. This is because noteblocks can only be played if you're within reach distance of them, so you have to stand at the center of the noteblocks to play them, but it's still nice to be able to move around while your song is playing.

## Acknowledgements
**Ayunami2000**: Came up with the concept of directly placing noteblocks with nbt data instead of manually tuning them.

**Sk8kman**: Several of Songplayer 3.0's changes were inspired by their fork of SongPlayer. Most notably was their alternate stage designs, but it also motivated me to implement playlists and togglable movements.

**Lizard16**: Cited by Sk8kman as the person who made the spherical stage design.

