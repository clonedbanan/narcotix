Narcotix Cop Mob Patch

Extract this ZIP over your Narcotix project folder, then run:

cd "C:\Users\caleb\Documents\GitHub\Narcotix"
powershell -ExecutionPolicy Bypass -File ".\APPLY_COP_MOB_PATCH.ps1"
.\gradlew.bat clean runClient

Textures included in this ZIP are placed at:
- src/main/resources/assets/narcotix/textures/entity/cop.png
- src/main/resources/assets/narcotix/textures/item/billy_club.png
- src/main/resources/assets/narcotix/textures/item/cop_spawn_egg.png
- src/main/resources/assets/narcotix/textures/item/wandering_plug_spawn_egg.png

Test commands:
/summon narcotix:cop
/give @s narcotix:cop_spawn_egg
/give @s narcotix:wandering_plug_spawn_egg
/give @s narcotix:billy_club

Notes:
- Cops target Wandering Plug entities.
- Cops hold a Billy Club.
- Cops use villager-like sounds.
- Cops do not add door-opening goals.
- Cops periodically scan nearby containers and remove Narcotix contraband except seeds.
- Natural spawn is attempted as an overworld creature spawn with ON_GROUND placement.
