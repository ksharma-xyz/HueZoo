# Huezoo — Stitch Design Review

> **How to use this doc**
> 1. Open the `screen.png` files alongside this doc (paths listed per screen).
> 2. Fill in every `[ YOUR FEEDBACK ]` block — a word or two is enough.
> 3. Update the `Keep / Change / Remove` tables.
> 4. Run the ready-made "Next Stitch Prompt" at the bottom when done.
>
> Source of truth: `docs/IMPLEMENTATION_PLAN_LEVELS_ECONOMY_UX.md`
> Bundle reviewed: `docs/stitch_huezoo_prd_design_doc/`

---

## Quick Orientation — What Was Generated

| # | Screen folder                    | Spec section                            |
|---|----------------------------------|-----------------------------------------|
| 1 | `huezoo_splash_screen_concept/`  | §4.2 Splash animation                   |
| 2 | `huezoo_home_premium_neon/`      | §4.1 Top bar + Home                     |
| 3 | `huezoo_gameplay_flower_layout/` | §4.5 Flower layout + §4.6 Feedback slot |
| 4 | `huezoo_levels_progress/`        | §4.1 Level bottom sheet                 |
| 5 | `huezoo_refill_out_of_tries/`    | §4.4 Gem economy + ads                  |

Open all five screens: `open docs/stitch_huezoo_prd_design_doc/*/screen.png`

---

---

## Screen 1 — Splash

📁 `huezoo_splash_screen_concept/screen.png`

### What Stitch built

- `Hue` solid white, `Zoo` outline cyan — wordmark rule followed ✅
- Thin animated beam below the wordmark simulating load state
- Three small squares at the bottom in cyan / magenta / gold (level color hint) ✅
- `"System.Initializing"` pulsing label below wordmark
- Very minimal — dark bg, no background imagery

### Spec compliance

| Spec requirement                | Status    | Notes                                  |
|---------------------------------|-----------|----------------------------------------|
| Hue solid + Zoo outline         | ✅ Done    | Correct rendering                      |
| Flicker inside Zoo letters      | ❌ Missing | No flicker keyframes in HTML           |
| 3 level colors cycle one by one | ❌ Missing | Only 3 static dots shown, no animation |
| Ends on current level color     | ❌ Missing | Static — no color state                |
| Feels neon/electric not chaotic | ✅ Partial | Restrained but also very bare          |

### Your Feedback

**Overall vibe:** `[ YOUR FEEDBACK: too minimal / right energy / needs more drama / etc. ]`

Splash screen must have same text style as we have ins the hue top bar, and the E and Z spacing
should be - as per already defined and make it reusabel and use in spalsh screen.
the splash screen logo will be same style but larger like 2x larger than the one inside the top bar,
but consider that it fits properly inside the
screen width. and has atleast 56 dp padding on each side. so should be within one single line.
Zoo text will be outline only and will flicker with colors and change levels as there are in the
app. 3 levels. and
then once flicker animation and color changes ahve completed then navigate to main home screen.
you cannot navigate back to spalsh screen once in hoime screen, .spalsh is displayed on fresh app
star only, see krail app for guidance.

**The wordmark size + position:** `[ YOUR FEEDBACK ]`

**The "System.Initializing" label:** `[ keep / remove / change copy ]`

**The 3 colored squares at the bottom:** use the shapes at random from confetti to be dispalyed
here. we need consistency in design syste

**The loading beam underneath wordmark:** `dont need this or keep or improve based off ur own ideas`

**Missing flicker animation — priority?** `[ must have before next round]`

### Keep / Change / Remove

| Decision  | Element | Note |
|-----------|---------|------|
| ✅ Keep    |         |      |
| 🔄 Change |         |      |
| ❌ Remove  |         |      |

---

---

## Screen 2 — Home

📁 `huezoo_home_premium_neon/screen.png`

### What Stitch built

