![Luna's Cosmetics](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/banner.png)

# Luna's Cosmetics

**A free wardrobe of living pets, hats and wings — plus a Cherry Cat theme for your whole game.**

No shop. No coins. No "supporter only". Every single cosmetic is unlocked for everyone, forever.

I really wanted a cat that sits on my head... but every cute cosmetic I found cost money. So now there's a wardrobe where **everything is free, for everyone** — and it has *so* many cats. 🐱🌸

---

## 🐾 Living pets that sit on your head

![Meet the pets](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/gallery/00_meet_the_pets.png)

**10 cats + Mini Moosh**, and they're not statues — they're *alive*:

- 👀 **Blink** (sometimes twice), **look around**, and tilt their heads when they're curious
- 👂 **Twitch their ears**, and flatten them when you get hurt or sneak
- 🐾 **Knead** ("make biscuits") on your head every now and then
- 😴 **Fall asleep** if you stand still for a while — and **purr** — then wake up with a stretch
- 🎐 **Swish their tails**, faster when you run
- 🐷 **Mini Moosh** bounces, flops its ears, wiggles its tail and its little sprout sways in the breeze

| Pet | |
|---|---|
| **Snowball** | fluffy white cat with pink ears |
| **Luna** | black cat with a glowing golden moon |
| **Stargazer** | covered in twinkling, *glowing* stars |
| **Sakura** | cherry-blossom pink with flowers in her fur |
| **Marmalade** | stripy ginger troublemaker |
| **Tuxedo** · **Calico** · **Siamese** · **Smokey** · **Cocoa** | classic coats, all hand-painted |
| **Mini Moosh** | a tiny pink moo-pig with a sprout on top. Boing! |

Pick where your pet rides: **lying on your head**, **sitting on your head**, or on your **left / right shoulder**.

![Snowball on head](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/gallery/01_snowball_on_head.png)

## 🎀 Hats & back pieces

- **Sakura Crown** — a ring of cherry blossoms
- **Kitty Ears** — pink, midnight or snow, and they twitch!
- **Star Halo** — floats, bobs and slowly spins (and glows)
- **Petal Wings** — cherry-blossom fairy wings that flutter harder when you run or fall

Wear one pet, one hat and one back piece at the same time.

![Stargazer and the Star Halo](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/gallery/04_stargazer_star_halo.png)

## 👗 The wardrobe

Click the **little cherry cat** next to *Options* (title screen or pause menu), or press **K**.

- A live 3D preview of *you* wearing your outfit — drag to spin
- Every cosmetic is a living little card; click to wear, click again to take off
- Everything is free. There is nothing to buy. Ever.

![Wardrobe](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/gallery/07_wardrobe.png)

## 🌸 Cherry Cat theme (optional)

Turn your whole game cherry-blossom pink:

- Pink buttons, sliders, text boxes, tabs and scrollbars
- Pink inventories, hotbar, **pink hearts** and XP bar
- Cherry petals drifting behind every menu (they dodge your mouse!)
- A **paw cursor**, and little **cat ears pop up** over whatever button you hover
- Buttons go *mew* when you click (can be turned off)
- A sleepy cat on the title screen logo — poke it!

It switches on and off instantly in *Settings* — no resource reload.

![Title screen](https://raw.githubusercontent.com/AttackStudios/lunascosmetics/main/media/gallery/10_cherry_cat_title_screen.png)

## ✏️ Make your own cosmetics

Build anything in **[Blockbench](https://www.blockbench.net/)** (free) and drop the `.bbmodel` into `config/lunascosmetics/custom/`:

| File name | Goes to |
|---|---|
| `hat_anything.bbmodel` | Hats |
| `pet_anything.bbmodel` | Pets |
| `back_anything.bbmodel` | Back |

- It appears in the **My Own** tab instantly, and updates live every time you save
- Cubes *and* meshes, multiple textures, and an **`idle` animation** that loops forever
- Anything in a group named `reference` is hidden in game, so you can build around a stand-in head
- A how-to and an example bow are created in that folder the first time you launch

## 👯 Friends see it too

Everyone who has Luna's Cosmetics sees each other's pets, hats and wings — **including custom Blockbench cosmetics**, which are sent to them automatically.

- **Server has the mod too?** It just works. Put the same jar in the server's `mods` folder (it's a harmless no-op for players without it).
- **Server doesn't?** Run the tiny optional relay from the GitHub repo and paste its address into *Settings → Sync relay*.

## 🔄 Automatic updates

Like Essential, Luna's Cosmetics keeps itself up to date: on launch it checks [the GitHub releases](https://github.com/AttackStudios/lunascosmetics/releases), downloads the new jar for your loader and Minecraft version, verifies its SHA-256 checksum, and swaps it in **after you close the game**. You'll get a little toast when an update is ready.

Don't want that? Turn off **Settings → Auto-update**.

## 📦 Install

| Loader | Minecraft | Needs |
|---|---|---|
| **Fabric** | 1.21.11 | Fabric Loader (Fabric API is bundled) |
| **NeoForge** | 1.21.1 | NeoForge 21.1.x |

Works client-side on any server. Installing it on the server as well enables automatic syncing between players.

## ❓ FAQ

**Is it really free?** Yes. Every cosmetic, for everyone. There's no store and there never will be.

**Can other players see my cat without the mod?** No — cosmetics are drawn by the mod, so friends need it installed too.

**Will it work with other cosmetic mods?** It only adds its own layer, so it should play nicely with most.

**Can I suggest a cosmetic?** Yes please!! I'd love ideas — open an issue on [GitHub](https://github.com/AttackStudios/lunascosmetics/issues).

---

Open source under the MIT license · [Source code](https://github.com/AttackStudios/lunascosmetics) · [Issues](https://github.com/AttackStudios/lunascosmetics/issues)
