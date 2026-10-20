# CapCams

Divisiones de cámara al estilo anime, series y películas: la pantalla se parte en paneles (diagonales, verticales,
horizontales, tres o cuatro vistas, imagen en imagen) y **cada panel muestra el mundo desde una cámara distinta**, a la vez.
Es una herramienta (comandos) y un **addon de NarrativeCraft** (etiquetas de Ink).

- Minecraft 1.21.1, Fabric. **Dependencia dura: [Veil](https://github.com/FoundryMC/Veil) 4.5.0+.** NarrativeCraft es opcional.
- Hay que instalarlo en el cliente **y** en el servidor (o en el mundo local), igual que NarrativeCraft: las etiquetas de
  Ink se ejecutan en el servidor y mandan la orden al cliente, que es quien dibuja.

## Studio (editor visual, como Axiom)

Abre el editor con la tecla **K** (configurable) o `/capcams studio`. Necesita ImGuiMC 2.x (Veil 4.5 exige esa rama).

- **Cámara libre:** no mueve a tu personaje. Clic derecho + arrastrar = mirar, **WASD / Espacio / Mayús** = volar, **rueda** =
  velocidad (Ctrl = x3). La vista "main" de la división es esta cámara.
- **Puntos de cámara:** aparecen en el mundo como una pirámide de visión (con la flecha "arriba" y su nombre, y el color del
  panel que los usa). **Enter** o el botón "+ Añadir" crea uno en la vista del editor. **Clic** sobre uno lo selecciona y
  muestra tres flechas (X rojo, Y verde, Z azul) para **arrastrarlo por ejes**. El inspector edita posición, giro y FOV,
  "Poner en mi vista" lo recoloca donde está el editor y "Ir a esta cámara" lleva el editor a su vista.
- **Vista previa de la cámara:** una ventana flotante muestra en directo lo que ve el punto seleccionado.
- **División en vivo:** elige forma, ángulo, hueco, borde, color y la cámara de cada panel; el viewport muestra el resultado
  mientras editas. "Reproducir transición" enseña la entrada animada.
- **Escenas:** guarda la división y sus cámaras con un nombre y copia la etiqueta de Ink (`# capcams_split <nombre>`).
- **Movimiento y extras de cada cámara** (opcionales): marca "Movimiento de entrada a salida" y aparece una cámara
  fantasma (la **salida**) unida por una línea. Se edita con las flechas, el inspector o "Salida = mi vista", y define un
  travelling / zoom / giro hacia ella. Parámetros: duración, retraso, curva, repetición (una vez, ida y vuelta, bucle) y
  "Reproducir movimiento". Además: **roll** (tercer valor del giro), **temblor** de cámara en mano (grados y frecuencia).
  El movimiento se mide desde que empieza la escena (`# capcams_split`), y en el Studio hay un bucle de previsualización.
- Los puntos son los mismos planos de `/capcams shot` (guardados en el servidor), así que valen en comandos y en Ink.

## HUD y mano

Mientras hay una división activa (o el Studio abierto) se **ocultan el HUD del juego** (barra de objetos, vida, hambre,
experiencia, mira, efectos, nombre del objeto y capas de pantalla) **y la mano / objeto en primera persona**. No se tocan
el chat, los títulos ni lo que dibujen otros mods (los diálogos de NarrativeCraft).

## Presets

| nombre | paneles | forma |
|---|---|---|
| `vertical` | 2 | corte vertical |
| `horizontal` | 2 | corte horizontal |
| `diagonal` / `diagonal_r` | 2 | corte inclinado como `/` o `\` (`angle`, por defecto 12°) |
| `triple` / `triple_diagonal` | 3 | dos cortes paralelos |
| `quad` | 4 | rejilla 2x2 |
| `pip` | 2 | el panel 2 es una ventana pequeña sobre la vista completa |

Parámetros (`preset:clave=valor,clave=valor`), por ejemplo `diagonal:angle=14,gap=0.01,color=ffd866`:

| clave | significado | por defecto |
|---|---|---|
| `angle` | inclinación en grados respecto a la vertical | 12 |
| `pivot` | posición del corte (0..1) | 0.5 |
| `gap` | hueco entre paneles (fracción del alto de pantalla) | 0.006 (pip: 0) |
| `border` | grosor del borde de cada panel | 0.004 (pip: 0.006) |
| `color` | color del borde `rrggbb` o `rrggbbaa` | `ffffff` |
| `bg` | color del hueco entre paneles | `000000` |
| `enter` | de qué lado entra el corte: `right` o `left` | `right` |
| `ease` | curva de la transición: `inout`, `out`, `in`, `linear`, `sine`, `back` | `inout` |
| `spread` | (triple) separación de los cortes | 1 |
| `size`, `corner`, `margin` | (pip) tamaño, esquina `tl/tr/bl/br`, margen | 0.42, `br`, 0.03 |

Los paneles se numeran de **1 a 4** en el orden en que aparecen (izquierda a derecha, arriba a abajo). Por defecto todos
muestran `main`.

## Cámaras de un panel

Mismo texto en comandos y en Ink (sin espacios):

| fuente | significado |
|---|---|
| `main` | la vista principal |
| `shot:<nombre>` | un plano guardado con `/capcams shot save` |
| `pos:x,y,z,yaw,pitch[,roll[,fov]]` | posición y giro exactos |
| `look:x,y,z>tx,ty,tz[,fov]` | desde un punto, mirando a otro |
| `entity:<nombre\|uuid>[:distancia[:altura[:lado]]]` | primer plano de una entidad (personaje de NarrativeCraft, NPC...) por nombre visible; la cámara se coloca delante de su cara |

## Configuración del cliente y modpacks

`config/capcams-client.toml` (se crea solo). Los comandos `/capcams` y el Studio son herramientas de autoría; **dentro de un
modpack se bloquean y ni siquiera se registran** (no salen en el autocompletado):

| clave | por defecto | significado |
|---|---|---|
| `modpack` | `"auto"` | `"auto"` detecta marcas de CurseForge, Modrinth, Prism/MultiMC, ATLauncher, FTB, packwiz o Technic; `"true"` / `"false"` lo fuerzan. En el entorno de desarrollo nunca es modpack. |
| `block_commands_in_modpack` | `true` | bloquea todos los comandos `/capcams` del cliente |
| `block_studio_in_modpack` | `true` | bloquea también el Studio (tecla K) |
| `accept_server_splits` | `true` | acepta las divisiones que ordene el servidor (`false` = ignorarlas) |

Lo que ordena el servidor (etiquetas de Ink y `/capsplit`) sigue funcionando en un modpack. Para depurar la detección, el
log de arranque dice `CapCams cliente: modpack=... (motivo)`; `-Dcapcams.modpack=true` también fuerza el modo modpack.

## Comandos de servidor (`/capsplit`, permiso de operador)

Dirigen la pantalla dividida de jugadores concretos (desde un command block, un datapack, la consola o un chat de op).
Aceptan **selectores de jugadores completos**: `@a`, `@p`, `@a[distance=..20]`, `@a[team=azul,gamemode=adventure]`,
un nombre o un UUID.

```
/capsplit start <jugadores> <segundos> <layout>     layout = preset con parámetros, p. ej. diagonal:angle=14,gap=0.01
/capsplit panel <jugadores> <1-4> <fuente>          main | pos:x,y,z,yaw,pitch | look:x,y,z>x,y,z | entity:nombre | shot:n
/capsplit clear <jugadores> [segundos]
/capsplit reload                                    vuelve a leer planos y escenas de disco y los sincroniza
```

Solo reciben la orden los jugadores que tienen CapCams; el comando avisa de los que no. El comando valida antes de enviar:
un layout que no sea un preset ni una escena guardada, o un `shot:` que no exista en el servidor, da error. `/capsplit list
shots|scenes` enseña lo que hay guardado y `/capsplit reload` relee los ficheros (útil si los editas a mano).

## Dónde viven los planos y las escenas

**En el servidor**, no en los clientes: `config/capcams/shots.json` y `config/capcams/scenes/<nombre>.json` del servidor
(en un mundo local, el servidor integrado usa los mismos ficheros de siempre, así que lo que ya tenías sigue valiendo). El
servidor es la única fuente de verdad:

- Al entrar y cada vez que algo cambia, el servidor **sincroniza** los planos y las escenas a todos los clientes con CapCams.
  El cliente solo guarda una copia en memoria; no lee ni escribe ficheros propios. Por eso `shot:<nombre>` y los nombres de
  escena funcionan en `/capsplit` y en Ink aunque el jugador esté en un modpack y no tenga ningún fichero.
- Editar (Studio, `/capcams shot ...`, `/capcams scene ...`) envía el cambio al servidor, que **exige permiso de operador
  (nivel 2)**. Si no lo tienes, el servidor avisa en el chat y deshace el cambio en tu cliente.
- Las etiquetas de Ink se validan al compilar la historia: un layout o un `shot:` inexistentes dan error.

## Comandos locales (`/capcams`, en el cliente; bloqueados en modpacks)

```
/capcams preview <preset[:params]> [segundos]   prueba una división (la transición de entrada dura 'segundos')
/capcams clear [segundos]                       vuelve a la vista única
/capcams panel <1-4> here                       el panel pasa a ser tu vista actual
/capcams panel <1-4> <fuente>                   ver tabla de fuentes
/capcams shot save <nombre>                     guarda tu vista actual como plano
/capcams shot list | delete <nombre>
/capcams scene save <nombre>                    guarda la división activa y la cámara de cada panel
/capcams scene load <nombre> | list | delete <nombre>
/capcams info
```

Flujo típico: colócate donde quieras el plano → `/capcams shot save cynthia_cara` → `/capcams preview diagonal` →
`/capcams panel 2 shot:cynthia_cara` → `/capcams scene save duelo`.

Los planos y las escenas se guardan en el servidor (ver "Dónde viven los planos y las escenas").

## NarrativeCraft

CapCams se registra como addon y añade tres etiquetas de Ink:

```ink
# capcams_split <layout> (seconds)     layout = nombre de una escena guardada, o preset con parámetros
# capcams_panel <1-4> <fuente>
# capcams_clear (seconds)
```

Ejemplo, con la escena `duelo` guardada:

```ink
=== chapter_2_cynthia ===
# camera camera dialogo
Cynthia: ¿Eh? ¡Cómo te pareces!
# capcams_split duelo 1.2
Cynthia: Demuéstrame cuán digno eres.
# capcams_panel 2 entity:Cynthia:1.6:0:20
# capcams_clear 0.8
-> END
```