- Top bar: `Hue` solid + `Zoo` outline, `ROOKIE / LEVEL 01` label top-right ✅
- Gems counter prominently: `1,250 GEMS` in large headline type ✅
- "The Threshold" hero card — very large, with a sci-fi background image
- Level progress arc/ring chart on right side (shows 72% to next level)
- Daily Challenge card and Global Leaderboard card below
- Bottom nav: 5 tabs, floating center "+" action button
- "VANGUARD" used as Level 2 name — **spec uses "Skilled"** ⚠️
- Streak (`14 DAYS`) and rank (`#412`) in top stats row - we need this.
- No tap gesture explained for top bar → level bottom sheet -> when top app bar huezoo text is
  taped, we naviate to the levels of app screen which dispalys different levels of app and then
  dispalys which level you are on, and toher levels below in kinds didsabled state.
-
- `+500 XP` shown on Threshold card — spec uses Gems, not XP ⚠️ -> ytes we use gems not XP.
- Background image on hero card: sci-fi scientist figure - > we will need to add some figures, if
  this is an image available use it. or we will add , so keep a palceholder for now.
  the image should be a sci-fi scientist figure, but if you dont have one, you can use an abstract
  gradient or pattern as a placeholder for now. the key is to have some visual interest on the card
  without making it too busy or distracting from the main information. we can iterate on the
  specific image choice in future rounds, but for now just make sure there is something there that
  gives us a sense of personality and vibe for the hero card.

### Spec compliance

| Spec requirement                       | Status     | Notes                                |
|----------------------------------------|------------|--------------------------------------|
| Hue solid + Zoo outline in top bar     | ✅ Done     |                                      |
| Level color accent grows with level    | ✅ Partial  | Only cyan shown (Level 1 correct)    |
| Top bar tap opens level bottom sheet   | ❌ Missing  | No tap interaction defined           |
| Gems displayed                         | ✅ Done     | Shows 1,250                          |
| Level names: Rookie / Skilled / Master | ⚠️ Partial | Uses "Vanguard" instead of "Skilled" |
| No XP — economy is Gems                | ⚠️ Off     | Shows "+500 XP" on hero card         |

### Your Feedback

**Overall home screen layout:**
`[ YOUR FEEDBACK: looks good, lets use it and make it slightly more emty space and spread out.]`

the copy text and the levels and the other things on the home screen, we need to make sure that they are consistent with the copy and
with the guidelines of the app ux.

### Keep / Change / Remove

| Decision  | Element | Note |
|-----------|---------|------|
| ✅ Keep    |         |      |
| 🔄 Change |         |      |
| ❌ Remove  |         |      |

---

---

## Screen 3 — Gameplay (Flower Layout)

📁 `huezoo_gameplay_flower_layout/screen.png`

### What Stitch built

- Fixed-height HUD feedback slot (`h-12`) at top ✅ — height is constant
- Feedback text: `"GREAT STREAK"` shown in cyan headline type
- Two slanted/skewed HUD chips: `Time Remaining 0:42` + `Calibration 12/50`
- 6 rectangular petals arranged in radial flower pattern ✅
- Each petal is 80×120px rectangle, rotated with `transform-origin: center 160px`
- Petal 3 (120deg) uses slightly different color (`#4633a1` vs `#3d2c8d`) to simulate odd one out
- Center hub with a pentagon icon
- Large cyan rocket button below the flower
- Background: subtle cyan dot grid pattern
- Bottom nav still visible (should arguably be hidden during gameplay)

### Spec compliance

| Spec requirement                         | Status    | Notes                                   |
|------------------------------------------|-----------|-----------------------------------------|
| Fixed-height feedback slot               | ✅ Done    | `h-12` fixed, text inside it            |
| Feedback text does not shift layout      | ✅ Done    | Container always same height            |
| 6 petal radial layout                    | ✅ Done    | Radial rotation correct                 |
| Petal shape is petal-like / organic      | ❌ Off     | Petals are plain rectangles, not curved |
| Unfold animation entering                | ❌ Missing | No entrance animation                   |
| Fold animation exiting before next round | ❌ Missing | No exit choreography                    |
| Center hub meaningful                    | ⚠️ Odd    | Pentagon icon doesn't mean anything     |
| No bottom nav during active play         | ❌ Present | Nav bar still shows                     |

### Your Feedback

**Overall gameplay layout:** `[ YOUR FEEDBACK: feels right / too tall / right density ]`

