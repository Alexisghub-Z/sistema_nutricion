"""Indexa los PDF docentes localmente sin modificar los originales."""
import hashlib
import json
import re
from pathlib import Path
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "ARCHIVOS"
OUTPUT = ROOT / ".cache" / "materiales-profesor"
TERMS = {
    "instalacion": r"install|instal|download|descarg|jdk|glassfish|payara|netbeans",
    "plantillas": r"facelet|templat|plantilla|ui:insert|ui:define|ui:composition",
    "validadores": r"validat|validado|f:validate|required|converter",
    "modelos": r"behavio|comportamiento|sequence diagram|secuencia|data model|modelo de datos|state diagram|use case|user stor",
    "exportacion": r"itext|poi\b|dataexporter|export.*pdf|export.*excel",
}


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    inventory = []
    for source in sorted(SOURCE.rglob("*")):
        if not source.is_file():
            continue
        item = {"archivo": str(source.relative_to(SOURCE)), "bytes": source.stat().st_size,
                "sha256": hashlib.file_digest(source.open("rb"), "sha256").hexdigest()}
        if source.suffix.lower() == ".pdf":
            reader = PdfReader(source)
            pages = []
            matches = {key: [] for key in TERMS}
            empty = []
            for number, page in enumerate(reader.pages, 1):
                content = page.extract_text(extraction_mode="layout") or ""
                pages.append({"pagina_pdf": number, "texto": content})
                if len(content.strip()) < 30:
                    empty.append(number)
                for key, pattern in TERMS.items():
                    if re.search(pattern, content, re.I):
                        matches[key].append(number)
            output = OUTPUT / (source.stem + ".json")
            output.write_text(json.dumps(pages, ensure_ascii=False), encoding="utf-8")
            item.update(paginas=len(pages), paginas_sin_texto=empty, coincidencias=matches,
                        indice=str(output.relative_to(ROOT)))
        inventory.append(item)
        print(json.dumps(item, ensure_ascii=False), flush=True)
    (OUTPUT / "inventario.json").write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
