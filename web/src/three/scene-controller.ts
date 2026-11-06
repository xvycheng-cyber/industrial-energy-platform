import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { buildDevice } from './device-factory'
import { buildWorkshop } from './building-factory'
import { createGround, createLights, createPipes } from './environment'
import { InteractionManager, type HoverInfo } from './interaction'
import { COLORS } from './palette'
import type { FactorySceneData } from './types'

export interface SceneCallbacks {
  onHover?: (info: HoverInfo | null, clientX: number, clientY: number) => void
  onDeviceClick?: (deviceId: number) => void
  onLineClick?: (lineId: number) => void
}

/**
 * 三维工厂场景控制器：负责渲染器、相机、控制器、模型与交互的生命周期。
 * 所有资源在 dispose 时释放，避免 WebGL 上下文与贴图泄漏。
 */
export class FactorySceneController {
  private readonly scene = new THREE.Scene()

  private readonly root = new THREE.Group()

  private readonly camera: THREE.PerspectiveCamera

  private readonly renderer: THREE.WebGLRenderer

  private readonly controls: OrbitControls

  private interaction: InteractionManager

  private frameId = 0

  private readonly clock = new THREE.Clock()

  private resizeObserver?: ResizeObserver

  private fanBlades: THREE.Object3D[] = []

  private blinkTargets: Array<{ group: THREE.Object3D; materials: THREE.MeshStandardMaterial[]; risk: string }> = []

  private readonly initialCamera = new THREE.Vector3(60, 46, 70)

  private readonly initialTarget = new THREE.Vector3(0, 2, 0)

  private disposed = false

  constructor(
    private readonly container: HTMLElement,
    callbacks: SceneCallbacks
  ) {
    const width = Math.max(container.clientWidth, 320)
    const height = Math.max(container.clientHeight, 320)

    this.renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setSize(width, height)
    this.renderer.shadowMap.enabled = true
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
    this.renderer.outputColorSpace = THREE.SRGBColorSpace
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping
    this.renderer.toneMappingExposure = 1.45
    this.renderer.domElement.style.display = 'block'
    this.renderer.domElement.style.width = '100%'
    this.renderer.domElement.style.height = '100%'
    this.renderer.domElement.style.cursor = 'grab'
    container.appendChild(this.renderer.domElement)

    this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
    this.camera.position.copy(this.initialCamera)

    this.scene.background = new THREE.Color(COLORS.background)
    this.scene.fog = new THREE.Fog(COLORS.fog, 240, 620)

    this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.08
    this.controls.maxPolarAngle = Math.PI / 2.15
    this.controls.minDistance = 12
    this.controls.maxDistance = 260
    this.controls.target.copy(this.initialTarget)

    this.scene.add(createLights())
    this.scene.add(this.root)

    this.interaction = new InteractionManager(this.camera, this.renderer.domElement, {
      onHover: callbacks.onHover,
      onDeviceClick: callbacks.onDeviceClick,
      onLineClick: callbacks.onLineClick
    })

    this.resizeObserver = new ResizeObserver(() => this.resize())
    this.resizeObserver.observe(container)
    window.addEventListener('resize', this.resize)

    this.animate()
  }

  get domElement(): HTMLCanvasElement {
    return this.renderer.domElement
  }