**The feedback slot ("GREAT STREAK"):**
`[ perfect position / move to bottom / too tall / too short ]`

**The slanted HUD chips (Time / Calibration):**
`[ love the skew / remove skew / keep skew but change data shown ]`

**The petal shapes (currently rectangles in flower pattern):**
`[ rectangles are fine / want actual curved petals / want different shapes ]`

**The center hub (pentagon icon):** `[ remove it / put something meaningful / put ΔE value there ]`

**The rocket launch button below flower:** `[ keep / remove / move / replace with something else ]`

**Bottom nav during gameplay:** `[ hide it / keep it / reduce to back-only ]`

**Background dot grid:** `[ keep / too noisy / subtle enough / remove ]`

### Keep / Change / Remove

| Decision  | Element | Note |
|-----------|---------|------|
| ✅ Keep    |         |      |
| 🔄 Change |         |      |
| ❌ Remove  |         |      |

---

---

## Screen 4 — Levels Bottom Sheet

📁 `huezoo_levels_progress/screen.png`

### What Stitch built

- Modal bottom sheet overlays dimmed home screen ✅
- Sheet header: "Levels & Progress" with gradient underline (cyan→magenta→gold) ✅
- 3 level cards stacked vertically:
    - **Level 1 Rookie** — cyan accent, potted plant icon, 0-199 threshold ✅
    - **Level 2 Skilled** — magenta accent, bolt icon, 200-49,999 threshold ✅
    - **Level 3 Master** — gold accent, medal icon, 50,000+ threshold ✅
- Each card shows: threshold, difficulty label, rewards multiplier, prestige badge name
- "Close Details" button at the bottom
- Top bar still shows "HueZoo" but without Hue/Zoo split styling ⚠️
- `"LVL 1"` shown in top right as a tappable button — triggers this sheet

### Spec compliance

| Spec requirement                     | Status    | Notes                           |
|--------------------------------------|-----------|---------------------------------|
| 3 levels, correct names              | ✅ Done    | Rookie / Skilled / Master       |
| Level colors (cyan / magenta / gold) | ✅ Done    | Correctly assigned              |
| Entry thresholds shown               | ✅ Done    | 0-199 / 200-49,999 / 50,000+    |
| What changes at each level           | ✅ Done    | Difficulty + Rewards + Prestige |
| Bottom sheet opened from top bar     | ✅ Done    | "LVL 1" in top bar triggers it  |
| Sheet handle bar                     | ✅ Done    | 12px pill at top of sheet       |
| Hue/Zoo split in top bar             | ❌ Missing | "HueZoo" rendered as plain text |

### Your Feedback

**Overall levels bottom sheet:**
`[ YOUR FEEDBACK: looks great / too much info / need less text / layout is right ]`

**The 3 level cards stacked:** `[ keep stacked / side-by-side / something else ]`

**The gradient header underline (cyan→magenta→gold):**
`[ love it / remove / subtle enough / too much ]`

**Level icons (plant / bolt / medal):** `[ keep / change icons / no icons needed ]`

**"What Changes" grid (Difficulty / Rewards / Prestige):**
`[ keep all 3 rows / simplify / add more info ]`

**"Close Details" button at bottom:** `[ keep / replace with swipe-to-dismiss only / fine as-is ]`

**"LVL 1" trigger in top bar:**
`[ good trigger / prefer tapping the wordmark instead / add chevron ]`

### Keep / Change / Remove

| Decision  | Element | Note |
|-----------|---------|------|
| ✅ Keep    |         |      |
| 🔄 Change |         |      |
| ❌ Remove  |         |      |

---

---

## Screen 5 — Refill / Out of Tries

📁 `huezoo_refill_out_of_tries/screen.png`

### What Stitch built

- Top bar: `Hue` solid + `Zoo` outline ✅ + `"AD-FREE"` badge in gold (paid user state) ✅
- Centered modal card — "Out of Tries" headline
- `"Energy Depleted"` subtitle with magenta dividers
- Gem balance: large `120` with diamond icon ✅
- Gem refill option: `"300 GEMS = 10 QUESTIONS"` ✅ — button **disabled** (not enough gems)
- "Insufficient gems: 180 more needed" error label ✅ — transparent math
- "OR" divider
- "WATCH AD" primary CTA — active and glowing ✅
- `"Server: Kinetic-Vault-01"` footer label — unclear purpose ⚠️
- No "Back to Home" option
- No "earn more gems" guidance
- Background: subtle grid + blurred glow blobs

