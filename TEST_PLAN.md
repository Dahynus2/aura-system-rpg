# Plan de pruebas — Etapa 1

Usa un mundo de supervivencia con trucos activados (para `/summon`, `/give`, `/gamemode`, `/aura admin`). Revisa `latest.log` tras cada bloque: no debe haber excepciones de `aurasystemrpg`.

## 0. Arranque
| # | Prueba | Resultado esperado |
|---|---|---|
| 0.1 | Iniciar cliente con el mod | Se crea `config/aurasystemrpg.json` y `config/aurasystemrpg-client.json` |
| 0.2 | *Controles* | Categoría "Aura System RPG" con K y R |
| 0.3 | `./gradlew runServer` | El servidor arranca sin cargar clases de cliente (sin `NoClassDefFoundError`) |
| 0.4 | Entrar al mundo | HUD arriba a la izquierda: `Nv.1 · Iniciado`, Aura 0, energía 100/100 |

## 1. Aura y antiexploit
| # | Prueba | Resultado esperado |
|---|---|---|
| 1.1 | Matar un zombi | Sube aura (≈ 8); aviso flotante `+N aura` |
| 1.2 | `/summon` 40 zombis y matarlos seguidos | El aura por kill **disminuye** progresivamente |
| 1.3 | Esperar ~12 min o repetir mañana | El rendimiento se recupera |
| 1.4 | Matar zombis + minar diamante + correr + descubrir bioma | El aura por acción sube hasta ~+30 % (variedad) |
| 1.5 | Correr contra un muro 2 min | **No** da aura de entrenamiento |
| 1.6 | Correr en línea recta 20 s | Da aura de entrenamiento |
| 1.7 | Entrar a un bioma nuevo | Mensaje "Bioma descubierto" una sola vez por bioma |
| 1.8 | Picar mena de carbón/hierro/diamante; picar piedra | Menas dan aura, la piedra no |
| 1.9 | Modo creativo: picar menas y matar monstruos | No da aura de minería |

## 2. Niveles y rangos
| # | Prueba | Resultado esperado |
|---|---|---|
| 2.1 | `/aura admin give @s 148` | Nivel 2, +1 punto, sonido y mensaje |
| 2.2 | `/aura admin give @s 5000` | Varios niveles; puntos extra en niveles múltiplos de 5 |
| 2.3 | Alcanzar nivel 5 y 1000 aura acumulada | Rango *Despertado* |
| 2.4 | Dar aura hasta nivel 22 / 38000 | Se queda en *Combatiente* (falta prueba) |
| 2.5 | `/aura admin trial @s` | Sube a *Experto* |
| 2.6 | Llegar a nivel 50 | La XP deja de acumular; el aura sigue sumando |

## 3. Estadísticas
| # | Prueba | Resultado esperado |
|---|---|---|
| 3.1 | Pulsar K / botón *Aura* del inventario | Se abre la pantalla; "+" activos solo con puntos libres |
| 3.2 | +1 a Vitalidad | `/attribute @s minecraft:max_health get` sube 1 |
| 3.3 | +1 a Fuerza / Agilidad / Resistencia | `attack_damage`, `movement_speed`, `armor` cambian |
| 3.4 | Subir una estadística a 20 | Botón "+" se desactiva; efecto pasa el punto 10 a la mitad |
| 3.5 | Intentar gastar sin puntos | Rechazado (mensaje rojo) |
| 3.6 | Reiniciar con poca aura | Rechazado, indica el coste |
| 3.7 | Reiniciar con aura suficiente | Puntos devueltos, aura descontada, coste siguiente +250 |
| 3.8 | Pasar el ratón sobre una estadística | Muestra su descripción |

## 4. Energía y poder
| # | Prueba | Resultado esperado |
|---|---|---|
| 4.1 | Pulsar R mirando a un zombi | Estela de partículas, impacto, daño; energía −20 |
| 4.2 | Mantener R | Respeta el enfriamiento (1,5 s); HUD muestra la cuenta atrás |
| 4.3 | Gastar toda la energía | "Energía insuficiente"; la barra se regenera (~2/s) |
| 4.4 | Apuntar a una pared | Impacto en el bloque, sin romperlo |
| 4.5 | Subir Control / Potencia / Regeneración / Energía | Menor coste y espera, más daño, más regen, barra más grande |
| 4.6 | Matar mobs con el poder | Cuentan para aura de combate; cada impacto da algo de "dominio" |
| 4.7 | Mirar al propio jugador / aliado en equipo | No recibe daño |

## 5. Persistencia
| # | Prueba | Resultado esperado |
|---|---|---|
| 5.1 | Salir y volver a entrar al mundo | Nivel, aura, estadísticas, biomas y rango intactos |
| 5.2 | Morir | Progreso intacto; atributos reaplicados; vida llena |
| 5.3 | Cambiar a Nether/End y volver (incl. volver del End) | Progreso y atributos intactos |
| 5.4 | Reiniciar el servidor | Progreso intacto; fatiga reiniciada (limitación conocida) |

## 6. Multijugador (servidor dedicado + 2 clientes)
| # | Prueba | Resultado esperado |
|---|---|---|
| 6.1 | Cada jugador gana aura | Progresos independientes |
| 6.2 | Jugador A lanza poder sobre B con PvP desactivado | B no recibe daño |
| 6.3 | Con PvP activado | B recibe daño; A no gana aura de combate por jugadores |
| 6.4 | Spam de clics en "+" y "Reiniciar" | Servidor ignora lo inválido; sin desincronización del HUD |
| 6.5 | Desconectar/reconectar | HUD se restablece al entrar |

## 7. Interfaz
| # | Prueba | Resultado esperado |
|---|---|---|
| 7.1 | Botón "HUD: SÍ/NO" | Oculta/muestra el HUD y se recuerda tras reiniciar |
| 7.2 | F1 | El HUD se oculta |
| 7.3 | Resoluciones/escala de GUI 1–4 | La pantalla es legible; el HUD no tapa la hotbar |
| 7.4 | Cambiar teclas en Controles | K y R funcionan con la nueva tecla |
