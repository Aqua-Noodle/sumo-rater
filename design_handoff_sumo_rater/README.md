# Handoff: Sumo Rater (Android)

## Overview
**Deliverable: a signed Android APK** (Kotlin + Jetpack Compose, minSdk 26). The user cannot run this from a browser comfortably — build the native app.

A minimal Android app for rating the daily performance of sumo wrestlers (rikishi) in the Makuuchi and Juryo divisions. One screen per bout: winner left, loser right, two swipe-wheel number pickers between them. Ratings are stored on-device only. A "community pooled ratings" mode is planned later (see State Management → future).

## About the Design Files
`Sumo Rater.dc.html` (+ `support.js`, `image-slot.js`) is a **design reference built in HTML** — a working prototype showing intended look and behavior. It is NOT production code. Recreate it natively for Android (Kotlin + Jetpack Compose recommended; Flutter/React Native acceptable). The `image-slot.js` drop-zone is prototype scaffolding — ignore it. The app is full-screen (edge-to-edge); system status bar and gesture nav are the phone's own. Ship as an APK / Play Store app.

## Fidelity
**High-fidelity.** Colors, type, spacing and interactions are final; match them exactly using platform equivalents.

## Data source
sumo-api.com (free, no key):
- `GET /api/basho/{bashoId}/torikumi/{division}/{day}` — `bashoId` = `YYYYMM` (months 01,03,05,07,09,11), division `Juryo` | `Makuuchi`, day 1–15. Fields used: `eastId, eastShikona, eastRank, westId, westShikona, westRank, winnerId`. Winner = side whose id equals `winnerId`; skip entries with no `winnerId` (not yet fought).
- Rank strings ("Maegashira 4 East") are shortened to `M4e` (Y/O/S/K/M/J + number + e/w).
- Default basho/day on launch = most recent: basho starts 2nd Sunday of its month; day = days since start + 1 (cap 15); before the start → previous basho, day 15.
- Bout order: all Juryo bouts (API order), then all Makuuchi.
- Cache responses; history back to 1958 must be browsable offline once fetched. Show "No bout data available for {Basho Year}, day {n}." when empty. Never mention the API in UI copy.
- Photos: no source exists. Show a generic silhouette placeholder; allow the user to set a photo per rikishi id (persisted locally).

## Screens

### 1. Rate (home)
- Top bar 56dp: hamburger (48×48 hit area, three 20×2dp ink lines, 5dp gap) at left; centered caption `JURYO · DAY 5 · 3 / 34` (12sp, letter-spacing .14em, uppercase, color mute, tabular numerals).
- Center block, vertically centered, horizontal row, gap 8dp:
  - Winner column (92dp wide): photo 92×118dp, radius 6dp; below (8dp gap) name 14sp/600 centered, rank 11sp mute letter-spacing .04em.
  - Winner wheel: 60dp wide, 192dp tall viewport, rows 64dp, vertical scroll with snap-to-center, fade mask (transparent → opaque 33%–67% → transparent). Digits 46sp/600, tabular, letter-spacing −.03em. Items top→bottom: `10, 9 … 1`, then one **blank** row at the bottom (= not rated). Swiping DOWN (finger moves down) therefore increases the number; the blank sits just below 1/0 and is the default resting position. Wheels are shifted up 30dp relative to the photo column (`margin-top:-30dp`, aligned to top).
  - Dash: 18×3dp ink bar, same −30dp offset.
  - Loser wheel: same styling. Items top→bottom: `(winner−1) … 0`, then blank. If winner is blank: `10 … 0`, then blank.
  - Loser column mirrors the winner column.
  - Captions `WIN` / `LOSS` under the wheels: 10sp, letter-spacing .18em, uppercase, faint, gap 66dp, margin-top 4dp.
- Bottom row (padding 0 12dp 16dp): `← Back` left (15sp/500, faint, 48dp min height; hidden on the first bout) and `Next →` right (17sp/600 ink).
- Done state (after last bout): `{Basho Year} · Day {n} done` 28sp/600; `{N} bouts rated on this phone.` 14sp mute; outlined button `View my ratings` (1dp ink border, 12×20dp padding, 14sp/500).
- Empty state: message 14sp mute + outlined `Pick another day` button (opens Menu).

### 2. Menu (hamburger)
- `×` close 48×48 top-left.
- Row `My ratings` … `{n} rikishi →` (18sp/600, 18×24dp padding, 1dp top border line).
- `BASHO` label (10sp faint uppercase .14em) + full-width select, 1dp ink border, 12×14dp padding, 16sp/600, options newest→oldest down to Hatsu 1958, labels `Aki 2026`.
- `DAY` label + 5-column grid of 15 buttons, 44dp tall, 1dp line border, 15sp/500; selected = ink background, bg-colored text.
- `Dark mode` row (16sp/500) with a 40×22dp outlined pill toggle, 14dp ink knob (left 3dp off / 22dp on).
- Footer 11sp faint: `Ratings are stored on this phone only.`
- Picking a basho resets to day 1; picking a day loads that day and returns to Rate.

