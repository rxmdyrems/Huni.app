#!/usr/bin/env python3
"""Verify dynamic Mihon/Kotlin ABI symbols survive the minified release build."""

from __future__ import annotations

import re
import struct
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

EXPECTED_OWNER_PREFIX = "Lkotlin/collections/ArraysKt"
EXPECTED_METHOD = "joinToString$default"
EXPECTED_DESCRIPTOR = (
    "([Ljava/lang/Object;Ljava/lang/CharSequence;Ljava/lang/CharSequence;"
    "Ljava/lang/CharSequence;ILjava/lang/CharSequence;"
    "Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)Ljava/lang/String;"
)
HTTP_SOURCE = "Leu/kanade/tachiyomi/source/online/HttpSource;"
EXPECTED_FIELD = "headers$delegate"
EXPECTED_FIELD_TYPE = "Lkotlin/Lazy;"
EXPECTED_LAZY_IMPL = "Lkotlin/SynchronizedLazyImpl;"
APP_INFO = "Leu/kanade/tachiyomi/AppInfo;"
APP_INFO_INSTANCE = (APP_INFO, "INSTANCE", APP_INFO)
APP_INFO_VERSION_METHOD = (APP_INFO, "getVersionName", "()Ljava/lang/String;")
REQUIRED_RUNTIME_CLASSES = (
    "Lkotlin/LazyKt;",
    "Lkotlin/collections/MapsKt;",
    "Lkotlin/coroutines/intrinsics/IntrinsicsKt;",
    "Lkotlinx/serialization/SerializationStrategy;",
    "Lkotlinx/serialization/internal/ArrayListSerializer;",
    "Lkotlinx/serialization/json/okio/OkioStreamsKt;",
    "Lokhttp3/brotli/BrotliInterceptor;",
    "Lokhttp3/zstd/Zstd;",
    "Lorg/jsoup/Jsoup;",
    "Luy/kohesive/injekt/api/InjektFactory;",
)


def u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def uleb128(data: bytes, offset: int) -> tuple[int, int]:
    value = 0
    shift = 0
    while True:
        byte = data[offset]
        offset += 1
        value |= (byte & 0x7F) << shift
        if byte < 0x80:
            return value, offset
        shift += 7
        if shift > 35:
            raise ValueError("invalid ULEB128 value")


@dataclass(frozen=True)
class DexSymbols:
    methods: frozenset[tuple[str, str, str]]
    fields: frozenset[tuple[str, str, str]]
    classes: frozenset[str]


def parse_dex(data: bytes) -> DexSymbols:
    if not data.startswith(b"dex\n"):
        raise ValueError("not a DEX file")

    string_count, string_off = u32(data, 56), u32(data, 60)
    type_count, type_off = u32(data, 64), u32(data, 68)
    proto_count, proto_off = u32(data, 72), u32(data, 76)
    field_count, field_off = u32(data, 80), u32(data, 84)
    method_count, method_off = u32(data, 88), u32(data, 92)
    class_count, class_off = u32(data, 96), u32(data, 100)

    strings: list[str] = []
    for index in range(string_count):
        item_off = u32(data, string_off + index * 4)
        _, pos = uleb128(data, item_off)
        end = data.index(b"\0", pos)
        strings.append(data[pos:end].decode("utf-8", errors="replace"))

    type_descriptors = [strings[u32(data, type_off + index * 4)] for index in range(type_count)]

    prototypes: list[str] = []
    for index in range(proto_count):
        row = proto_off + index * 12
        return_type = u32(data, row + 4)
        parameters_off = u32(data, row + 8)
        parameters = ""
        if parameters_off:
            count = u32(data, parameters_off)
            parameters = "".join(
                type_descriptors[struct.unpack_from("<H", data, parameters_off + 4 + n * 2)[0]]
                for n in range(count)
            )
        prototypes.append(f"({parameters}){type_descriptors[return_type]}")

    fields: list[tuple[str, str, str]] = []
    for index in range(field_count):
        row = field_off + index * 8
        owner_idx, type_idx, name_idx = struct.unpack_from("<HHI", data, row)
        fields.append((type_descriptors[owner_idx], strings[name_idx], type_descriptors[type_idx]))

    methods: list[tuple[str, str, str]] = []
    for index in range(method_count):
        row = method_off + index * 8
        owner_idx, proto_idx, name_idx = struct.unpack_from("<HHI", data, row)
        methods.append((type_descriptors[owner_idx], strings[name_idx], prototypes[proto_idx]))

    defined_fields: set[tuple[str, str, str]] = set()
    defined_methods: set[tuple[str, str, str]] = set()
    defined_classes: set[str] = set()

    def read_encoded_fields(pos: int, count: int, owner: str) -> int:
        field_idx = 0
        for _ in range(count):
            diff, pos = uleb128(data, pos)
            _, pos = uleb128(data, pos)  # access flags
            field_idx += diff
            if field_idx >= len(fields):
                raise ValueError("field index out of range")
            field_owner, name, field_type = fields[field_idx]
            if field_owner == owner:
                defined_fields.add((field_owner, name, field_type))
        return pos

    def read_encoded_methods(pos: int, count: int, owner: str) -> int:
        method_idx = 0
        for _ in range(count):
            diff, pos = uleb128(data, pos)
            _, pos = uleb128(data, pos)  # access flags
            code_off, pos = uleb128(data, pos)
            method_idx += diff
            if method_idx >= len(methods):
                raise ValueError("method index out of range")
            method_owner, name, descriptor = methods[method_idx]
            if method_owner == owner and code_off != 0:
                defined_methods.add((method_owner, name, descriptor))
        return pos

    for index in range(class_count):
        row = class_off + index * 32
        class_idx = u32(data, row)
        owner = type_descriptors[class_idx]
        defined_classes.add(owner)
        class_data_off = u32(data, row + 24)
        if class_data_off == 0:
            continue
        pos = class_data_off
        static_fields, pos = uleb128(data, pos)
        instance_fields, pos = uleb128(data, pos)
        direct_methods, pos = uleb128(data, pos)
        virtual_methods, pos = uleb128(data, pos)
        pos = read_encoded_fields(pos, static_fields, owner)
        pos = read_encoded_fields(pos, instance_fields, owner)
        pos = read_encoded_methods(pos, direct_methods, owner)
        read_encoded_methods(pos, virtual_methods, owner)

    return DexSymbols(frozenset(defined_methods), frozenset(defined_fields), frozenset(defined_classes))


