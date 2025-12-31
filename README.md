# PhoneMagic

PhoneMagic es una aplicación de edición de fotos construida con Jetpack Compose (MVP).

Slogan: PhoneMagic: edita, mejora y comparte tu mundo.

## Estructura del proyecto
- `navigation/` - NavGraph y rutas
- `ui/home/` - Pantalla de inicio (importar imagen)
- `ui/editor/` - Editor principal con filtros, mejoras, stickers y texto
- `ui/export/` - Exportar/Compartir
- `viewmodel/` - `EditorViewModel` con estado y pilas undo/redo
- `model/` - Modelos de estado y capas

## Flujo de la app
1. Inicio: el usuario elige una imagen desde la galería.
2. Editor: aplicar filtros, mejorar calidad, añadir stickers y texto.
3. Exportar: guardar la imagen final y compartir.

## Características implementadas (MVP)
- Navegación entre pantallas
- Selección de imagen desde galería
- Preview en editor
- Estado simple con undo/redo
- Menú de herramientas con botones demo para filtros, mejoras, stickers y texto

## Roadmap / Proyección para inversores
PhoneMagic apunta a usuarios móviles que buscan una edición rápida y social. Las prioridades: mejorar la UX, añadir IA para upscale y restauración, integrar CameraX y un marketplace de stickers.

Proyección financiera (resumen):
- Año 1 (MVP): 100k descargas, ARPU $0.50 -> ingresos $50k
- Año 2: 1M descargas, introducir suscripción y compras in-app -> ingresos esperados $500k+

Documentación técnica y guía de uso están en `app/src/main/java/...` como comentarios en los composables.

