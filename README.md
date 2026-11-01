# WaveyElytra

**English** | [Русский](README_RU.md)

## About

**WaveyElytra** brings your elytra to life. Instead of flat, motionless wings, the mod renders a three-dimensional model of the same vanilla shape and gives it physics. On the ground the wings sway like a cape in the wind, and in flight they behave like a falcon's wings: they stay firm, flap when you speed up and tuck in against the body when you dive.

The silhouette, size, texture and vanilla opening animation are kept: vanilla and custom elytra textures, capes used as elytra, dyes and the enchantment glint all work.

Customize the look and behavior of the wings using the in-game config screen, accessible via the mod list.

Since WaveyElytra is purely visual and fully client-side, it works on any server without requiring installation server-side.

## Features

### 3D Model

- The wing is extruded one pixel deep from its texture, like items held in hand: every pixel of the texture becomes a tiny cube
- Every opaque pixel on the edge has side faces, so the wing is visible from the side instead of being a thin panel
- The shoulders and the inner edge of the wing are drawn just like on the vanilla texture
- The shape comes from the texture itself, so custom elytra and profile capes render without holes along the edges

### On the Ground

- The wings behave like a cape in the wind: the faster you move, the further back they lean
- While standing, the wings sway gently in gusts of wind, and waves run along them while walking and sprinting
- The wings spread apart and never merge into a single piece
- After you stop, the wings settle down smoothly, with inertia
- While crouching, the wings lie along the bent back, with no shaking or wobbling

### In Flight

- The wings are rigid, with no waves: only the whole wing moves
- In level flight they are spread out and tremble slightly
- When boosting with a firework they make a few quick flaps
- They flap on sharp turns
- In a dive they tuck in against the body and sweep back, but never cross each other
- All transitions are smooth, without jerks

### Helicopter Mode

- In flight the wings meet in the middle of the back and spin like a rotor
- The faster you fly, the faster the rotor spins
- After landing the wings make a few more turns, gradually slowing down, and return to their place
- Its own rotor sound, which can be turned off separately

### Settings

- Turn the mod on and off: when disabled, the vanilla elytra come back
- Helicopter mode and helicopter sound
- Wing size
- Stiffness, weight, air drag, flutter and maximum bend of the wings
- Number of wing segments
- Simulation range and simplification range for other players