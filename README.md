# Aura System RPG — Etapa 1 (Minecraft 1.21.10 · Fabric)

Mod RPG donde el jugador farmea **aura** con acciones reales, sube de **nivel y rango**, reparte **estadísticas** y lanza un **poder**. Esta es la **Etapa 1** del plan del prompt: aura + niveles + rangos + estadísticas + guardado + un poder lanzable, todo validado en el servidor.

## Arquitectura elegida

| Decisión | Valor | Motivo |
|---|---|---|
| Minecraft | **1.21.10** | pedido por el usuario |
| Cargador | **Fabric** (Loader 0.18.1, Fabric API 0.138.3+1.21.10) | ligero, API estable para red/eventos/datos |
| Mappings | **Yarn 1.21.10+build.2** | último Yarn publicado para 1.21.10 |
| Build | Loom 1.11-SNAPSHOT, Gradle 8.14.3, **Java 21** | recomendado por Fabric para 1.21.9/1.21.10 |
| Persistencia | **Data Attachment API** de Fabric (persistente + `copyOnDeath`) | se guarda en el jugador: sobrevive a muerte, dimensiones y reconexión |
| Red | `CustomPayload` + `PayloadTypeRegistry` | el cliente solo envía *intenciones*; el servidor valida todo |
| Poder | Proyectil simulado en el servidor (raycast + partículas) | sin entidad propia no hay renderer ni desincronización |

Un solo *source set* (sin separar cliente/servidor). Las clases del paquete `client` solo se referencian desde `AuraSystemClient`, por lo que un servidor dedicado nunca las carga.

## Estructura

```
aura-system-rpg/
├─ build.gradle · settings.gradle · gradle.properties · gradle/wrapper/gradle-wrapper.properties
├─ README.md · TEST_PLAN.md
└─ src/main
   ├─ resources/fabric.mod.json · assets/aurasystemrpg/lang/*.json
   └─ java/com/aurasystem/rpg
      ├─ AuraSystemMod.java            entrypoint común
      ├─ config/AuraConfig.java        config del servidor (JSON)
      ├─ data/   AuraData · ModAttachments        guardado por jugador
      ├─ aura/   Activity · ActivityTracker · AuraService   aura + antiexploit
      ├─ level/  LevelCurve            curva de XP y puntos por nivel
      ├─ rank/   Rank · RankService    10 rangos (los altos exigen pruebas)
      ├─ stats/  StatType · StatEffects · StatService   8 estadísticas
      ├─ power/  AuraPower · Powers · PowerService · AuraBoltManager
      ├─ network/ Payloads · ModNetworking
      ├─ event/  ModEvents · TrainingTracker · MiningRewards
      ├─ command/ AuraCommand
      └─ client/ AuraSystemClient · AuraHud · AuraScreen · ClientKeys · ClientAuraState · ClientSettings
```

## Compilar en GitHub (sin instalar nada)

Sube el proyecto a un repositorio: la acción `.github/workflows/build.yml` compila con Java 21 + Gradle 8.14.3 y deja el `.jar` en *Actions → (último run) → Artifacts → aura-system-rpg*.

## Compilar en tu PC

Requisitos: **JDK 21** y conexión a internet (Gradle descarga Minecraft, Yarn y Fabric API).

```bash
# Una sola vez, para generar el wrapper (el .jar del wrapper no se incluye):
gradle wrapper --gradle-version 8.14.3
# (o simplemente abre la carpeta en IntelliJ IDEA: importa el proyecto Gradle y crea el wrapper)

./gradlew build          # Windows: gradlew.bat build
```

El mod queda en `build/libs/aura-system-rpg-0.1.0.jar` (**no** uses el `-sources.jar`).

## Instalar y probar

