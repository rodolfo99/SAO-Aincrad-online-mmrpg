#!/usr/bin/env python3
"""Amplía los dos pisos originales sin sustituir los catálogos ni las partidas.

Ejecutar con el servidor detenido. No requiere paquetes externos.
"""
import argparse
import copy
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import tempfile
import uuid

ROOT = Path(__file__).resolve().parents[1]
FIELDS = {
    'npcs': ('id', 'x', 'z'),
    'monsters': ('id', 'x', 'z'),
    'monsterZones': ('id', 'x', 'z', 'radius'),
    'buildings': ('id', 'kind', 'x', 'z', 'rotation'),
    'resources': ('id', 'typeId', 'x', 'z'),
}


def geometry(floor):
    result = {key: floor[key] for key in ('radius', 'treeCount', 'spawn')}
    result['portal'] = {key: floor['portal'][key] for key in ('x', 'z')}
    for collection, fields in FIELDS.items():
        result[collection] = [
            {key: item[key] for key in fields}
            for item in sorted(floor.get(collection, []), key=lambda item: item['id'])
        ]
    training = floor.get('training')
    result['training'] = None
    if training:
        result['training'] = {key: training[key] for key in ('x', 'z', 'radius')}
        result['training']['dummies'] = [
            {key: item[key] for key in ('id', 'x', 'z')}
            for item in sorted(training['dummies'], key=lambda item: item['id'])
        ]
    return result


def expand(world, preset):
    if world.get('schemaRevision') != preset['schemaRevision']:
        raise ValueError('Se requiere un mundo de esquema 7; inicia primero la versión anterior para migrarlo.')
    result, changed = copy.deepcopy(world), []
    for entry in preset['floors']:
        candidates = [f for f in result['floors'] if f['id'] == entry['id']]
        if len(candidates) != 1:
            raise ValueError(f"No se encuentra un único piso {entry['id']}.")
        floor = candidates[0]
        current = geometry(floor)
        if current == entry['after']:
            continue
        if current != entry['before']:
            raise ValueError(
                f"El piso {entry['id']} tiene una distribución personalizada. "
                'No se ha modificado ningún archivo. Amplíalo desde root y ajusta sus posiciones manualmente.'
            )
        target = entry['after']
        for key in ('radius', 'treeCount', 'spawn'):
            floor[key] = copy.deepcopy(target[key])
        floor['portal'].update(target['portal'])
        for collection in FIELDS:
            by_id = {item['id']: item for item in floor.get(collection, [])}
            for item in target[collection]:
                if item['id'] in by_id:
                    by_id[item['id']].update(item)
                else:
                    floor.setdefault(collection, []).append(copy.deepcopy(item))
        floor.pop('props', None)  # El servidor regenera las colisiones con la semilla.
        changed.append(floor['id'])
    # Los nodos nuevos tampoco pueden reutilizar un ID de otro piso añadido por root.
    ids = []
    for floor in result['floors']:
        for collection in FIELDS:
            ids.extend(item['id'] for item in floor.get(collection, []))
        ids.extend(item['id'] for item in (floor.get('training') or {}).get('dummies', []))
    if len(ids) != len(set(ids)):
        raise ValueError('Hay identificadores repetidos. No se ha modificado ningún archivo.')
    return result, changed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', nargs='?', type=Path, default=ROOT / 'world/world.json')
    parser.add_argument('--dry-run', action='store_true', help='Validar y mostrar cambios sin escribir.')
    args = parser.parse_args()
    target = args.world.resolve()
    original = target.read_bytes()
    preset = json.loads((ROOT / 'world/expansions/floors-large-v1.json').read_text(encoding='utf-8'))
    result, changed = expand(json.loads(original), preset)
    if not changed:
        print('Los dos pisos ya están ampliados. No se modificó ningún archivo.')
        return
    print('Pisos a ampliar: ' + ', '.join(map(str, changed)) + '. Radios finales: 92 y 84 m.')
    if args.dry_run:
        print('Comprobación completada; no se escribió ningún archivo.')
        return
    content = (json.dumps(result, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    backup_dir = target.parent / 'backups'
    backup_dir.mkdir(exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup = backup_dir / f'{target.stem}-antes-ampliacion-{stamp}-{uuid.uuid4().hex[:8]}.json'
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(dir=target.parent, prefix='.ampliar-', suffix='.partial', delete=False) as output:
            temporary = Path(output.name)
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        temporary.chmod(target.stat().st_mode & 0o777)
        if target.read_bytes() != original:
            raise ValueError('El archivo cambió durante la operación. Detén el servidor y vuelve a intentarlo.')
        with backup.open('xb') as output:
            output.write(original)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, target)
        print(f'Ampliación completada. Copia anterior: {backup}')
        print('Puedes iniciar el servidor. Personajes, claves y configuración de IA se conservan en data/.')
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError) as error:
        raise SystemExit(f'No se pudo ampliar el mundo: {error}') from error
