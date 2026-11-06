import * as THREE from 'three'
import { COLORS, RISK_COLORS } from './palette'
import type { DeviceNode } from './types'

export interface BuiltDevice {
  group: THREE.Group
  accentMaterials: THREE.MeshStandardMaterial[]
  fanBlades?: THREE.Object3D
}

function standard(color: number, metalness = 0.35, roughness = 0.55): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ color, metalness, roughness })
}

function box(w: number, h: number, d: number, material: THREE.Material, x = 0, y = 0, z = 0): THREE.Mesh {
  const mesh = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material)
  mesh.position.set(x, y, z)
  mesh.castShadow = true
  mesh.receiveShadow = true
  return mesh
}

function cylinder(radius: number, height: number, material: THREE.Material, x = 0, y = 0, z = 0): THREE.Mesh {
  const mesh = new THREE.Mesh(new THREE.CylinderGeometry(radius, radius, height, 20), material)
  mesh.position.set(x, y, z)
  mesh.castShadow = true
  mesh.receiveShadow = true
  return mesh
}

/** 数控机床 / 注塑机 / 涂覆机：机座 + 立柱 + 主轴 + 操作面板 */
function buildMachine(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): THREE.Group {
  const group = new THREE.Group()
  group.add(box(2.4, 0.24, 1.7, dark, 0, 0.12, 0))
  group.add(box(2.0, 1.15, 1.35, body, 0, 0.8, 0))
  group.add(box(0.55, 1.05, 0.75, dark, 0.62, 1.85, 0))
  group.add(cylinder(0.16, 0.7, dark, 0.62, 1.35, 0.35))
  group.add(box(0.5, 0.36, 0.08, accent, -0.55, 1.32, 0.7))
  group.add(box(1.1, 0.12, 0.9, accent, 0, 1.42, -0.1))
  return group
}

/** 空压机：卧式储气罐 + 电机 + 管路 */
function buildCompressor(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): THREE.Group {
  const group = new THREE.Group()
  const tank = cylinder(0.62, 2.1, body, 0, 0.85, 0)
  tank.rotation.z = Math.PI / 2
  group.add(tank)
  group.add(box(0.9, 0.75, 0.85, dark, 1.45, 0.55, 0))
  group.add(cylinder(0.09, 1.2, dark, -1.15, 1.25, 0))
  group.add(box(0.34, 0.34, 0.34, accent, 0.9, 1.32, 0.35))
  group.add(box(1.9, 0.16, 0.7, dark, 0, 0.08, 0))
  return group
}

/** 冷却泵：底座 + 泵体 + 立管 + 阀门 */
function buildPump(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): THREE.Group {
  const group = new THREE.Group()
  group.add(box(1.5, 0.2, 1.1, dark, 0, 0.1, 0))
  group.add(cylinder(0.42, 0.9, body, 0, 0.66, 0))
  group.add(cylinder(0.16, 1.3, dark, 0.1, 1.4, 0))
  group.add(box(0.5, 0.18, 0.5, accent, 0.1, 1.05, 0))
  group.add(cylinder(0.09, 0.9, dark, -0.72, 0.7, 0.35))
  return group
}

/** 工业风机：机壳 + 叶轮 + 风管 */
function buildFan(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): { group: THREE.Group; blades: THREE.Object3D } {
  const group = new THREE.Group()
  const housing = new THREE.Mesh(new THREE.CylinderGeometry(0.78, 0.78, 0.55, 24), body)
  housing.rotation.x = Math.PI / 2
  housing.position.set(0, 1.1, 0)
  housing.castShadow = true
  group.add(housing)

  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.79, 0.05, 8, 32), accent)
  ring.position.set(0, 1.1, 0.28)
  group.add(ring)

  const blades = new THREE.Group()
  for (let i = 0; i < 4; i += 1) {
    const blade = box(0.62, 0.06, 0.18, dark, 0.32, 0, 0)
    blade.rotation.z = (Math.PI / 2) * i
    blades.add(blade)
  }
  blades.position.set(0, 1.1, 0.3)
  group.add(blades)

  group.add(box(0.5, 0.9, 0.5, dark, 0, 0.45, -0.5))
  group.add(box(1.4, 0.16, 0.9, dark, 0, 0.08, -0.2))
  const duct = box(0.55, 0.55, 0.8, dark, 0, 1.1, -0.85)
  group.add(duct)
  return { group, blades }
}

