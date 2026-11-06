import * as THREE from 'three'
import { COLORS } from './palette'
import type { WorkshopNode } from './types'

export interface LabelSprite {
  sprite: THREE.Sprite
  texture: THREE.Texture
}

export function createLights(): THREE.Group {
  const group = new THREE.Group()

  const ambient = new THREE.HemisphereLight(0xcfe2ff, 0x123047, 1.5)
  group.add(ambient)

  const key = new THREE.DirectionalLight(0xffffff, 2.1)
  key.position.set(40, 60, 30)
  key.castShadow = true
  key.shadow.mapSize.set(1024, 1024)
  key.shadow.camera.near = 1
  key.shadow.camera.far = 220
  key.shadow.camera.left = -80
  key.shadow.camera.right = 80
  key.shadow.camera.top = 80
  key.shadow.camera.bottom = -80
  key.shadow.bias = -0.0008
  group.add(key)

  const rim = new THREE.DirectionalLight(0x5b9fe6, 1.0)
  rim.position.set(-50, 30, -40)
  group.add(rim)

  return group
}

export function createGround(width: number, depth: number): THREE.Group {
  const group = new THREE.Group()

  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(width + 40, depth + 34),
    new THREE.MeshStandardMaterial({ color: COLORS.ground, metalness: 0.08, roughness: 0.9 })
  )
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.02
  ground.receiveShadow = true
  group.add(ground)

  const grid = new THREE.GridHelper(Math.max(width, depth) + 60, 48, COLORS.grid, 0x122b42)
  grid.position.y = 0
  group.add(grid)

  const roadMaterial = new THREE.MeshStandardMaterial({ color: COLORS.road, metalness: 0.1, roughness: 0.9 })
  const roadLength = width + 40
  for (const z of [depth / 2 + 5.5, -depth / 2 - 5.5]) {
    const road = new THREE.Mesh(new THREE.BoxGeometry(roadLength, 0.12, 4.6), roadMaterial)
    road.position.set(0, 0.03, z)
    road.receiveShadow = true
    group.add(road)

    const dashMaterial = new THREE.MeshBasicMaterial({ color: 0x2a4a66 })
    for (let x = -roadLength / 2 + 2; x < roadLength / 2; x += 6) {
      const dash = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.02, 0.18), dashMaterial)
      dash.position.set(x, 0.1, z)
      group.add(dash)
    }
  }

  return group
}

export function createLabel(text: string, position: THREE.Vector3, width = 10): LabelSprite {
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 128
  const context = canvas.getContext('2d')
  if (context) {
    context.clearRect(0, 0, canvas.width, canvas.height)
    context.fillStyle = 'rgba(10, 23, 38, 0.72)'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.strokeStyle = 'rgba(46, 163, 255, 0.6)'
    context.lineWidth = 4
    context.strokeRect(2, 2, canvas.width - 4, canvas.height - 4)
    context.fillStyle = COLORS.label
    context.font = 'bold 52px "PingFang SC", "Microsoft YaHei", sans-serif'
    context.textAlign = 'center'
    context.textBaseline = 'middle'
    context.fillText(text, canvas.width / 2, canvas.height / 2)
  }

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  const material = new THREE.SpriteMaterial({ map: texture, transparent: true, depthWrite: false })
  const sprite = new THREE.Sprite(material)
  sprite.position.copy(position)
  sprite.scale.set(width, width / 4, 1)
  return { sprite, texture }
}

/** 能源管线：从配电房出发，沿地埋/桥架走向各车间 */
export function createPipes(workshops: WorkshopNode[], centerX: number, depth: number): THREE.Group {
  const group = new THREE.Group()
  if (workshops.length === 0) {
    return group
  }

  const stationPosition = new THREE.Vector3(centerX - 26, 0, -depth / 2 - 10)
  const station = new THREE.Group()
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(5, 0.4, 4),
    new THREE.MeshStandardMaterial({ color: COLORS.road, metalness: 0.3, roughness: 0.7 })
  )
  base.position.set(stationPosition.x, 0.2, stationPosition.z)
  base.receiveShadow = true
  station.add(base)

  const transformer = new THREE.Mesh(
    new THREE.CylinderGeometry(0.9, 0.9, 1.6, 16),
    new THREE.MeshStandardMaterial({
      color: COLORS.substation,
      emissive: COLORS.substation,
      emissiveIntensity: 0.5,
      metalness: 0.4,
      roughness: 0.4
    })
  )
  transformer.position.set(stationPosition.x, 1.2, stationPosition.z)
  transformer.castShadow = true
  station.add(transformer)

  const label = createLabel('配电房', new THREE.Vector3(stationPosition.x, 3.6, stationPosition.z), 6)
  station.add(label.sprite)
  group.add(station)

  const pipeMaterial = new THREE.MeshStandardMaterial({
    color: COLORS.pipe,
    emissive: COLORS.pipe,
    emissiveIntensity: 0.55,
    metalness: 0.4,
    roughness: 0.35
  })

  for (const workshop of workshops) {
    const start = new THREE.Vector3(stationPosition.x + 2, 1.4, stationPosition.z + 2)
    const end = new THREE.Vector3(workshop.position.x - workshop.width / 2, 1.6, workshop.position.z + workshop.depth / 2 - 1)
    const mid = new THREE.Vector3((start.x + end.x) / 2, 2.6, (start.z + end.z) / 2)
    const curve = new THREE.CatmullRomCurve3([start, mid, end])
    const tube = new THREE.Mesh(new THREE.TubeGeometry(curve, 40, 0.11, 8, false), pipeMaterial)
    tube.castShadow = true
    group.add(tube)
  }

  return group
}
