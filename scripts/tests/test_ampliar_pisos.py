"""Pruebas del actualizador sobre archivos temporales, sin tocar la partida."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'scripts/ampliar-pisos.py'
spec = importlib.util.spec_from_file_location('ampliar', SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
PRESET = json.loads((ROOT / 'world/expansions/floors-large-v1.json').read_text())


def compact_world():
    world = json.loads((ROOT / 'world/world.json').read_text())
    for entry in PRESET['floors']:
        floor = next(f for f in world['floors'] if f['id'] == entry['id'])
        old = entry['before']
        for key in ('radius', 'treeCount', 'spawn'):
            floor[key] = copy.deepcopy(old[key])
        floor['portal'].update(old['portal'])
        for collection in module.FIELDS:
            existing = {item['id']: item for item in floor[collection]}
            floor[collection] = [existing[item['id']] | item for item in old[collection]]
    return world


class ExpansionTest(unittest.TestCase):
    def run_script(self, file, *args):
        return subprocess.run([sys.executable, str(SCRIPT), str(file), *args], capture_output=True, text=True)

    def test_preserves_custom_rules_and_makes_exact_backup_once(self):
        world = compact_world()
        world['floors'][0]['npcs'][0]['name'] = 'Mi guía'
        world['floors'][0]['monsterZones'][0]['population'] = 4
        world['characterOptions']['classes'][0]['hp'] = 190
        world['pvp']['murderPenalty'] = 30
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / 'mi mundo.json'
            original = json.dumps(world).encode()
            file.write_bytes(original)
            self.assertEqual(self.run_script(file, '--dry-run').returncode, 0)
            self.assertEqual(file.read_bytes(), original)
            self.assertFalse((file.parent / 'backups').exists())
            result = self.run_script(file)
            self.assertEqual(result.returncode, 0, result.stderr)
            expanded = json.loads(file.read_bytes())
            self.assertEqual([f['radius'] for f in expanded['floors']], [92, 84])
            self.assertEqual(sum(len(f['resources']) for f in expanded['floors']), 22)
            self.assertEqual(expanded['characterOptions'], world['characterOptions'])
            self.assertEqual(expanded['pvp'], world['pvp'])
            self.assertEqual(expanded['floors'][0]['npcs'][0]['name'], 'Mi guía')
            self.assertEqual(expanded['floors'][0]['monsterZones'][0]['population'], 4)
            backups = list((file.parent / 'backups').iterdir())
            self.assertEqual(len(backups), 1)
            self.assertEqual(backups[0].read_bytes(), original)
            first = file.read_bytes()
            self.assertEqual(self.run_script(file).returncode, 0)
            self.assertEqual(file.read_bytes(), first)
            self.assertEqual(list((file.parent / 'backups').iterdir()), backups)

    def test_custom_geometry_aborts_every_floor_without_writing(self):
        world = compact_world()
        world['floors'][1]['spawn']['z'] = 17
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / 'world.json'
            original = json.dumps(world).encode()
            file.write_bytes(original)
            result = self.run_script(file)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn('distribución personalizada', result.stderr)
            self.assertEqual(file.read_bytes(), original)
            self.assertFalse((file.parent / 'backups').exists())

    def test_expansion_rejects_collision_with_an_added_floor_id(self):
        world = compact_world()
        extra = copy.deepcopy(world['floors'][0])
        extra['id'] = 3
        extra['npcs'] = []; extra['monsters'] = []; extra['monsterZones'] = []
        extra['buildings'] = []; extra['training'] = None
        extra['resources'] = [{'id': 'resource-1-5', 'typeId': 'iron-vein', 'x': 20, 'z': 20}]
        world['floors'].append(extra)
        with self.assertRaisesRegex(ValueError, 'identificadores repetidos'):
            module.expand(world, PRESET)


if __name__ == '__main__':
    unittest.main()
