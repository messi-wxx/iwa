"""Extract icons and colors from iwa_full_02_home.png design."""
from pathlib import Path
from PIL import Image, ImageDraw

ASSETS = Path(r"C:\Users\Administrator\.cursor\projects\e-TestCursor-iwa\assets")
RES = Path(r"E:\TestCursor\iwa\app\src\main\res")
SRC = ASSETS / "iwa_full_02_home.png"

src = Image.open(SRC).convert("RGB")
w, h = src.size
print("size", w, h)
print("bg", src.getpixel((40, 120)))

# Detect white cards
small = src.resize((w // 4, h // 4))
sw, sh = small.size
mp = [[0] * sw for _ in range(sh)]
sp = small.load()
for y in range(sh):
    for x in range(sw):
        r, g, b = sp[x, y]
        if r > 245 and g > 245 and b > 245:
            mp[y][x] = 1

visited = [[False] * sw for _ in range(sh)]
boxes = []
for y in range(sh):
    for x in range(sw):
        if mp[y][x] == 0 or visited[y][x]:
            continue
        stack = [(x, y)]
        visited[y][x] = True
        minx = maxx = x
        miny = maxy = y
        cnt = 0
        while stack:
            cx, cy = stack.pop()
            cnt += 1
            minx = min(minx, cx)
            maxx = max(maxx, cx)
            miny = min(miny, cy)
            maxy = max(maxy, cy)
            for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                if 0 <= nx < sw and 0 <= ny < sh and not visited[ny][nx] and mp[ny][nx]:
                    visited[ny][nx] = True
                    stack.append((nx, ny))
        if cnt > 800:
            boxes.append((minx * 4, miny * 4, (maxx + 1) * 4, (maxy + 1) * 4))

boxes.sort(key=lambda b: (b[1], b[0]))
print("cards", len(boxes))
for i, b in enumerate(boxes):
    print(i, b)

dbg = src.copy()
d = ImageDraw.Draw(dbg)
for b in boxes:
    d.rectangle(b, outline=(255, 0, 0), width=3)
dbg.save(ASSETS / "home_card_detect.png")


def to_line_icon(rgb_img: Image.Image, teal=(11, 110, 122)) -> Image.Image:
    """White/light -> transparent; dark/teal strokes -> solid teal."""
    rgba = rgb_img.convert("RGBA")
    px = rgba.load()
    ww, hh = rgba.size
    for y in range(hh):
        for x in range(ww):
            r, g, b, a = px[x, y]
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            # near white / card bg
            if lum > 230:
                px[x, y] = (0, 0, 0, 0)
            else:
                # keep stroke; boost teal
                alpha = int(min(255, (230 - lum) * 2.2))
                if alpha < 40:
                    px[x, y] = (0, 0, 0, 0)
                else:
                    px[x, y] = (teal[0], teal[1], teal[2], alpha)
    return rgba


def tight_square(img: Image.Image, pad: int = 6) -> Image.Image:
    bbox = img.getbbox()
    if not bbox:
        return img
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(img.width, r + pad)
    b = min(img.height, b + pad)
    c = img.crop((l, t, r, b))
    side = max(c.width, c.height)
    out = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    out.paste(c, ((side - c.width) // 2, (side - c.height) // 2))
    return out


names = [
    "ic_home_meter",
    "ic_home_replace",
    "ic_home_urge",
    "ic_home_scene",
    "ic_home_diagnose",
    "ic_home_calibrate",
    "ic_home_dma",
    "ic_home_iot",
]

# Take top 8 cards
cards = boxes[:8]
assert len(cards) >= 8, f"need 8 cards, got {len(cards)}"

densities = {
    "drawable-mdpi": 48,
    "drawable-hdpi": 72,
    "drawable-xhdpi": 96,
    "drawable-xxhdpi": 144,
    "drawable-xxxhdpi": 192,
}

preview_dir = ASSETS / "home_icon_crops"
preview_dir.mkdir(exist_ok=True)

for i, name in enumerate(names):
    l, t, r, b = cards[i]
    cw, ch = r - l, b - t
    # icon sits in upper ~55% of card, horizontally centered
    icon_box = (
        l + int(cw * 0.18),
        t + int(ch * 0.08),
        r - int(cw * 0.18),
        t + int(ch * 0.55),
    )
    crop = src.crop(icon_box)
    icon = tight_square(to_line_icon(crop), pad=4)
    icon.save(preview_dir / f"{name}.png")
    for folder, size in densities.items():
        d = RES / folder
        d.mkdir(parents=True, exist_ok=True)
        icon.resize((size, size), Image.Resampling.LANCZOS).save(d / f"{name}.png", optimize=True)
    print("icon", name, icon_box)

# Settings gear: top-right area
# Approximate: right of title row
gear_box = (int(w * 0.82), int(h * 0.055), int(w * 0.96), int(h * 0.12))
gear = src.crop(gear_box)
gear_icon = tight_square(to_line_icon(gear, teal=(18, 52, 77)), pad=2)
gear_icon.save(preview_dir / "ic_settings.png")
for folder, size in densities.items():
    sz = {48: 36, 72: 54, 96: 72, 144: 108, 192: 144}[size]
    (RES / folder).mkdir(parents=True, exist_ok=True)
    gear_icon.resize((sz, sz), Image.Resampling.LANCZOS).save(
        RES / folder / "ic_settings.png", optimize=True
    )
print("gear", gear_box)

# Remove conflicting vectors
for name in names + ["ic_settings"]:
    xml = RES / "drawable" / f"{name}.xml"
    if xml.exists():
        xml.unlink()
        print("removed", xml.name)

print("done")
