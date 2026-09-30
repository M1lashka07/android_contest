# Figma implementation reference

Captured on 2026-09-30 from the user-supplied [Figma design](https://www.figma.com/design/wDMrBSoV985s6SlXOJnbEb/?node-id=0-1).
The reference page is Sprint-3, node 0:1. The application identity shown in the artwork is **GAAMETIME**.

## Evidence and local assets

Figma MCP responses, original reference renders, provenance manifests and device screenshots are retained locally for verification and are excluded from the public repository. The application artwork and fonts listed below are included as runtime resources.

- High fidelity responses: `docs/figma-context/<Screen>.txt`.
- Original screen renders, for review only: `docs/figma-reference/<Screen>.png`. These must never be used as application screens or backgrounds.
- All 86 returned image/vector assets downloaded unchanged into `app/src/main/assets/figma/`.
- `docs/figma-context/assets.json`: screen, response variable, local filename and source URL provenance.
- `docs/figma-context/asset-usage.json`: every asset occurrence and the nearest named Figma node/tag, preserving the returned layout geometry.
- `docs/figma-context/asset-metadata.json`: intrinsic width/height, byte size and SHA-256 for every downloaded file.
- Poppins Regular and Bold are bundled in `app/src/main/res/font/`. Their SIL Open Font License is bundled in `app/src/main/assets/licenses/Poppins-OFL.txt`. Source: [Google Fonts Poppins](https://github.com/google/fonts/tree/main/ofl/poppins).

## Screens

| Screen | Node | Canvas | Evidence |
| --- | --- | --- | --- |
| Splash | 1:5 | 375 × 812 | Full context and render |
| Onboarding 1 | 1:124 | 375 × 812 | Full context and render |
| Onboarding 2 | 1:379 | 375 × 812 | Full context and render |
| Onboarding 3 | 1:441 | 375 × 812 | Full context and render |
| Login | 1:469 | 375 × 812 | Full context and render |
| Landing | 1:514 | 375 × 937 | Full context and render |
| Create Account | 1:622 | 375 × 1015 | Full context and render |
| Side Menu | 1:150 | 375 × 812 | Full context and render |
| Schedule Game | 1:248 | 375 × 812 | Full context and render |
| Success | 1:302 | Not retrieved | Figma Starter MCP quota stopped this request |

The exact quota error is saved locally in `docs/figma-context/Success.txt`. Do not claim that the Success screen was inspected or visually matched.

## Tokens

| Role | Value |
| --- | --- |
| Primary | #FA5075 |
| Gradient light | #FF6480 |
| Gradient dark | #F22E63 |
| Main text | #030303 |
| Surface | #FFFFFF |
| Status illustration color | #F84570 |
| Gold player | #F4C73E |
| Heading font | Poppins Bold |
| Body font | Poppins Regular |
| Onboarding heading | 24, two lines, centered |
| Form / screen heading | 22 Bold |
| Side menu | 16 Regular; user name 16 Bold |
| Onboarding body and primary CTA | 14; CTA Bold |
| Form fields / supporting copy | 12 Regular |
| Landing card heading | 12 Bold |
| Landing card text | 10 Regular, tracking -0.3 |
| Date section labels FROM/TO | 8 Regular, primary |
| Pill button | 210 × 58, radius 100 |
| Landing card | 315 × 169, radius 10 |
| Form underline | 1 px, primary |

The exact gradients vary by slot: splash 114.789°, buttons 164.560°, cards 151.786° in CSS coordinates. Preserve the color endpoints and visual direction when expressing the brush in Compose.

## Primary artwork map

Positions below are in the original screen coordinate space and include the 44 px iPhone status-bar area. Native Android insets should replace the mock status/home bars. Keep the visual relationships within the content area; allow scrolling for tall forms and cards.

| Local file | Node | Slot x,y,w,h | Meaning / use |
| --- | --- | --- | --- |
| splash_group7.svg | 1:7, 1:35 | 100.09,273.09,173.905,102.654 | Complete white logo + GAAMETIME + tagline; the Figma tree contains an identical duplicate |
| splash_graphics.svg | 1:63 | -26,642,431,285 | Bottom abstract decorations, clipped by screen |
| onboarding1_image.png | 1:149 | 48,164,327,232 | Gamer celebrating |
| onboarding2_image.png | 1:388 | 27,163,348,233 | Two gamers; decorative calendar/clock are separate layers |
| onboarding2_group45.svg | 1:389 | 56,190.98; natural 35.09 × 44.8363 | Calendar overlay, rotation -37.12°; rotated bounds about 55.05 × 56.92 |
| onboarding2_group46.svg | 1:407 | 193.01,178.96; natural 23.6311 × 26.2306 | Stopwatch overlay, rotation +38.14°; rotated bounds about 34.76 × 35.24 |
| onboarding3_image.png | 1:445 | 30,144,316,294 | Gamers with voice/chat bubbles |
| login_image.png | 1:471 | 0,95,323,205 | Gamer and monitor |
| createaccount_image.png | 1:625 | 27,65,321,247 | Seated gamer |
| landing_image1.png | 1:547 | 211,159,112,107 | Schedule card figure + clock |
| landing_vector.svg | 1:545 | 207,254.02,130.852,14.2747 | Schedule card shadow |
| landing_vector1.svg | 1:548 | 263.75,135.75,28.5,5.5 | Schedule card cloud |
| landing_vector2.svg | 1:549 | approx 260.1,159.1; natural 6.29316 square | Schedule card dot, rotation -80.85° |
| landing_image.png | 1:532 | 198,360,121,91 | Statistics card gamers |
| landing_ellipse2.svg | 1:533 | approx 302,332; natural 18.8334 × 18.8724 | Statistics cyan ring, rotation +98.02° |
| landing_ellipse3.svg | 1:536 | 240.5,354.5,9.41282,9.39357 | Statistics small ring |
| landing_image2.png | 1:557 | 141,509,202,114 | Discover telescope illustration |
| landing_image3.png | 1:565 | 205,696,131,103 | Chat illustration |
| landing_ellipse1.png | 1:523 | 264,58,22,22 | Reference avatar placeholder, intrinsic export 44 × 44 |
| sidemenu_ellipse3.png | 1:155 | 62.33,117.33,51.33,51.33 | Reference avatar placeholder, intrinsic export 103 × 103 |

Illustrations are original assets, never substitutes or redraws. Do not stretch their aspect ratios. PNG alpha transparency must remain intact.

## Layout and interaction notes

- Onboarding: heading starts near y=431, with 224 px text width. Body starts near y=539; skip near y=665; slider dots are 10 px at x=163,183,203 and y=757. Third slide uses a pill CTA at x=83,y=661. Swiping should move slides; skip should complete onboarding.
- Login: content x=52, field width 263. Heading y=341; supporting copy y=386; underline y=470 and y=520; CTA x=83,y=573. Forgot password is right-aligned under the password. Social icons are the original historical Google+ and Facebook graphics, 35 × 34, at x=145 and 195 near y=679.
- Registration is a tall, scrollable page. Heading x=52,y=338. Text fields/underlines are at y=456/482,506/532,556/582,606/632,656/682,706/732. Split country code and phone, field widths 35 and 210 with gap 18. CTA y=776; sign-in footer y=940.
- Landing has top header at y≈57, four 315 × 169 cards with 10 px gaps and left edge near x=30. Card y values: 116,295,472.57,653. Card headings are inset 24 px horizontally and 30 px vertically. Bottom navigation begins at y=828 and includes an elevated central schedule circle; use the local original taskbar/base assets.
- Side menu is a full screen white view. Avatar x=60,y=115,56 × 56. User name x=127,y=115. Menu text x=100, first row y=224, row pitch about 58.4. Pink icons are about 16–20 px, aligned at x≈60. Logout is near y=700.
- Schedule form: horizontal margin 30. Header y=110; underlined name/category/prize near y=173/223/267. FROM and TO rows at y=340/398, date/time controls below. Description underline y=570; notification reminder row y=625. Publish x=84,y=687.
- Exact field and icon geometry is preserved in the local-only context text and asset usage manifest. Use each native interactive control with accessible touch targets while preserving the drawn asset size.

## Design limitations and deliberate platform adaptations

1. Figma provided only the nine inspected screens above plus the inaccessible Success node. Profiles, statistics, chat details, discovery contents, edit states, error states and backend behavior cannot be inferred as pixel-matched screens from this copy.
2. The mockups are for iPhone X. Android should show real system status/navigation bars and use actual screen insets; the downloaded mock battery/Wi-Fi/mobile-signal artwork is retained solely as source evidence.
3. The two reference avatars render as a black-and-white checkerboard in Figma. User avatars are dynamic data, so user-selected/profile images should replace the mock photo at runtime.
4. The source has duplicate logo/status-bar layers on Splash and duplicate category/dropdown/line layers on Schedule. A single visible instance reproduces the appearance.
5. Create Account includes a plain gray 14 × 10 rectangle at x=269,y=115 over the illustration. It is present in the source render and is not part of the downloaded PNG. Treat it as a source-design artifact, explicitly document any choice to omit it.
6. Poppins covers Latin and Devanagari; Russian localized copy should use a Cyrillic-capable Android fallback. English reference text uses bundled Poppins.
7. Existing static artwork is low-resolution (1× for most illustrations). Display at its intended size; do not claim higher-resolution source art was supplied.
