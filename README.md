# Gips

A mod that gets you the NBT of items and containers.

## What is it intended to do?

Gips can be used to easily grab the NBT of containers while in GUIs like chests and shulkers, and by looking at blocks and entities.

### Why?

It can be used to read special items in a pinch when you are in survival or a GUI. After that, you can copy the NBT into a creative world. What "special items" is, is something you define: crash eggs, 32ks, written books, [buycraft vouchers](https://youtu.be/kKvAo9rzv5g?t=538), negative damage axes, or item exploits aimed to crash your game.

## What can it not do?

* Get the contents of containers by simply looking at them (Clientside limitation)
* Grab the lore of placed containers / blocks
* Set the NBT of a container based on the NBT copied while inside a container
* Duplication glitches

## How do I use it?

### Getting the NBT a whole container
1. Open the container
2. Press the `Copy NBT` button to grab the NBT, or `Copy Name` to get the name (including formatting) of the container.

### Dumping the NBT of a container
1. Open the container
2. Press the `Dump NBT` button.
3. Go to your Minecraft folder and find the folder named `gips`

Note: You will also automatically dump the NBT when you close the container.

### Getting the NBT of an item inside a container
1. Open the container
2. Hover above the item you want to copy the NBT of
3. Press the `G` key.

Alternatively, to copy the name, press `Y`.

### Getting the NBT of a block, fluid, or entity
1. Stare at one of the above, the range is 50 blocks, more than the debug range, so you have a lot of wiggle room.
2. Press the `G` key

Depending on your permission level (on a server), you may get the contents of chests and entities (i.e. chested donkeys) without opening them.

### Changing the controls
1. Go to Options > Controls > Key Binds
2. Scroll down until you find the `Gips` category.

## Commands

### `gips viewnbt [copy] [path]`
You must be holding an item to view its NBT.

### `gips give <item> [amount]`
Gives you an item, do not put a space between the item and its NBT, must be in creative mode.

### `gips modifynbt <data>`
Modifies the item you are holding, must be in creative mode.

### `gips dump`
Toggles the dumping feature of the mod, this feature is hardcoded to be on by default at startup. The NBT is dumped to a file under `<minecraftFolder>/gips`, with a name of a date similar to the screenshot date, followed by the container name. **This is only triggered every time you open a container and NOT when you look at anything.** No filtering is done, unlike when you manually copy the NBT. **Dumping will pipe the player's inventory in addition to the container contents.**