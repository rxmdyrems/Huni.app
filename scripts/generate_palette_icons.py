#!/usr/bin/env python3
"""Generate Huni adaptive launcher icons for all theme palettes.

Run from the repository root with: python3 scripts/generate_palette_icons.py
The paths are the cropped Huni bird logo specified in the design checklist;
coordinates are scaled to fit inside Android's 66% adaptive-icon safe zone.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
PALETTES = [
    ("amber", "Amber", "#16121C", "#FFB84D"),
    ("violet", "Violet", "#14111F", "#B79CFF"),
    ("green", "Green", "#0F1A14", "#6FD98B"),
    ("maroon", "Maroon", "#1C1114", "#E5707E"),
    ("ocean", "Ocean", "#0E1620", "#6CB8FF"),
    ("teal", "Teal", "#0D1918", "#4FD6C8"),
    ("rose", "Rose", "#1D1119", "#FF8FB8"),
    ("orange", "Orange", "#1C1410", "#FF9A5C"),
    ("indigo", "Indigo", "#11131F", "#8C9EFF"),
    ("slate", "Slate", "#111317", "#CBD5E1"),
]
HEAD = "M118 150 L136 118 L162 96 L252 58 L214 96 L262 80 L226 108 L264 114 L206 124 L176 146 L142 152 Z"
NECK = "M142 152 L176 146 L226 168 L212 206 L168 246 Q184 200 142 152 Z"
EYE = "M146 130 L174 120 L160 136 Z"
# x' = .32*x - 6.8 and y' = .32*y + 6.8; source crop [100,280]x[40,255]
TRANSFORM = '<group android:scaleX="0.32" android:scaleY="0.32" android:translateX="-6.8" android:translateY="6.8" />'

def vector(name: str, paths: str) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="0.32" android:scaleY="0.32" android:translateX="-6.8" android:translateY="6.8">
{paths}
    </group>
</vector>
'''

colors = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
for key, _label, background, _accent in PALETTES:
    colors.append(f'    <color name="huni_icon_{key}_background">{background}</color>')
colors.append('</resources>')
(ROOT / "values/huni_icon_palette_colors.xml").write_text("\n".join(colors) + "\n")

for key, _label, background, accent in PALETTES:
    foreground = f'''        <path android:fillColor="{accent}" android:fillAlpha="0.9"
            android:strokeColor="#FFF1CF" android:strokeAlpha="0.8" android:strokeWidth="2"
            android:pathData="{HEAD}" />
        <path android:fillColor="{accent}" android:fillAlpha="0.55"
            android:strokeColor="#FFF1CF" android:strokeAlpha="0.8" android:strokeWidth="2"
            android:pathData="{NECK}" />
        <path android:fillColor="{background}" android:pathData="{EYE}" />'''
    (ROOT / f"drawable/ic_huni_{key}_foreground.xml").write_text(vector(key, foreground))
    monochrome = f'''        <path android:fillColor="#FFFFFFFF" android:pathData="{HEAD}" />
        <path android:fillColor="#FFFFFFFF" android:pathData="{NECK}" />'''
    (ROOT / f"drawable/ic_huni_{key}_monochrome.xml").write_text(vector(key, monochrome))

    adaptive = ROOT / "mipmap-anydpi-v26" / f"ic_launcher_{key}.xml"
    adaptive.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/huni_icon_{key}_background" />
    <foreground android:drawable="@drawable/ic_huni_{key}_foreground" />
</adaptive-icon>
''')
    adaptive_v33 = ROOT / "mipmap-anydpi-v33" / f"ic_launcher_{key}.xml"
    adaptive_v33.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/huni_icon_{key}_background" />
    <foreground android:drawable="@drawable/ic_huni_{key}_foreground" />
    <monochrome android:drawable="@drawable/ic_huni_{key}_monochrome" />
</adaptive-icon>
''')
print(f"Generated {len(PALETTES)} adaptive icons and monochrome layers in {ROOT}")