### Spec compliance

| Spec requirement                         | Status    | Notes                       |
|------------------------------------------|-----------|-----------------------------|
| Gem balance shown                        | ✅ Done    | 120 gems displayed          |
| 300 gems = 10 questions conversion shown | ✅ Done    | Explicit and visible        |
| Refill disabled when insufficient        | ✅ Done    | Disabled + error shown      |
| "180 more needed" transparent math       | ✅ Done    | Clear shortfall displayed   |
| Watch Ad as fallback CTA                 | ✅ Done    | Primary button styling      |
| AD-FREE badge for paid user              | ✅ Done    | Gold badge visible          |
| No back to home option                   | ❌ Missing | User feels trapped          |
| Earn gems guidance                       | ❌ Missing | No "play daily to earn" tip |
| Economy: 1 gem per correct shown         | ❌ Missing | Rate not communicated       |

### Your Feedback

**Overall refill screen:** `[ YOUR FEEDBACK: clear / too complex / right energy / feels punishing ]`

**"Out of Tries" headline:** `[ good / too dramatic / change copy ]`

**"Energy Depleted" subtitle:** `[ love it / too sci-fi / change to plain language ]`

**Gem balance display (large number + diamond icon):** `[ keep / make smaller / move position ]`

**"300 GEMS = 10 QUESTIONS" label:** `[ perfect / too prominent / move elsewhere ]`

**Disabled refill button with error math:**
`[ keep this pattern / remove error / show progress bar instead ]`

**"WATCH AD" as active primary CTA:** `[ correct hierarchy / should be secondary / remove ]`

**"Server: Kinetic-Vault-01" footer label:** `[ keep / remove / replace with something useful ]`

**Missing: Back to Home:** `[ add it / not needed / user should use system back ]`

### Keep / Change / Remove

| Decision  | Element | Note |
|-----------|---------|------|
| ✅ Keep    |         |      |
| 🔄 Change |         |      |
| ❌ Remove  |         |      |

---

---

## Cross-Screen Issues (Found in Analysis)

These appear across multiple screens — decide once, applies everywhere.

| Issue                                                                                   | Screens affected                                 | Your call                                                         |
|-----------------------------------------------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------------|
| Font: Antonio used for headlines — spec uses Bebas Neue + Clash Display + Space Grotesk | All                                              | `[ fix fonts / keep Antonio / mix ]`                              |
| Border-radius: `0px` everywhere — very sharp, no squircle shapes                        | All                                              | `[ keep sharp / add squircle for swatches / apply to cards too ]` |
| `borderRadius: {"DEFAULT": "0px"}` in Tailwind config                                   | All                                              | `[ keep / override per component ]`                               |
| Hue/Zoo split inconsistent across screens                                               | Home ✅, Gameplay ✅, Refill ✅, Levels ❌, Splash ✅ | `[ enforce split everywhere ]`                                    |
| Bottom nav visible during gameplay                                                      | Screen 3                                         | `[ hide in gameplay / keep ]`                                     |
| XP terminology used instead of Gems                                                     | Screen 2                                         | `[ fix everywhere ]`                                              |
| Level names: "Vanguard" appears once, doesn't match Rookie/Skilled/Master               | Screen 2                                         | `[ rename to Skilled everywhere ]`                                |
| No animation / motion code in any screen                                                | All                                              | `[ add as priority / defer / add keyframes next round ]`          |

---

---

## Global Decisions

Fill these once — they'll be injected into every next prompt.

**Preferred font stack:**
`[ Keep Antonio / Switch to Bebas Neue for headlines + Space Grotesk for body / Mixed ]`

**Corner radius style:**
`[ Keep all-sharp (0px) / Add squircle for swatches / Subtle radius (8px) for cards only ]`

**Color palette locked?**
`[ Colours feel right / Too neon / Not neon enough / Adjust specific colour: ________ ]`

