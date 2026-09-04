from PIL import Image
from pathlib import Path

assets = Path(r"C:\Users\Administrator\.cursor\projects\e-TestCursor-iwa\assets")
res = Path(r"E:\TestCursor\iwa\app\src\main\res")
store = Path(r"E:\TestCursor\iwa\app\src\main\ic_launcher_store")
store.mkdir(parents=True, exist_ok=True)

full = Image.open(assets / "iwa_app_icon_1024.png").convert("RGBA")
fg = Image.open(assets / "iwa_app_icon_foreground_1024.png").convert("RGBA")

legacy = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

foreground = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}

for size, name in [(512, "ic_launcher_512.png"), (1024, "ic_launcher_1024.png")]:
    full.resize((size, size), Image.Resampling.LANCZOS).save(store / name, optimize=True)
    print("store", name)

for folder, size in legacy.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    img = full.resize((size, size), Image.Resampling.LANCZOS)
    img.save(d / "ic_launcher.png", optimize=True)
    img.save(d / "ic_launcher_round.png", optimize=True)
    print(folder, size)

for folder, size in foreground.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    img = fg.resize((size, size), Image.Resampling.LANCZOS)
    img.save(d / "ic_launcher_foreground.png", optimize=True)
    print(folder, size, "foreground")

print("done")
