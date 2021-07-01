# SongPlayer
A Fabric mod for Minecraft that plays noteblocks. The current version is for Minecraft 1.17.

# How to use
The mod is in build/lib/song-player-1.1.0.jar. There will also be a releases section where you can grab my mod from.

This mod requires fabric api.

# Adding songs
You can put midis in the .minecraft/songs folder.

# Using the client
All the commands are case insensitive.

## $help
Lists commands

## $help \<command>
Explains a command and shows the command usage.

## $play \<file name>
Plays a particular midi from the .minecraft/songs folder.

## $playurl \<url>
Plays a midi from a particular url. The url should be a direct download link to a midi.

## $stop
Stops playing/building

## $goto \<mm:ss>
Goes to a specific time in the song.

## $loop
Toggles song looping.

## $current
Gets the current song that is playing.

## $songs
Lists the songs in your .minecraft/songs folder.

## $setCreativeCommand \<command>
By default, the client uses /gmc to go into creative mode. However, /gmc does not work on all servers. If the creative command is different, set it with this command. For example, if the server uses vanilla commands, do `$setCreativeCommand /gamemode creative`.

## $setSurvivalCommand \<command>
By default, the client uses /gms to go into survival mode. However, /gms does not work on all servers. If the survival command is different, set it with this command. For example, if the server uses vanilla commands, do `$setSurvivalCommand /gamemode survival`.

## $toggleFakePlayer
Toggles whether a fake player will show up to represent your true position while playing a song. When playing a song, since it automatically enables freecam, your true position will be different from your apparent position. The fake player will show where you actually are. By default, this is disabled.

# Mechanism
SongPlayer places noteblocks with nbt and instrument data already in them, so the noteblocks do not need to be individually tuned. Ayunami2000 had previously done a proof-of-concept of this method.

My client will automatically detect what noteblocks are needed and place them automatically before each song is played, which makes playing songs quite easy. The only drawback is that you need to be able to switch between creative and survival mode, which my client will attempt to do automatically.

When playing a song, freecam is enabled. You will be able to move around freely, but in reality you are only moving your camera while your player stays at the center of the noteblocks. This is because noteblocks can only be played if you're within reach distance of them, so you have to stand at the center of the noteblocks to play them, but it's still nice to be able to move around while your song is playing.

# 1.17 Support
I was pretty late to update my client to 1.17, so Ayunami2000 decided to do it for me, which is pretty neat. You can see their repo here: https://github.com/ayunami2000/SongPlayer-1.17. At the moment of writing this however, they don't seem to have updated the noteblock base id, which would make the client get the notes wrong, so I don't recommend using it.

I've updated SongPlayer to 1.17 myself now so you can get it from here.