**Overall energy level:**
`[ More intense / Exactly right / Pull back slightly ]`

**Animation priority for next round:**
`[ Splash flicker first / Flower unfold first / Both / Defer all animation ]`

---

---

## Non-Negotiables (Lock These — Do Not Change)

These must survive every future iteration:

1. `Hue` solid + `Zoo` outline — every screen, every state
2. 3 levels: Rookie (0-199) / Skilled (200-49,999) / Master (50,000+)
3. Level colors: Cyan (L1) / Magenta (L2) / Gold (L3)
4. Fixed-height feedback slot during gameplay — no layout shift
5. Economy: 1 gem per correct, 300 gems = 10 questions
6. No forced ads mid-round

---

---

## Next Stitch Prompt (Copy/Edit/Paste)

After filling in your feedback above, copy this prompt and insert your decisions into the
`[DECISIONS]` section.

```
Iterate on the HueZoo Stitch design. Use the existing 5 screens as the baseline.

NON-NEGOTIABLES (do not change):
- Wordmark: "Hue" solid fill, "Zoo" outline stroke — every screen, no exceptions
- 3 levels: Rookie (0-199) / Skilled (200-49,999) / Master (50,000+)
- Level colors: Cyan = Level 1, Magenta = Level 2, Gold = Level 3
- Gem economy: 1 gem per correct, 300 gems = 10 questions refill
- Gameplay feedback slot must be fixed height (no layout shift on message change)
- Font stack: Bebas Neue (headlines/numbers) + Space Grotesk (body/labels)

KEEP FROM PREVIOUS OUTPUT:
[ Fill from your "Keep" rows above — e.g. ]
- Refill screen: gem balance display with diamond icon + shortfall math
- Levels sheet: 3 stacked level cards with per-level color accents
- Home: level progress ring chart and stats row
- Flower layout: 6-petal radial arrangement with center hub

CHANGE FROM PREVIOUS OUTPUT:
[ Fill from your "Change" rows above — e.g. ]
- Splash: add actual flicker animation keyframes inside "Zoo" outline text
- Splash: animate 3 level colors cycling one by one (not just static dots)
- Home: replace "VANGUARD" with "SKILLED", replace XP with Gems
- Home hero card: replace sci-fi image with abstract color gradient or remove image
- Gameplay: replace rectangular petals with curved petal shapes (use border-radius + rotation)
- Gameplay: hide bottom nav during active gameplay round
- Gameplay: center hub — replace pentagon icon with live ΔE value
- Levels sheet: apply Hue/Zoo split styling to top bar (same as other screens)
- Refill: add "Back to Home" secondary link below Watch Ad button
- Refill: add small tip text "Earn gems: +1 per correct answer"
- Refill: remove "Server: Kinetic-Vault-01" footer label

REMOVE FROM PREVIOUS OUTPUT:
[ Fill from your "Remove" rows above — e.g. ]
- [nothing decided yet]

ADDITIONAL INSTRUCTIONS:
- Apply font fix: Use Antonio ONLY as fallback; primary headlines = Bebas Neue, body = Space Grotesk
- Ensure border-radius is 0px on cards, but apply squircle path on swatch/petal tiles
- Do not use any XP references — economy is Gems only
- Add CSS keyframe animation for the splash flicker (multi-step opacity on Zoo outline interior glow)
- Add CSS keyframe animation for flower petal entrance (stagger each petal 60ms apart, scale from 0.3 to 1)

SCREENS TO REGENERATE:
[ List only the screens you want changed ]
1. Splash screen
2. Home screen
3. Gameplay flower layout
4. Levels bottom sheet
5. Refill / out of tries

OUTPUT FORMAT:
Same structure as before — one HTML file per screen + screen PNG.
```

---

## Iteration Log

Track changes across rounds so you can diff easily.

| Round        | Date       | Bundle folder                   | Key changes made                      |
|--------------|------------|---------------------------------|---------------------------------------|
| 1 (baseline) | 2026-03-20 | `stitch_huezoo_prd_design_doc/` | Initial generation from master prompt |
| 2            |            |                                 |                                       |
| 3            |            |                                 |                                       |

