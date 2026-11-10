#!/usr/bin/env python3
"""Generate Huni palette-mode launcher assets from the transparent bird mark.

Install Pillow with `python3 -m pip install Pillow`, then run
`python3 scripts/generate_launcher_icons.py` from any working directory.

Only `ic_launcher_<palette>` resources are generated here. The stable default
`ic_launcher` adaptive icon remains the original transparent-background mark.
Palette-mode foregrounds keep the bird's original colors and fit inside the
centered 66% adaptive-icon safe zone; backgrounds are darkened until they
provide at least 3:1 WCAG contrast against the bird's dominant visible color.
"""

from collections import defaultdict
from pathlib import Path

from PIL import Image, ImageDraw

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

# (palette id, Appearance accent color). The starting background is the
# palette's actual swatch color; contrast-safe values are calculated below.
PALETTES = [
    ("amber", "#FFB84D"),
    ("violet", "#B79CFF"),
    ("green", "#6FD98B"),
    ("maroon", "#E5707E"),
    ("ocean", "#6CB8FF"),
    ("teal", "#4FD6C8"),
    ("rose", "#FF8FB8"),
    ("orange", "#FF9A5C"),
    ("indigo", "#8C9EFF"),
    ("slate", "#CBD5E1"),
]


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.removeprefix("#")
    return tuple(int(value[index : index + 2], 16) for index in (0, 2, 4))


def hex_rgb(color: tuple[int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*color)


def dominant_visible_color() -> tuple[int, int, int]:
    """Return the alpha-weighted dominant 5-bit RGB cluster center."""
    source = Image.open(SOURCE).convert("RGBA")
    counts: dict[tuple[int, int, int], float] = defaultdict(float)
    pixels = source.tobytes()
    for offset in range(0, len(pixels), 4):
        red, green, blue, alpha = pixels[offset : offset + 4]
        if alpha >= 32:
            counts[(red // 8, green // 8, blue // 8)] += alpha / 255
    if not counts:
        raise ValueError(f"The bird mark has no visible pixels: {SOURCE}")
    channel_bins = max(counts, key=counts.get)
    return tuple(min(255, channel * 8 + 4) for channel in channel_bins)


def luminance(color: tuple[int, int, int]) -> float:
    def linear(channel: int) -> float:
        value = channel / 255
        return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4

    return (
        0.2126 * linear(color[0])
        + 0.7152 * linear(color[1])
        + 0.0722 * linear(color[2])
    )


def contrast_ratio(first: tuple[int, int, int], second: tuple[int, int, int]) -> float:
    high, low = sorted((luminance(first), luminance(second)), reverse=True)
    return (high + 0.05) / (low + 0.05)


def contrast_safe_background(
    palette_color: str,
    bird_color: tuple[int, int, int],
) -> tuple[str, int, float]:
    background = rgb(palette_color)
    steps = 0
    while contrast_ratio(bird_color, background) < 3.0:
        background = tuple(round(channel * 0.9) for channel in background)
        steps += 1
        if steps > 20:
            raise ValueError(f"Could not reach 3:1 contrast from {palette_color}")
    return hex_rgb(background), steps, contrast_ratio(bird_color, background)


def bird_layer(size: int, monochrome: bool = False) -> Image.Image:
    source = Image.open(SOURCE).convert("RGBA")
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"The bird mark has no visible pixels: {SOURCE}")
    source = source.crop(bounds)

    safe_px = round(size * 0.66)
    scale = min(safe_px / source.width, safe_px / source.height)
    target = (
        max(1, round(source.width * scale)),
        max(1, round(source.height * scale)),
    )
    source = source.resize(target, Image.Resampling.LANCZOS)
    if monochrome:
        mark = Image.new("RGBA", target, (255, 255, 255, 0))
        mark.putalpha(source.getchannel("A"))
    else:
        # Preserve the user-supplied bird's RGB colors and transparent alpha.
        mark = source

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

    bird_color = dominant_visible_color()
    print(f"Dominant visible bird color: {hex_rgb(bird_color)}")
    for name, palette_color in PALETTES:
        background, steps, ratio = contrast_safe_background(palette_color, bird_color)
        print(f"{name}: {palette_color} -> {background} ({steps} darkening steps, {ratio:.2f}:1)")
        write_background(name, background)
        for density, size in DENSITIES.items():
            directory = RES / f"mipmap-{density}"
            directory.mkdir(parents=True, exist_ok=True)
            foreground = bird_layer(size)
            foreground.save(directory / f"launcher_icon_{name}_foreground.png")
            bird_layer(size, monochrome=True).save(directory / f"launcher_icon_{name}_monochrome.png")

            # Pre-adaptive launcher fallback, with a round drawable when requested.
            for suffix, round_icon in (("", False), ("_round", True)):
                canvas = Image.new("RGBA", (size, size), (*rgb(background), 255))
                if round_icon:
                    mask = Image.new("L", (size, size), 0)
                    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
                    canvas.putalpha(mask)
                canvas.alpha_composite(foreground)
                canvas.save(directory / f"ic_launcher_{name}{suffix}.png")

        for round_icon in (False, True):
            write_adaptive_xml(name, round_icon, api33=False)
            write_adaptive_xml(name, round_icon, api33=True)

    # Remove stale anydpi drafts; adaptive icons use explicit API 26/33 folders.
    for name, _ in PALETTES:
        for suffix in ("", "_round"):
            stale = RES / "mipmap-anydpi" / f"ic_launcher_{name}{suffix}.xml"
            stale.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
