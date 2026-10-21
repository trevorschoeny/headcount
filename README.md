# Headcount

Headcount is for entity cramming builds and mob farms. Look at an entity crammed in with others and it tells you how many are packed together. Scroll to pick any one of them, and your next click hits that one. It's handy for waxing one copper golem out of a crowd.

With Headcount on the server too, you can also keep counts of the entities around you on your HUD, for ranges and entity types you choose. The cramming count and scrolling work on any server.

## Features

### Cramming count

- Look at an entity that's crammed in with others, and the number of entities packed together shows just under your crosshair.
- It counts the entity you're looking at and every entity touching it, the same check the game uses for cramming.
- An entity on its own shows nothing, and no limit is shown next to the count.

### Scroll to select

- While you look at crammed entities, the mouse wheel steps through them instead of your hotbar, and the picked one gets an outline.
- Your next click goes to the picked entity, whether that's a honeycomb, a name tag, a lead, shears, a trade, or an attack.

### Range counts

- Keep up to 16 ranges, each a sphere around you, and see how many entities are inside each one.
- Each range has its own radius, a category, and a list of the entity types to count.
- Any range can show as a line on your HUD.
- Range counts need Headcount on the server.

## Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [MenuKit](https://modrinth.com/mod/menukit) 7.3.0 or newer, below 8.0.0, which draws the settings screen, the range list, and the HUD lines.

## Compatibility

It runs on Minecraft 26.3 with Fabric. On the client alone, the cramming count and Scroll to select work on any server. Install it on the server too, with MenuKit and Fabric API, for range counts.

## How to use

### Cramming count

Look at an entity. If any other entity's box overlaps its box, a white number under your crosshair shows how many entities are crammed together, counting the one you're looking at. Only entities touching that one are counted, so in a row of mobs that each touch their neighbors, the one in the middle shows 3.

### Scroll to select

- While the count is showing, a gray Scroll to select hint sits under it until you scroll.
- Scroll the mouse wheel to step through the crammed entities. The line changes to show which one you're on, like 3 / 18, and that entity gets an outline.
- Your next click goes to the picked entity instead of the one under your crosshair. That covers anything aimed at one entity: items like honeycomb, name tags, leads, and shears, trading, and attacking.
- Looking away gives the wheel back to your hotbar and drops the pick. Scroll the same group again and it carries on from where you left off, as long as the same entities are still crammed together.

### Range counts

A range counts the entities within a set distance of you. You start with three: all entities within 16 blocks, hostile mobs within 32 blocks, and hostile mobs within 128 blocks.

The ranges are in Headcount's settings, in a list.

- Add range adds a range of 16 blocks that counts every entity, shown on the HUD. It's grayed once you have 16.
- Each row has Within … blocks (1 to 256), a category dropdown (all, mobs, hostile, passive, or non-mobs), and a dropdown of every entity type in that category, all ticked. Untick the types you don't want counted.
- The HUD toggle on a row shows that range on your screen as a line, like Hostile within 32 blocks: 6. Hostile counts are tinted red, passive counts green, and a line dims when its count is zero.
- The up and down buttons reorder the rows, and remove deletes one without asking.
- On a server without Headcount the list is grayed, with a note saying why. Outside a world you can still edit it.

### Settings

Open Headcount's settings from Mod Menu or Mod Hub.

| Setting | Default |
| --- | --- |
| Cramming count | On |
| Scroll to select | On |
| Range counts on screen | Top left |

Range counts on screen can be top left, top center, top right, bottom left, or bottom right.

## License

MIT.