1. Instala **Fabric Loader 0.18.1 o superior** para 1.21.10 (https://fabricmc.net/use/installer/).
2. Copia en `.minecraft/mods/`: `aura-system-rpg-0.1.0.jar` y **Fabric API 0.138.3+1.21.10** (o más nueva para 1.21.10).
3. Para servidor dedicado: mismos dos jars en `mods/` del servidor y el jar en cada cliente.
4. En desarrollo: `./gradlew runClient` y `./gradlew runServer`.

Sigue **TEST_PLAN.md** para validar cada sistema.

## Controles y comandos

| Acción | Por defecto |
|---|---|
| Abrir menú de aura / estadísticas | **K** (o botón *Aura* sobre el inventario) |
| Lanzar *Proyectil de aura* | **R** |

Ambas teclas se cambian en *Opciones → Controles → Aura System RPG*.

`/aura` — resumen del jugador. Solo operadores (nivel 2): `/aura admin give <jugador> <cantidad>`, `/aura admin trial <jugador>`, `/aura admin reset <jugador>`.

## Cómo funciona (resumen de diseño)

**Aura.** Se gana por: combate (monstruos y jefes: dragón, wither, warden, elder guardian), entrenamiento (correr/nadar *desplazándose* al menos 6 bloques por ventana de 2 s), exploración (+30 por bioma nuevo, una vez), minería (menas raras; ancient debris 15, diamante/esmeralda 8…) y dominio (+1 por impacto de poder). Cada actividad vale según su dificultad.

**Antiexploit** (`ActivityTracker`): cada actividad acumula *fatiga* al ganar aura y la pierde con el tiempo (12 min por defecto). El multiplicador es `1 / (1 + fatiga/softCap)`: repetir lo mismo rinde cada vez menos. Mezclar ≥2 actividades en 10 min da **+10 % por actividad extra (máx. +30 %)**. Los valores están en `config/aurasystemrpg.json`.

**Niveles.** XP = aura ganada. `xp para subir = 100 + 40·n + 8·n²`. Cada nivel da 1 punto de estadística (+1 extra cada 5 niveles). Nivel máximo 50 → ≈58 puntos totales para 8 estadísticas con tope 20 (160 posibles): hay que especializarse.

**Rangos.** Iniciado → Divino. Cada rango pide nivel + aura acumulada; del 5.º (Experto) en adelante además **pruebas de ascensión** (1 por rango, 6 en total para Divino). Nunca se baja de rango. *Las pruebas jugables llegan en la Etapa 3; mientras tanto se otorgan con `/aura admin trial`.*

**Estadísticas** (los 10 primeros puntos valen 1; los siguientes valen 0,5):

| Estadística | Efecto por punto efectivo |
|---|---|
| Fuerza | +3 % daño físico |
| Vitalidad | +1 vida (½ corazón) |
| Resistencia | +0,4 armadura, +1 % antiretroceso |
| Agilidad | +1 % velocidad, +1,5 % vel. ataque |
| Energía | +10 energía máxima (base 100) |
| Regeneración | +0,4 energía/s (base 2/s) |
| Control | −3 % coste (máx. −50 %), −2 % enfriamiento (máx. −30 %) |
| Potencia | +5 % daño de técnicas |

Reiniciar cuesta aura: `500 + 250 × reinicios previos` (configurable). Los atributos se aplican como modificadores **temporales** y se reaplican al entrar, reaparecer y cada 10 s.

**Energía ≠ aura.** *Aura* = recurso de progreso (rangos, gasto). *Energía* = combustible de los poderes (barra azul), se regenera sola.

**Proyectil de aura.** Coste 20 energía · enfriamiento 1,5 s · daño 6 (×Potencia) · alcance 28 bloques · se detiene en bloques y golpea entidades (respeta PvP y equipos). Los kills cuentan para tu aura de combate.

## Seguridad / multijugador

- Los paquetes del cliente son solo intenciones (`SpendStat`, `CastPower`, `ResetStats`); el servidor revisa puntos, tope, energía, enfriamiento, nivel y coste.
- Daño, recompensas y energía se calculan únicamente en el servidor.
- El servidor reenvía el estado (`Sync`) solo cuando cambia (≤ 4 veces/s por jugador).
- Estado en memoria (fatiga, enfriamientos, proyectiles) se limpia al parar el servidor.

## Limitaciones conocidas de la Etapa 1

- **No he podido compilar este proyecto** en mi entorno (sin acceso a la red para descargar Minecraft). Las versiones de dependencias y las APIs más cambiantes de 1.21.9/1.21.10 (categorías de teclas, HUD, `getEntityWorld`, redes, Data Attachments, permisos de comandos) las verifiqué contra la documentación de Fabric; el resto proviene de la API de Yarn y puede necesitar algún ajuste menor en el primer `build`.
- La fatiga antiexploit vive en memoria: se reinicia si el servidor se reinicia.
- Sonidos y partículas son los de vanilla (alternativa funcional). Sin texturas propias todavía.
- Aún no hay: árbol de habilidades, más poderes, objetos/recetas, misiones, pruebas de rango, jefes ni estructuras como fuente de aura.

## Hoja de ruta

1. **Etapa 1 (esta):** aura, niveles, rangos, estadísticas, guardado, 1 poder.
2. **Etapa 2:** puntos de habilidad + árbol interactivo (ramas Fuerza/Defensa/Movilidad/Control/Poderes), asignación de poderes a ranuras, dash, escudo, explosión, rayo.
3. **Etapa 3:** objetos y recetas (núcleos, cristales, pergaminos, amuletos, reinicio de stats), misiones diarias/semanales, pruebas de ascensión.
4. **Etapa 4:** jefes por fases con entidades propias, estructuras como fuente de aura.
5. **Etapa 5:** efectos visuales/sonoros propios, pulido de balance e interfaz.
