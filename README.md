# SongPlayer
A Fabric mod for Minecraft that plays songs with noteblocks.
The current version is for Minecraft 1.19.x.

# How to install
You can grab the mod jar from releases section.
This mod requires fabric api.

# Adding songs
You can put midis or NoteBlockStudio files in the `.minecraft/songs` folder.
SongPlayer supports any valid midi and all versions of NBS files.

# Using the client
To get started, add some midis or nbs files to your songs folder, and use `$play <filename>` in an open area.
If you have provided it a valid midi or nbs file, it will try to set your gamemode to creative, place the required noteblocks for the song, try to switch you to survival, then start playing.

# Commands
All the commands are case insensitive.

### $help
Lists all SongPlayer commands

### $help \<command>
Explains a command and shows the command usage.

### $play \<filename or url>
Plays a particular midi from the .minecraft/songs folder, or, if a url is specified, downloads the song at that url and tries to play it.

If there is a song already playing, the new song will be added to the queue.

### $stop
Stops playing/building and clears the queue.

### $skip
Skips the current song and goes to the next one in the queue.

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

### $songs
*aliases: `$list`*

Lists the songs in your .minecraft/songs folder.

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

### $testSong
A command I used used for testing during development.
It plays every single noteblock sound possible in order.
However, there are 400 possible noteblocks but the max stage size is 300, so that last 100 notes aren't played.

# Mechanism
SongPlayer places noteblocks with nbt and instrument data already in them, so the noteblocks do not need to be individually tuned. Ayunami2000 has previously done a proof-of-concept of this method.

My client will automatically detect what noteblocks are needed and place them automatically before each song is played, which makes playing songs quite easy. The only drawback is that you need to be able to switch between creative and survival mode, which my client will attempt to do automatically.

When playing a song, freecam is enabled. You will be able to move around freely, but in reality you are only moving your camera while your player stays at the center of the noteblocks. This is because noteblocks can only be played if you're within reach distance of them, so you have to stand at the center of the noteblocks to play them, but it's still nice to be able to move around while your song is playing.
