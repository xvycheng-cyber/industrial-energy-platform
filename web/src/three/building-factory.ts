import * as THREE from 'three'
import { COLORS } from './palette'
import { createLabel, type LabelSprite } from './environment'
import type { WorkshopNode } from './types'

export interface BuiltWorkshop {
  group: THREE.Group
  lineStrips: THREE.Mesh[]
  labels: LabelSprite[]
}

function buildLineStrip(line: WorkshopNode['lines'][number]): THREE.Group {
  const group = new THREE.Group()

  const deck = new THREE.Mesh(
    new THREE.BoxGeometry(14, 0.08, 1.15),
    new THREE.MeshStandardMaterial({ color: 0x2b6486, metalness: 0.35, roughness: 0.6 })
  )
  deck.position.y = 0
  deck.receiveShadow = true
  deck.userData = { kind: 'line', lineId: line.id, name: line.name, code: line.code }
  group.add(deck)

  const stripeMaterial = new THREE.MeshStandardMaterial({
    color: COLORS.lineStripe,
    emissive: COLORS.lineStripe,
    emissiveIntensity: 0.5,
    metalness: 0.2,
    roughness: 0.4
  })
  for (const z of [-0.5, 0.5]) {
    const stripe = new THREE.Mesh(new THREE.BoxGeometry(14, 0.02, 0.08), stripeMaterial)
    stripe.position.set(0, 0.06, z)
    group.add(stripe)
  }

  for (let x = -5; x <= 5; x += 2.5) {
    const arrow = new THREE.Mesh(new THREE.ConeGeometry(0.14, 0.34, 8), stripeMaterial)
    arrow.rotation.z = -Math.PI / 2
    arrow.position.set(x, 0.07, 0)
    group.add(arrow)
  }

  group.position.set(0, 0.34, line.position.z)
  return group
}

/**
 * 车间厂房 + 产线。
 * 墙体半透明，便于俯视观察内部产线与设备。
 */
export function buildWorkshop(workshop: WorkshopNode): BuiltWorkshop {
  const group = new THREE.Group()
  group.position.set(workshop.position.x, 0, workshop.position.z)
  group.userData = { kind: 'workshop', id: workshop.id, name: workshop.name }

  const { width, depth, height } = workshop

  const floor = new THREE.Mesh(
    new THREE.BoxGeometry(width, 0.3, depth),
    new THREE.MeshStandardMaterial({ color: 0x18374f, metalness: 0.15, roughness: 0.85 })
  )
  floor.position.y = 0.15
  floor.receiveShadow = true
  group.add(floor)

  const wallMaterial = new THREE.MeshStandardMaterial({
    color: COLORS.building,
    transparent: true,
    opacity: 0.4,
    metalness: 0.2,
    roughness: 0.7,
    side: THREE.DoubleSide
  })
  const wallThickness = 0.25
  const walls: Array<[number, number, number, number, number, number]> = [
    [width, height, wallThickness, 0, height / 2, -depth / 2],
    [width, height, wallThickness, 0, height / 2, depth / 2],
    [wallThickness, height, depth, -width / 2, height / 2, 0],
    [wallThickness, height, depth, width / 2, height / 2, 0]
  ]
  for (const [w, h, d, x, y, z] of walls) {
    const wall = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), wallMaterial)
    wall.position.set(x, y, z)
    group.add(wall)
  }

  const edgeGeometry = new THREE.EdgesGeometry(new THREE.BoxGeometry(width, height, depth))
  const edges = new THREE.LineSegments(
    edgeGeometry,
    new THREE.LineBasicMaterial({ color: COLORS.buildingEdge, transparent: true, opacity: 0.75 })
  )
  edges.position.y = height / 2
  group.add(edges)

  const columnMaterial = new THREE.MeshStandardMaterial({ color: 0x2b5471, metalness: 0.45, roughness: 0.5 })
  for (const x of [-width / 2 + 0.4, width / 2 - 0.4]) {
    for (const z of [-depth / 2 + 0.4, depth / 2 - 0.4]) {
      const column = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, height, 10), columnMaterial)
      column.position.set(x, height / 2, z)
      column.castShadow = true
      group.add(column)
    }
  }

  const lineStrips: THREE.Mesh[] = []
  const labels: LabelSprite[] = []
  for (const line of workshop.lines) {
    const strip = buildLineStrip(line)
    group.add(strip)
    strip.traverse((child) => {
      if (child instanceof THREE.Mesh && child.userData.kind === 'line') {
        lineStrips.push(child)
      }
    })
    if (workshop.lines.length <= 3) {
      const lineLabel = createLabel(line.name, new THREE.Vector3(0, 1.45, line.position.z), 4.6)
      group.add(lineLabel.sprite)
      labels.push(lineLabel)
    }
  }

  const labelPosition = new THREE.Vector3(0, height + 2.4, 0)
  const roof = new THREE.Mesh(
    new THREE.BoxGeometry(width, 0.12, depth),
    new THREE.MeshStandardMaterial({
      color: COLORS.roof,
      transparent: true,
      opacity: 0.16,
      metalness: 0.3,
      roughness: 0.6
    })
  )
  roof.position.y = height
  group.add(roof)

  const label = createLabel(workshop.name, labelPosition, 9)
  group.add(label.sprite)
  labels.push(label)

  return { group, lineStrips, labels }
}