def main(apk_path: str) -> int:
    apk = Path(apk_path)
    if not apk.is_file():
        print(f"ERROR: APK not found: {apk}", file=sys.stderr)
        return 2

    all_methods: set[tuple[str, str, str]] = set()
    all_fields: set[tuple[str, str, str]] = set()
    all_classes: set[str] = set()
    with zipfile.ZipFile(apk) as archive:
        dex_names = sorted(
            (name for name in archive.namelist() if re.fullmatch(r"classes(?:\d+)?\.dex", name)),
            key=lambda name: 1 if name == "classes.dex" else int(name.removeprefix("classes").removesuffix(".dex")),
        )
        if not dex_names:
            print("ERROR: APK contains no classes*.dex files", file=sys.stderr)
            return 2
        for name in dex_names:
            symbols = parse_dex(archive.read(name))
            all_methods.update(symbols.methods)
            all_fields.update(symbols.fields)
            all_classes.update(symbols.classes)

    expected_methods = {
        method for method in all_methods
        if method[0].startswith(EXPECTED_OWNER_PREFIX)
        and method[1] == EXPECTED_METHOD
        and method[2] == EXPECTED_DESCRIPTOR
    }
    if not expected_methods:
        print(
            f"ERROR: minified APK is missing {EXPECTED_OWNER_PREFIX}*->{EXPECTED_METHOD}{EXPECTED_DESCRIPTOR}",
            file=sys.stderr,
        )
        return 1

    expected_field = (HTTP_SOURCE, EXPECTED_FIELD, EXPECTED_FIELD_TYPE)
    if expected_field not in all_fields:
        found = sorted(
            field for field in all_fields if field[0] == HTTP_SOURCE and field[1] == EXPECTED_FIELD
        )
        print(
            f"ERROR: {HTTP_SOURCE}->{EXPECTED_FIELD} must retain type {EXPECTED_FIELD_TYPE}; found {found}",
            file=sys.stderr,
        )
        return 1

    if EXPECTED_LAZY_IMPL not in all_classes:
        print(f"ERROR: minified APK is missing {EXPECTED_LAZY_IMPL}", file=sys.stderr)
        return 1

    if APP_INFO not in all_classes:
        print(f"ERROR: minified APK is missing dynamic extension host class {APP_INFO}", file=sys.stderr)
        return 1

    if APP_INFO_INSTANCE not in all_fields:
        print(f"ERROR: minified APK is missing dynamic extension field {APP_INFO_INSTANCE}", file=sys.stderr)
        return 1

    if APP_INFO_VERSION_METHOD not in all_methods:
        print(f"ERROR: minified APK is missing dynamic extension method {APP_INFO_VERSION_METHOD}", file=sys.stderr)
        return 1

    missing_runtime_classes = sorted(set(REQUIRED_RUNTIME_CLASSES) - all_classes)
    if missing_runtime_classes:
        print(
            f"ERROR: minified APK is missing dynamically used runtime classes: {missing_runtime_classes}",
            file=sys.stderr,
        )
        return 1

    print(
        f"Verified minified DEX in {apk.name}: {sorted(expected_methods)}, "
        f"{HTTP_SOURCE}->{EXPECTED_FIELD}:{EXPECTED_FIELD_TYPE}, SynchronizedLazyImpl, and "
        f"AppInfo.INSTANCE/getVersionName, plus {len(REQUIRED_RUNTIME_CLASSES)} extension runtime classes, are retained."
    )
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(f"Usage: {Path(sys.argv[0]).name} <release.apk>", file=sys.stderr)
        raise SystemExit(2)
    raise SystemExit(main(sys.argv[1]))
