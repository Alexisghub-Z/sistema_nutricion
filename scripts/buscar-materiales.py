"""Busca texto por página en los índices locales, incluidos los resultados OCR."""
import json
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1] / ".cache" / "materiales-profesor"
if len(sys.argv) != 2:
    raise SystemExit('Uso: python scripts/buscar-materiales.py "término"')
term = sys.argv[1].casefold()
compact_term = re.sub(r"\s+", "", term)
total = 0
for source in sorted(root.rglob("*.json")):
    if source.name == "inventario.json":
        continue
    pages = json.loads(source.read_text(encoding="utf-8-sig"))
    if not isinstance(pages, list):
        continue
    for page in pages:
        content = page.get("texto", "")
        if term not in content.casefold() and compact_term not in re.sub(r"\s+", "", content.casefold()):
            continue
        lines = content.splitlines()
        found = next((i for i, line in enumerate(lines) if compact_term in re.sub(r"\s+", "", line.casefold())), 0)
        snippet = " ".join(lines[max(0, found - 1):found + 3])
        print(f"{source.relative_to(root)} | página PDF {page['pagina_pdf']} | {snippet[:350]}")
        total += 1
print(f"Coincidencias: {total}")
