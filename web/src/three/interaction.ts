import * as THREE from 'three'

export interface HoverInfo {
  kind: 'device' | 'line'
  id: number
  title: string
  subtitle: string
  risk?: string
}

export interface InteractionCallbacks {
  onHover?: (info: HoverInfo | null, clientX: number, clientY: number) => void
  onDeviceClick?: (deviceId: number) => void
  onLineClick?: (lineId: number) => void
}

interface HighlightState {
  object: THREE.Object3D
  scale: THREE.Vector3
}

/** 射线拾取：悬浮提示、点击选中、悬浮放大高亮 */
export class InteractionManager {
  private readonly raycaster = new THREE.Raycaster()

  private readonly pointer = new THREE.Vector2()

  private targets: THREE.Object3D[] = []

  private highlighted: HighlightState | null = null

  private downPoint: { x: number; y: number } | null = null

  private readonly handlePointerMove = (event: PointerEvent) => this.onPointerMove(event)

  private readonly handlePointerDown = (event: PointerEvent) => {
    this.downPoint = { x: event.clientX, y: event.clientY }
  }

  private readonly handlePointerUp = (event: PointerEvent) => {
    if (!this.downPoint) {
      return
    }
    const distance = Math.hypot(event.clientX - this.downPoint.x, event.clientY - this.downPoint.y)
    this.downPoint = null
    if (distance > 6) {
      return
    }
    const hit = this.pick(event)
    if (!hit) {
      return
    }
    if (hit.kind === 'device') {
      this.callbacks.onDeviceClick?.(hit.deviceId)
    } else {
      this.callbacks.onLineClick?.(hit.lineId)
    }
  }

  private readonly handlePointerLeave = () => {
    this.restoreHighlight()
    this.callbacks.onHover?.(null, 0, 0)
  }

  constructor(
    private readonly camera: THREE.Camera,
    private readonly domElement: HTMLElement,
    private readonly callbacks: InteractionCallbacks
  ) {
    this.domElement.addEventListener('pointermove', this.handlePointerMove)
    this.domElement.addEventListener('pointerdown', this.handlePointerDown)
    this.domElement.addEventListener('pointerup', this.handlePointerUp)
    this.domElement.addEventListener('pointerleave', this.handlePointerLeave)
  }

  setTargets(targets: THREE.Object3D[]): void {
    this.targets = targets
    this.restoreHighlight()
  }

  dispose(): void {
    this.domElement.removeEventListener('pointermove', this.handlePointerMove)
    this.domElement.removeEventListener('pointerdown', this.handlePointerDown)
    this.domElement.removeEventListener('pointerup', this.handlePointerUp)
    this.domElement.removeEventListener('pointerleave', this.handlePointerLeave)
    this.targets = []
    this.restoreHighlight()
  }

  private pointerFromEvent(event: PointerEvent): THREE.Vector2 {
    const rect = this.domElement.getBoundingClientRect()
    this.pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    this.pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
    return this.pointer
  }

  private pick(event: PointerEvent):
    | { kind: 'device'; deviceId: number }
    | { kind: 'line'; lineId: number }
    | null {
    if (this.targets.length === 0) {
      return null
    }
    this.raycaster.setFromCamera(this.pointerFromEvent(event), this.camera)
    const intersections = this.raycaster.intersectObjects(this.targets, true)
    for (const intersection of intersections) {
      let node: THREE.Object3D | null = intersection.object
      while (node) {
        const kind = node.userData?.kind
        if (kind === 'device') {
          return { kind: 'device', deviceId: Number(node.userData.deviceId) }
        }
        if (kind === 'line') {
          return { kind: 'line', lineId: Number(node.userData.lineId) }
        }
        node = node.parent
      }
    }
    return null
  }

  private onPointerMove(event: PointerEvent): void {
    this.raycaster.setFromCamera(this.pointerFromEvent(event), this.camera)
    const intersections = this.raycaster.intersectObjects(this.targets, true)

    let hoveredGroup: THREE.Object3D | null = null
    let info: HoverInfo | null = null

    for (const intersection of intersections) {
      let node: THREE.Object3D | null = intersection.object
      while (node) {
        const userData = node.userData
        if (userData?.kind === 'device') {
          hoveredGroup = node
          info = {
            kind: 'device',
            id: Number(userData.deviceId),
            title: String(userData.name ?? '设备'),
            subtitle: String(userData.code ?? ''),
            risk: userData.risk ? String(userData.risk) : undefined
          }
          break
        }
        if (userData?.kind === 'line') {
          hoveredGroup = node
          info = {
            kind: 'line',
            id: Number(userData.lineId),
            title: String(userData.name ?? '产线'),
            subtitle: String(userData.code ?? '')
          }
          break
        }
        node = node.parent
      }
      if (info) {
        break
      }
    }

    if (hoveredGroup !== this.highlighted?.object) {
      this.restoreHighlight()
      if (hoveredGroup) {
        this.applyHighlight(hoveredGroup)
      }
    }

    this.domElement.style.cursor = info ? 'pointer' : 'grab'
    this.callbacks.onHover?.(info, event.clientX, event.clientY)
  }

  private applyHighlight(object: THREE.Object3D): void {
    this.highlighted = { object, scale: object.scale.clone() }
    object.scale.multiplyScalar(1.07)
    object.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        const materials = Array.isArray(child.material) ? child.material : [child.material]
        for (const material of materials) {
          if (material instanceof THREE.MeshStandardMaterial) {
            material.emissiveIntensity = Math.min(1.2, material.emissiveIntensity + 0.25)
          }
        }
      }
    })
  }

  private restoreHighlight(): void {
    if (!this.highlighted) {
      return
    }
    const { object, scale } = this.highlighted
    object.scale.copy(scale)
    object.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        const materials = Array.isArray(child.material) ? child.material : [child.material]
        for (const material of materials) {
          if (material instanceof THREE.MeshStandardMaterial) {
            material.emissiveIntensity = Math.max(0, material.emissiveIntensity - 0.25)
          }
        }
      }
    })
    this.highlighted = null
  }
}
