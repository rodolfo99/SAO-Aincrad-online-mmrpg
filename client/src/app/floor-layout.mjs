// Shared world-space dimensions for scenery and map overlays. Coordinates stay
// authoritative on the server; this module never changes movement or collision.
export function floorLayout(floor) {
  const radius = Number.isFinite(floor?.radius) ? floor.radius : 46;
  const margin = Math.max(3, radius * .05), extent = radius + margin;
  return {
    radius, diameter: radius * 2, area: Math.round(Math.PI * radius * radius),
    viewBox: `${-extent} ${-extent} ${extent * 2} ${extent * 2}`,
    markerScale: radius / 46,
    road: { x: 0, from: -radius + 6, to: radius - 6, width: 5.6 },
    plaza: { x: floor?.spawn?.x || 0, z: (floor?.spawn?.z ?? 16) + 1, radius: 8.9 },
    cloudRadius: radius + 36, castleZ: -radius - 60,
    densityScale: Math.min(4.75, (radius / 46) ** 2)
  };
}
