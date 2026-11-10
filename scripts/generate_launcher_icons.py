#!/usr/bin/env python3
"""Generate Huni's per-palette launcher icons from the transparent bird mark.

Install Pillow once with `python3 -m pip install Pillow`, then run from any
working directory with `python3 scripts/generate_launcher_icons.py`.

The source PNG remains transparent. Palette foregrounds preserve its shading
and alpha, and are fitted inside the centered 66% adaptive-icon safe area.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCE = ROOT / "scripts/assets/huni_bird_transparent.png"
DENSITIES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

# id, background (palette darkBg), foreground (palette accentDark)
PALETTES = [
    ("amber", "#16121C", "#FFB84D"),
    ("violet", "#14111F", "#B79CFF"),
    ("green", "#0F1A14", "#6FD98B"),
    ("maroon", "#1C1114", "#E5707E"),
    ("ocean", "#0E1620", "#6CB8FF"),
    ("teal", "#0D1918", "#4FD6C8"),
    ("rose", "#1D1119", "#FF8FB8"),
    ("orange", "#1C1410", "#FF9A5C"),
    ("indigo", "#11131F", "#8C9EFF"),
    ("slate", "#111317", "#CBD5E1"),
]


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.removeprefix("#")
    return tuple(int(value[index : index + 2], 16) for index in (0, 2, 4))


def tinted_layer(size: int, accent: str, monochrome: bool = False) -> Image.Image:
    source = Image.open(SOURCE).convert("RGBA")
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"The bird mark has no visible pixels: {SOURCE}")
    source = source.crop(bounds)
    alpha = source.getchannel("A")
    luminance = ImageOps.grayscale(source)
    safe_px = round(size * 0.66)
    scale = min(safe_px / source.width, safe_px / source.height)
    target = (max(1, round(source.width * scale)), max(1, round(source.height * scale)))
    alpha = alpha.resize(target, Image.Resampling.LANCZOS)

    if monochrome:
        mark = Image.new("RGBA", target, (255, 255, 255, 0))
        mark.putalpha(alpha)
    else:
        luminance = luminance.resize(target, Image.Resampling.LANCZOS)
        base = rgb(accent)
        pixels = []
        for value, opacity in zip(luminance.tobytes(), alpha.tobytes()):
            # Map the supplied silver facets to shaded accent tones while
            # retaining the source's transparent edges and fine detail.
            factor = 0.28 + 0.72 * (value / 255)
            pixels.append((*[min(255, round(channel * factor)) for channel in base], opacity))
        mark = Image.new("RGBA", target)
        mark.putdata(pixels)

    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    layer.alpha_composite(mark, ((size - target[0]) // 2, (size - target[1]) // 2))
    return layer


def write_background(name: str, color: str) -> None:
    path = RES / f"drawable/launcher_icon_{name}_background.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        f'''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{color}" />
</shape>
''',
        encoding="utf-8",
    )


def write_adaptive_xml(name: str, round_icon: bool, api33: bool) -> None:
    suffix = "_round" if round_icon else ""
    monochrome = (
        f'\n    <monochrome android:drawable="@mipmap/launcher_icon_{name}_monochrome" />'
        if api33
        else ""
    )
    path = RES / f"mipmap-anydpi-v{'33' if api33 else '26'}/ic_launcher_{name}{suffix}.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/launcher_icon_{name}_background" />
    <foreground android:drawable="@mipmap/launcher_icon_{name}_foreground" />{monochrome}
</adaptive-icon>
''',
        encoding="utf-8",
    )


def main() -> None:
    if not SOURCE.is_file():
        raise FileNotFoundError(f"Missing transparent bird mark: {SOURCE}")

    for name, background, accent in PALETTES:
        write_background(name, background)
        for density, size in DENSITIES.items():
            directory = RES / f"mipmap-{density}"
            directory.mkdir(parents=True, exist_ok=True)
            # Adaptive foreground and Android 13+ monochrome layers.
            tinted_layer(size, accent).save(directory / f"launcher_icon_{name}_foreground.png")
            tinted_layer(size, accent, monochrome=True).save(directory / f"launcher_icon_{name}_monochrome.png")

            # Pre-adaptive launcher fallback, with a round drawable where requested.
            for suffix, round_icon in (("", False), ("_round", True)):
                canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
                mask = Image.new("L", (size, size), 255)
                if round_icon:
                    mask = Image.new("L", (size, size), 0)
                    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
                bg = Image.new("RGBA", (size, size), (*rgb(background), 255))
                canvas.alpha_composite(bg)
                canvas.putalpha(mask)
                canvas.alpha_composite(tinted_layer(size, accent))
                canvas.save(directory / f"ic_launcher_{name}{suffix}.png")

        for round_icon in (False, True):
            write_adaptive_xml(name, round_icon, api33=False)
            write_adaptive_xml(name, round_icon, api33=True)

    # Remove early generator outputs from development runs; these are superseded
    # by density-aware PNG fallbacks and are not part of the generated set.
    for name, _, _ in PALETTES:
        for suffix in ("", "_round"):
            stale = RES / "mipmap-anydpi" / f"ic_launcher_{name}{suffix}.xml"
            stale.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
