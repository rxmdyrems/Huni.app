from pathlib import Path

out = Path(__file__).parents[1] / "app/src/main/res/drawable"
palette = [
    ("amber", "#F2B84B", "#8C5A25"), ("violet", "#A98BFF", "#5634A4"),
    ("green", "#6FD98B", "#236B3A"), ("maroon", "#E5707E", "#8C2738"),
    ("ocean", "#6CB8FF", "#245B9A"), ("teal", "#4FD6C8", "#176B65"),
    ("rose", "#FF8FB8", "#A83D69"), ("orange", "#FF9A5C", "#A94D20"),
    ("indigo", "#8C9EFF", "#384793"), ("slate", "#CBD5E1", "#4A5568"),
    ("mint", "#8EE6C2", "#287D61"), ("coral", "#FF9B85", "#994331"),
]
for i, (name, body, accent) in enumerate(palette):
    extra = "<path android:fillColor=\"%s\" android:pathData=\"M38,27 l5,-10 l5,11\"/>" % accent if i % 3 == 0 else ""
    cheek = "<path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M28,45 a3,3 0,1 0,6,0 a3,3 0,1 0,-6,0 M46,45 a3,3 0,1 0,6,0 a3,3 0,1 0,-6,0\"/>"
    if i % 3 == 1:
        cheek = "<path android:fillColor=\"#FFFFC4D0\" android:pathData=\"M23,49 a5,3 0,1 0,10,0 a5,3 0,1 0,-10,0 M47,49 a5,3 0,1 0,10,0 a5,3 0,1 0,-10,0\"/>" + "<path android:fillColor=\"#FFFFFFFF\" android:pathData=\"M29,44 a2,2 0,1 0,4,0 a2,2 0,1 0,-4,0 M47,44 a2,2 0,1 0,4,0 a2,2 0,1 0,-4,0\"/>"
    if i % 3 == 2:
        extra = "<path android:fillColor=\"%s\" android:pathData=\"M24,30 q8,-12 16,0 q8,-12 16,0\"/>" % accent
    xml = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="96dp" android:height="96dp" android:viewportWidth="80" android:viewportHeight="80">
    <path android:fillColor="{body}" android:pathData="M40,10 A30,30 0,1 0,40,70 A30,30 0,1 0,40,10"/>
    {extra}
    {cheek}
    <path android:fillColor="{accent}" android:pathData="M35,54 q5,5 10,0 q-5,8 -10,0"/>
</vector>
'''
    (out / f"avatar_{name}.xml").write_text(xml)