### 3. My ratings (table)
- `←` back to Menu; caption `MY RATINGS`.
- Header row 10sp faint uppercase: `RIKISHI | AVG | N`.
- Rows (10×20dp padding, 1dp top line): photo 36×46dp radius 4; name 15sp/600 + rank 11sp mute; avg 20sp/600 tabular (1 decimal) in 56dp column; count 12sp faint in 40dp column. Sorted by average desc. Only rikishi with ≥1 non-blank rating appear. Tap → Detail.
- Empty: `No ratings yet. Rate a bout and the rikishi appear here.`

### 4. Rikishi detail
- `←` back. Header: photo 72×92dp; name 24sp/600 letter-spacing −.02em; rank 12sp mute; right: avg 32sp/600, `AVG · {n}` 10sp faint uppercase.
- `RATING OVER TIME` label + segmented toggle `Days | Basho` (1dp ink border, 6×12dp padding, 11sp/500, selected = ink fill).
- Line chart, 340×150 logical units, y axis 0/5/10 (gridlines color line, labels 10sp faint), x from 24 to 340, y = 10 + (10−v)·12. Line 2dp ink, round joins. Dots r 3.5, 1.5dp ink stroke, fill = bg for wins, ink for losses. First/last x labels (`Aki 2026 d5` or basho name). Days mode: one point per rated bout, chronological. Basho mode: one point per basho = mean rating.
- Rating rows (12×20dp padding, 1dp top line): `vs {opponent}` 14sp/600; `{Basho Year} · Day {n}` 11sp 70% opacity; `WIN`/`LOSS` 11sp uppercase .14em 70%; rating 24sp/600 right-aligned in 40dp. **Win rows: background #FFFFFF, text #000000. Loss rows: background #000000, text #FFFFFF** (fixed, regardless of theme). Newest first.

## Interactions & Behavior
- Wheels: momentum scroll with snap; selection = row nearest center (debounce ~90ms after scroll end). Changing the winner clamps the loser to ≤ winner−1 and rebuilds the loser list; if the loser was blank it stays blank. Programmatic sync of both wheels when a bout loads: saved values, else BOTH BLANK (default = not rated).
- Rules: winner ∈ 1–10 or blank; loser ∈ 0–(winner−1) or blank; if winner blank, loser ∈ 0–10. Both blank = bout unrated (any stored rating is deleted).
- `Next →` saves the current bout's values and advances. `← Back` goes to the previous bout (no save). Resume position per basho-day is persisted.
- Swipe on the bout block (horizontal, ≥8px to start, cancelled if vertical movement dominates so wheel scrolling is unaffected):
  - While dragging: block follows finger, `translateX(dx) rotate(dx/40 deg)`, no transition.
  - Release with |dx| ≤ 70px: spring back, 450ms `cubic-bezier(.34,1.56,.64,1)`.
  - Release |dx| > 70px: fly out 220ms ease-in to ±520px with ±14° rotation and fade to 0; then (left = Next/save, right = Back) the new bout enters from the opposite side (offset ∓90px, ∓4°, opacity 0 → identity) with the same 450ms overshoot spring. Swiping right on the first bout only springs back.
- Dark mode toggle persisted; set system bar icon colors accordingly.

## State Management
- `ratings: { [key]: { bashoId, day, div, winnerId, winnerName, winnerRank, loserId, loserName, loserRank, w, l, ts } }` where key = `${bashoId}-${day}-${div}-${index}`; `w`/`l` may be null (blank). Persist (Room/SharedPreferences/DataStore).
- `pos: { [`${bashoId}-${day}`]: index }` resume positions; `where: { bashoId, day }` last viewed; `theme: 'light'|'dark'`.
- UI state: `screen ∈ rate|menu|table|detail`, `idx`, `w`, `l`, `mode ∈ day|basho`, `selected` rikishi id, swipe `dx / dragging / fly / enter`.
- Derived: per-rikishi aggregate from ratings (name, latest rank, list of {bashoId, day, opp, won, rating}, avg, count), skipping null ratings.
- **Future (do not build now, keep the model compatible):** community pooled ratings — server sync keyed by the same bout key + user id; only bouts of the *ongoing* basho are open to community rating, all older bashos (back to 1958) remain solo/offline-only.

## Design Tokens
Type: Archivo (Google Fonts), weights 400/500/600/700; fallback Helvetica/sans-serif. Tabular numerals wherever numbers are shown.

Light: bg #F4F1EB · ink #141311 · mute #5A564F · faint #9A958B · line #E4E0D7 · silhouette #E6E2D9 / #CFCAC0
Dark: bg #161513 · ink #F1EDE6 · mute #A8A39A · faint #6E6A63 · line #2A2926 · silhouette #26251F / #3A3832
Fixed: win row #FFFFFF on #000000 text; loss row #000000 with #FFFFFF text.

Spacing: 4 / 8 / 12 / 14 / 16 / 18 / 20 / 24 dp. Wheel row 64dp, viewport 192dp. Radii: 4 (thumb), 6 (photo). No shadows.

## Assets
- No image assets. Photo placeholder is a generated silhouette (rect + circle head + ellipse shoulders in the silhouette colors). Users may set their own photos per rikishi.
- Icons: hamburger (3 lines), `×`, `←`, `→` — text/simple shapes only.

## Files
- `Sumo Rater.dc.html` — the full prototype (template + logic in one file; the `class Component` block holds all behavior, data rules and the sample bout list).
- `support.js` — prototype runtime (ignore).
- `image-slot.js` — drag-and-drop photo placeholder used in the prototype (ignore; implement a native image picker instead).
