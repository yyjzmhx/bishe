from __future__ import annotations

import argparse
import re
import struct
from collections import OrderedDict
from pathlib import Path

import olefile


FC_CLX_INDEX = 33
ANSI_SPECIAL_MAP = {
    0x82: "\u201A",
    0x83: "\u0192",
    0x84: "\u201E",
    0x85: "\u2026",
    0x86: "\u2020",
    0x87: "\u2021",
    0x88: "\u02C6",
    0x89: "\u2030",
    0x8A: "\u0160",
    0x8B: "\u2039",
    0x8C: "\u0152",
    0x91: "\u2018",
    0x92: "\u2019",
    0x93: "\u201C",
    0x94: "\u201D",
    0x95: "\u2022",
    0x96: "\u2013",
    0x97: "\u2014",
    0x98: "\u02DC",
    0x99: "\u2122",
    0x9A: "\u0161",
    0x9B: "\u203A",
    0x9C: "\u0153",
    0x9F: "\u0178",
}
CONTROL_TRANSLATION = str.maketrans(
    {
        "\r": "\n",
        "\x07": "\n",
        "\x0b": "\n",
        "\x0c": "\n",
        "\x1e": "",
        "\x1f": "",
        "\x00": "",
    }
)


def u16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<H", data, offset)[0]


def u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def parse_fib(word_stream: bytes) -> dict[str, int]:
    fib_base_flags = u16(word_stream, 0x0A)
    use_1table = bool(fib_base_flags & 0x0200)

    pos = 32
    csw = u16(word_stream, pos)
    pos += 2 + csw * 2

    cslw = u16(word_stream, pos)
    pos += 2
    rg_lw = [u32(word_stream, pos + i * 4) for i in range(cslw)]
    pos += cslw * 4

    cb_rg_fc_lcb = u16(word_stream, pos)
    pos += 2
    rg_fc_lcb = [(u32(word_stream, pos + i * 8), u32(word_stream, pos + i * 8 + 4)) for i in range(cb_rg_fc_lcb)]

    if len(rg_lw) < 4:
        raise ValueError("FIB rg_lw is shorter than expected; could not read ccpText.")
    if len(rg_fc_lcb) <= FC_CLX_INDEX:
        raise ValueError("FIB rg_fc_lcb is shorter than expected; could not read fcClx/lcbClx.")

    fc_clx, lcb_clx = rg_fc_lcb[FC_CLX_INDEX]
    return {
        "use_1table": int(use_1table),
        "ccp_text": rg_lw[3],
        "fc_clx": fc_clx,
        "lcb_clx": lcb_clx,
    }


def parse_clx(table_stream: bytes, fc_clx: int, lcb_clx: int) -> tuple[list[int], list[bytes]]:
    clx = table_stream[fc_clx : fc_clx + lcb_clx]
    if not clx:
        raise ValueError("CLX is empty.")

    pos = 0
    while pos < len(clx) and clx[pos] == 0x01:
        if pos + 3 > len(clx):
            raise ValueError("Malformed CLX: truncated Prc header.")
        cb_grpprl = u16(clx, pos + 1)
        pos += 3 + cb_grpprl

    if pos >= len(clx) or clx[pos] != 0x02:
        raise ValueError("Malformed CLX: missing Pcdt marker.")
    if pos + 5 > len(clx):
        raise ValueError("Malformed CLX: truncated Pcdt header.")

    lcb = u32(clx, pos + 1)
    plcpcd = clx[pos + 5 : pos + 5 + lcb]
    if len(plcpcd) != lcb:
        raise ValueError("Malformed CLX: PlcPcd size does not match lcb.")
    if lcb < 4 or (lcb - 4) % 12 != 0:
        raise ValueError("Malformed PlcPcd: size does not align to CP/Pcd entries.")

    piece_count = (lcb - 4) // 12
    cp_count = piece_count + 1
    cps = [struct.unpack_from("<i", plcpcd, i * 4)[0] for i in range(cp_count)]
    pcd_offset = cp_count * 4
    pcds = [plcpcd[pcd_offset + i * 8 : pcd_offset + (i + 1) * 8] for i in range(piece_count)]
    return cps, pcds


def decode_ansi_piece(piece_bytes: bytes) -> str:
    chars: list[str] = []
    for value in piece_bytes:
        if value in ANSI_SPECIAL_MAP:
            chars.append(ANSI_SPECIAL_MAP[value])
        else:
            chars.append(bytes([value]).decode("cp1252"))
    return "".join(chars)


def decode_piece(word_stream: bytes, pcd: bytes, cp_length: int) -> str:
    if len(pcd) != 8:
        raise ValueError("Malformed Pcd entry.")

    fc_compressed = u32(pcd, 2)
    compressed = bool(fc_compressed & 0x40000000)
    fc = fc_compressed & 0x3FFFFFFF

    if compressed:
        byte_offset = fc // 2
        raw = word_stream[byte_offset : byte_offset + cp_length]
        return decode_ansi_piece(raw)

    byte_offset = fc
    raw = word_stream[byte_offset : byte_offset + cp_length * 2]
    return raw.decode("utf-16le", errors="ignore")


