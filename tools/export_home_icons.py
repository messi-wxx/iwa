"""Crop design icons to drawable PNGs (teal line art on transparent)."""
from pathlib import Path
from PIL import Image

assets = Path(r"C:\Users\Administrator\.cursor\projects\e-TestCursor-iwa\assets")
out = Path(r"E:\TestCursor\iwa\app\src\main\res\drawable-xxhdpi")
out.mkdir(parents=True, exist_ok=True)


def to_teal_line(img: Image.Image, teal=(11, 110, 122, 255)) -> Image.Image:
    """Keep dark strokes as teal, make light/white transparent."""
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                px[x, y] = (0, 0, 0, 0)
                continue
            # brightness: light pixels -> transparent
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum > 210:
                px[x, y] = (0, 0, 0, 0)
            else:
                # map darkness to alpha for soft edges
                alpha = int(min(255, max(0, (210 - lum) * 1.4)))
                px[x, y] = (teal[0], teal[1], teal[2], alpha)
    return rgba


def tight_crop(img: Image.Image, pad: int = 8) -> Image.Image:
    bbox = img.getbbox()
    if not bbox:
        return img
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(img.width, r + pad)
    b = min(img.height, b + pad)
    cropped = img.crop((l, t, r, b))
    # pad to square
    side = max(cropped.width, cropped.height)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(cropped, ((side - cropped.width) // 2, (side - cropped.height) // 2))
    return square


# Settings outline -> navy
settings_src = Image.open(assets / "iwa_ic_settings_outline.png")
settings = to_teal_line(settings_src, teal=(18, 52, 77, 255))
settings = tight_crop(settings, pad=12)
settings.resize((72, 72), Image.Resampling.LANCZOS).save(out / "ic_settings.png", optimize=True)
print("saved ic_settings.png", settings.size)

# Icon sheet 2x4
sheet = Image.open(assets / "iwa_home_icons_sheet.png").convert("RGBA")
sw, sh = sheet.size
cell_w, cell_h = sw // 2, sh // 4
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
for i, name in enumerate(names):
    row, col = divmod(i, 2)
    # sheet is 2 cols x 4 rows
    col, row = i % 2, i // 2
    cell = sheet.crop((col * cell_w, row * cell_h, (col + 1) * cell_w, (row + 1) * cell_h))
    icon = to_teal_line(cell)
    icon = tight_crop(icon, pad=10)
    icon.resize((120, 120), Image.Resampling.LANCZOS).save(out / f"{name}.png", optimize=True)
    print("saved", name)

print("done")