/** 输送设备：输送带 + 滚筒 + 端部驱动 */
function buildConveyor(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): THREE.Group {
  const group = new THREE.Group()
  group.add(box(2.9, 0.16, 0.95, dark, 0, 0.55, 0))
  group.add(box(2.8, 0.06, 0.7, body, 0, 0.66, 0))
  for (let i = -2; i <= 2; i += 1) {
    const roller = cylinder(0.11, 0.8, accent, i * 0.6, 0.68, 0)
    roller.rotation.x = Math.PI / 2
    group.add(roller)
  }
  group.add(box(0.2, 0.5, 0.9, dark, -1.35, 0.28, 0))
  group.add(box(0.2, 0.5, 0.9, dark, 1.35, 0.28, 0))
  return group
}

/** 机械手：底座 + 大臂 + 小臂 + 夹爪 */
function buildRobot(body: THREE.Material, dark: THREE.Material, accent: THREE.MeshStandardMaterial): THREE.Group {
  const group = new THREE.Group()
  group.add(cylinder(0.45, 0.3, dark, 0, 0.15, 0))
  group.add(box(0.32, 1.0, 0.32, body, 0, 0.8, 0))
  const arm = box(1.15, 0.24, 0.28, body, 0.48, 1.34, 0)
  arm.rotation.z = -0.32
  group.add(arm)
  group.add(box(0.22, 0.55, 0.22, dark, 1.0, 1.02, 0))
  group.add(box(0.3, 0.16, 0.3, accent, 1.0, 0.78, 0))
  return group
}

/**
 * 根据设备类型构建设备模型。
 * 不依赖任何外部模型文件，全部使用基础几何体组合。
 */
export function buildDevice(device: DeviceNode): BuiltDevice {
  const accent = new THREE.MeshStandardMaterial({
    color: RISK_COLORS[device.risk],
    emissive: RISK_COLORS[device.risk],
    emissiveIntensity: device.risk === 'HIGH' ? 0.5 : device.risk === 'MEDIUM' ? 0.34 : 0.22,
    metalness: 0.3,
    roughness: 0.45
  })
  const body = standard(COLORS.deviceBody)
  const dark = standard(COLORS.deviceDark, 0.5, 0.6)

  const type = device.deviceType ?? ''
  let group: THREE.Group
  let fanBlades: THREE.Object3D | undefined

  if (/数控|机床|注塑|涂覆|加工|冲压/.test(type)) {
    group = buildMachine(body, dark, accent)
  } else if (/空压|压缩/.test(type)) {
    group = buildCompressor(body, dark, accent)
  } else if (/泵|冷却/.test(type)) {
    group = buildPump(body, dark, accent)
  } else if (/风机|通风|风扇/.test(type)) {
    const built = buildFan(body, dark, accent)
    group = built.group
    fanBlades = built.blades
  } else if (/输送|传送|皮带|装配/.test(type)) {
    group = buildConveyor(body, dark, accent)
  } else if (/机械手|机器人|robot|arm/i.test(type)) {
    group = buildRobot(body, dark, accent)
  } else {
    group = buildMachine(body, dark, accent)
  }

  // 状态指示灯：远距离也能识别颜色
  const light = new THREE.Mesh(new THREE.SphereGeometry(0.16, 12, 12), accent)
  light.position.set(-1.0, 2.35, 0)
  group.add(light)
  const pole = cylinder(0.045, 0.6, dark, -1.0, 2.0, 0)
  group.add(pole)

  // 设备基座 + 地面风险环，强化设备与产线的层次关系
  group.add(box(2.6, 0.06, 2.0, dark, 0, 0.03, 0))
  const ring = new THREE.Mesh(
    new THREE.RingGeometry(1.24, 1.38, 40),
    new THREE.MeshBasicMaterial({
      color: RISK_COLORS[device.risk],
      transparent: true,
      opacity: device.risk === 'HIGH' ? 0.85 : device.risk === 'OFFLINE' ? 0.2 : 0.42,
      side: THREE.DoubleSide,
      depthWrite: false
    })
  )
  ring.rotation.x = -Math.PI / 2
  ring.position.y = 0.075
  group.add(ring)

  group.position.set(device.position.x, device.position.y, device.position.z)
  group.userData = {
    kind: 'device',
    deviceId: device.id,
    risk: device.risk,
    name: device.name,
    code: device.code,
    accentMaterials: [accent]
  }

  return { group, accentMaterials: [accent], fanBlades }
}