def extract_main_text(doc_path: Path) -> str:
    with olefile.OleFileIO(doc_path) as ole:
        word_stream = ole.openstream("WordDocument").read()
        fib = parse_fib(word_stream)
        table_name = "1Table" if fib["use_1table"] else "0Table"
        table_stream = ole.openstream(table_name).read()
        cps, pcds = parse_clx(table_stream, fib["fc_clx"], fib["lcb_clx"])

    main_limit = fib["ccp_text"]
    parts: list[str] = []
    for i, pcd in enumerate(pcds):
        start_cp = cps[i]
        end_cp = cps[i + 1]
        if start_cp >= main_limit:
            break
        if end_cp <= 0:
            continue

        start = max(start_cp, 0)
        end = min(end_cp, main_limit)
        length = end - start
        if length <= 0:
            continue

        text = decode_piece(word_stream, pcd, end_cp - start_cp)
        relative_start = start - start_cp
        relative_end = relative_start + length
        parts.append(text[relative_start:relative_end])

    return normalize_text("".join(parts))


def normalize_text(text: str) -> str:
    text = text.translate(CONTROL_TRANSLATION)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n[ \t]+", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def split_reference_entries(reference_text: str) -> OrderedDict[int, str]:
    entries: OrderedDict[int, str] = OrderedDict()
    current_number: int | None = None
    current_lines: list[str] = []

    pattern = re.compile(r"^\s*(?:\[(\d+)\]|（(\d+)）|\((\d+)\)|(\d+)[\.\s、])\s*(.*)$")

    for raw_line in reference_text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        match = pattern.match(line)
        if match:
            if current_number is not None:
                entries[current_number] = " ".join(current_lines).strip()
            groups = match.groups()
            number = next(int(value) for value in groups[:4] if value)
            tail = groups[4].strip()
            current_number = number
            current_lines = [tail] if tail else []
        elif current_number is not None:
            current_lines.append(line)

    if current_number is not None:
        entries[current_number] = " ".join(current_lines).strip()

    return entries


def extract_reference_section(full_text: str) -> tuple[str, str]:
    heading_pattern = re.compile(r"(?m)^\s*参考文献\s*$")
    matches = list(heading_pattern.finditer(full_text))
    if not matches:
        raise ValueError("未找到“参考文献”标题。")

    match = matches[-1]
    body_text = full_text[: match.start()].strip()
    reference_text = full_text[match.end() :].strip()

    stop_pattern = re.compile(r"(?m)^\s*(致\s*谢|谢\s*辞|附\s*录|作者简介|攻读.*期间.*成果)\s*$")
    stop_match = stop_pattern.search(reference_text)
    if stop_match:
        reference_text = reference_text[: stop_match.start()].strip()

    return body_text, reference_text


def expand_citation_content(content: str) -> list[int]:
    normalized = (
        content.replace("，", ",")
        .replace("、", ",")
        .replace("－", "-")
        .replace("—", "-")
        .replace("–", "-")
        .replace(" ", "")
    )
    numbers: list[int] = []
    for part in filter(None, normalized.split(",")):
        if "-" in part:
            start_text, end_text = part.split("-", 1)
            if start_text.isdigit() and end_text.isdigit():
                start = int(start_text)
                end = int(end_text)
                if start <= end:
                    numbers.extend(range(start, end + 1))
                else:
                    numbers.extend(range(start, end - 1, -1))
        elif part.isdigit():
            numbers.append(int(part))
    return numbers


def extract_citation_order(body_text: str, valid_numbers: set[int]) -> list[int]:
    normalized = body_text.translate(str.maketrans({"［": "[", "］": "]"}))
    citation_pattern = re.compile(r"\[([0-9,\-，、—–\s]+)\]")

    seen: set[int] = set()
    ordered: list[int] = []
    for match in citation_pattern.finditer(normalized):
        for number in expand_citation_content(match.group(1)):
            if number in valid_numbers and number not in seen:
                seen.add(number)
                ordered.append(number)
    return ordered


def build_output(entries: OrderedDict[int, str], citation_order: list[int]) -> str:
    cited = [number for number in citation_order if number in entries]
    remaining = [number for number in entries if number not in cited]
    ordered_numbers = cited + remaining

    lines = []
    for new_index, original_number in enumerate(ordered_numbers, start=1):
        content = entries[original_number]
        lines.append(f"[{new_index}] {content}")
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description="Reorder thesis bibliography entries by first citation order in a .doc file.")
    parser.add_argument("--doc", required=True, help="Path to the source .doc file")
    parser.add_argument("--out", required=True, help="Path to the output text file")
    parser.add_argument("--debug-text", help="Optional path to save extracted main text")
    args = parser.parse_args()

    doc_path = Path(args.doc)
    out_path = Path(args.out)

    text = extract_main_text(doc_path)
    if args.debug_text:
        Path(args.debug_text).write_text(text, encoding="utf-8-sig")

    body_text, reference_text = extract_reference_section(text)
    entries = split_reference_entries(reference_text)
    if not entries:
        raise ValueError("未解析到参考文献条目。")

    citation_order = extract_citation_order(body_text, set(entries))
    if not citation_order:
        raise ValueError("未在正文中解析到可匹配参考文献编号的引用。")

    output = build_output(entries, citation_order)
    out_path.write_text(output, encoding="utf-8-sig")

    print(f"Extracted references: {len(entries)}")
    print(f"Cited references in order: {len(citation_order)}")
    print(f"Output: {out_path}")


if __name__ == "__main__":
    main()
