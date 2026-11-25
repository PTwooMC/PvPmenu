# Ultimate PvP Utilities (Fabric, Minecraft Java 1.21.11)

Client-side, fair-play PvP utilities in one mod. Press **Right Shift** in game to open the menu
(rebind under Options > Controls > Key Binds > Ultimate PvP Utilities). Settings save to `config/pvpmenu.json`.

## Menu tabs
Home, HUD, Visuals, Crosshair, Utilities, Warm-up, Presets, Settings, Guide. A search box finds any setting.

- **HUD**: cards (armor, totems, CPS, keystrokes, combo, target info, info, item counters for any item or potion,
  potion timers, custom text with {fps} {ping} {cps} ...). Rename any card, edit the layout by dragging.
- **Visuals**: screen tweaks, fullbright, see through lava, clear water, No Fog, particles, hands.
- **Crosshair**: Simple or Advanced mode, looks for target/hit/etc., pixel editor, PNG image crosshairs (drag a PNG onto the menu).
- **Utilities**: food and saturation display, container preview, TNT fuse countdown, shield indicator, shield sound control,
  slot key labels, freelook, zoom, toggle sprint/sneak.
- **Warm-up**: reaction test, click speed test, tracking test, aim trainer.
- **Guide**: how-to, fair-play note, troubleshooting, changelog, copy diagnostics.

## Build
JDK 21 and Gradle (IntelliJ sets this up). `./gradlew build` -> `build/libs/ultimate-pvp-utilities-1.0.0.jar`.
Never use the `-sources.jar`. Needs Fabric API for 1.21.11.

## Credits
See `META-INF/licenses/THIRD_PARTY_NOTICES.txt` inside the jar.