  setData(data: FactorySceneData): void {
    this.clearRoot()
    this.root.add(createGround(data.width, data.depth))
    this.root.add(createPipes(data.workshops, 0, data.depth))

    const pickTargets: THREE.Object3D[] = []
    this.fanBlades = []
    this.blinkTargets = []

    for (const workshop of data.workshops) {
      const built = buildWorkshop(workshop)
      this.root.add(built.group)
      const workshopLight = new THREE.PointLight(0x9fd4ff, 45, 60, 2)
      workshopLight.position.set(workshop.position.x, workshop.height + 4, workshop.position.z)
      this.root.add(workshopLight)
      pickTargets.push(...built.lineStrips)

      for (const line of workshop.lines) {
        for (const device of line.devices) {
          const builtDevice = buildDevice(device)
          builtDevice.group.scale.setScalar(1.42)
          builtDevice.group.position.set(
            workshop.position.x + device.position.x,
            0.34,
            workshop.position.z + line.position.z
          )
          this.root.add(builtDevice.group)
          pickTargets.push(builtDevice.group)

          if (builtDevice.fanBlades) {
            this.fanBlades.push(builtDevice.fanBlades)
          }
          if (device.risk === 'HIGH' || device.risk === 'MEDIUM') {
            this.blinkTargets.push({
              group: builtDevice.group,
              materials: builtDevice.accentMaterials,
              risk: device.risk
            })
          }
        }
      }
    }

    this.interaction.setTargets(pickTargets)
    this.fitCamera(data)
  }

  resetView(): void {
    this.camera.position.copy(this.initialCamera)
    this.controls.target.copy(this.initialTarget)
    this.controls.update()
  }

  resize(): void {
    if (this.disposed) {
      return
    }
    const width = Math.max(this.container.clientWidth, 320)
    const height = Math.max(this.container.clientHeight, 320)
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(width, height)
  }

  dispose(): void {
    if (this.disposed) {
      return
    }
    this.disposed = true
    cancelAnimationFrame(this.frameId)
    this.resizeObserver?.disconnect()
    window.removeEventListener('resize', this.resize)
    this.interaction.dispose()
    this.controls.dispose()
    this.clearRoot()
    this.renderer.dispose()
    if (this.renderer.domElement.parentNode === this.container) {
      this.container.removeChild(this.renderer.domElement)
    }
  }

  private fitCamera(data: FactorySceneData): void {
    const radius = Math.max(data.width, data.depth) * 0.44
    const fov = (this.camera.fov * Math.PI) / 180
    const distance = (radius / Math.sin(fov / 2)) * 0.86

    const center = new THREE.Vector3(0, 3, 0)
    const direction = new THREE.Vector3(0.8, 0.82, 1.0).normalize()
    this.initialCamera.copy(center).add(direction.multiplyScalar(distance))
    this.initialTarget.copy(center)

    this.camera.position.copy(this.initialCamera)
    this.camera.lookAt(this.initialTarget)
    this.controls.target.copy(this.initialTarget)
    this.controls.maxDistance = Math.max(distance * 2.4, 180)
    this.controls.update()
  }

  private clearRoot(): void {
    const geometries = new Set<THREE.BufferGeometry>()
    const materials = new Set<THREE.Material>()

    this.root.traverse((object) => {
      if (object instanceof THREE.Mesh || object instanceof THREE.LineSegments) {
        geometries.add(object.geometry)
        const list = Array.isArray(object.material) ? object.material : [object.material]
        for (const material of list) {
          materials.add(material)
        }
      }
      if (object instanceof THREE.Sprite) {
        const spriteMaterial = object.material
        if (spriteMaterial.map) {
          spriteMaterial.map.dispose()
        }
        materials.add(spriteMaterial)
      }
    })

    for (const geometry of geometries) {
      geometry.dispose()
    }
    for (const material of materials) {
      material.dispose()
    }

    this.root.clear()
    this.fanBlades = []
    this.blinkTargets = []
  }

  private animate = (): void => {
    if (this.disposed) {
      return
    }
    this.frameId = requestAnimationFrame(this.animate)
    const delta = this.clock.getDelta()
    const elapsed = this.clock.elapsedTime

    for (const blades of this.fanBlades) {
      blades.rotation.z += delta * 2.6
    }

    for (const target of this.blinkTargets) {
      const high = target.risk === 'HIGH'
      const pulse = high ? 0.3 + 0.4 * (0.5 + 0.5 * Math.sin(elapsed * 3.2)) : 0.35
      for (const material of target.materials) {
        material.emissiveIntensity = pulse
      }
    }

    this.controls.update()
    this.renderer.render(this.scene, this.camera)
  }
}
