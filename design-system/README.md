# Turnos Odontología — sistema visual

**Mode:** system-only  
**Revision:** 1  
**Approval:** approved (2026-11-25, by user)  
**Decision source:** elección explícita del usuario en la primera intervención visual (2026-11-25).  
**Status:** foundation approved

## Propósito

Fundación visual portable para la aplicación web de agenda odontológica: reserva pública para pacientes y consola de gestión para un odontólogo. No cambia la UI de la aplicación. La identidad y el sistema quedan pendientes de revisión del usuario.

## Fuentes y auditoría

- `knowledge-base/01_vision_y_objetivos.md`: propósito, tareas y alcance v1.
- `knowledge-base/02_descripcion_general.md`: aplicación web y stack TypeScript vanilla.
- `knowledge-base/03_actores_y_roles.md`: paciente público y odontólogo como usuarios.
- `frontend/index.html` y `frontend/admin.html`: estado actual sin marca, estilos, componentes visuales ni fuentes declaradas; son shells mínimos.
- `C:/Users/santi/Downloads/styles.css` (referencia local provista por el usuario): paleta, tipografía de sistema, superficies, feedback semántico, radio, estados de agenda y reglas responsive. Es referencia de trabajo, no CSS aprobado ni aplicado.

## Decisiones provisionales y pendientes

- La paleta azul petróleo, neutrales y colores semánticos se trasladan de la hoja de estilos provista; confirmar antes de tratarla como identidad aprobada.
- La tipografía se mantiene en un stack de sistema, coherente con la referencia; familia definitiva `(TBD)`.
- `Turnos Odontología` es una etiqueta de trabajo tomada del nombre del proyecto y la referencia; nombre de marca `(TBD)`.
- Logotipo, imaginería, tono verbal y personalidad de marca `(TBD)`. El SVG incluido es un placeholder claramente rotulado, no una propuesta de logo final.
- Espaciado, sombras y detalles adicionales de interacción se normalizan aquí para revisión; no son decisiones de marca confirmadas.

## Reemplazo de identidad y tokens

1. Cambiar el nombre visible de las previews en `design-system/brand.js` (`BRAND_NAME`), sin editar cada HTML.
2. Reemplazar `design-system/assets/logo.svg` en la misma ruta al contar con un logo aprobado. Mantener el aviso provisional hasta entonces.
3. Ajustar colores, tipografía, espaciado, radios, sombras, foco y movimiento en `design-system/tokens.css`.
4. Volver a renderizar y revisar las previews tras cambiar tokens o logo. Si el usuario pide una revisión del sistema compartido, incrementar `Revision`, dejar `Approval: pending` y actualizar `visual_design.status` a `pending`.

## Archivos

- `brand.md`: reglas y decisiones de identidad.
- `brand.js`: nombre centralizado y binding para `[data-brand-name]`.
- `tokens.css`: tokens compartidos y respuesta a movimiento reducido.
- `components.html`: catálogo local de componentes y estados representativos.
- `assets/logo.svg`: placeholder provisional.
- `previews/`: capturas objetivo 390×844 y 1440×900, si el renderizado está disponible.

## Preview manual

Abrir `design-system/components.html` directamente en un navegador moderno. El catálogo carga solamente `tokens.css`, `brand.js` y `assets/logo.svg` locales. No necesita servidor, servicio remoto ni dependencias. Pasos: (1) abrir ese archivo local; (2) activar la barra de dispositivos/responsive del navegador; (3) establecer viewport CSS de 390×844 y revisar; (4) repetir a 1440×900. Usar solo los datos ficticios incluidos.

## Registro de preview — 2026-11-25

- Captura móvil Playwright, viewport y archivo exactos de 390×844: `previews/mobile-390x844.png`.
- Captura desktop Playwright, viewport y archivo exactos de 1440×900: `previews/desktop-1440x900.png`.
- Ambas capturas se inspeccionaron. En móvil, `innerWidth: 390`, `innerHeight: 844`, `scrollWidth: 375` y `clientWidth: 375`; no se observó desbordamiento horizontal. En desktop, `innerWidth: 1440`, `innerHeight: 900`, `scrollWidth: 1425` y `clientWidth: 1425`; no se observó desbordamiento horizontal. La diferencia de 15 px entre viewport y client width corresponde al scrollbar vertical.
- Consola Playwright: cero errores. La aprobación de la fundación sigue pendiente.

## Aprobación

`Approval: approved (2026-11-25, by user)` — Revision 1 aprobada explícitamente por el usuario, con nombre, logo y tipografía todavía `(TBD)`. La política `system-only` elimina solicitudes rutinarias de mockups por cambio, pero **no** elimina esta aprobación inicial. Ningún cambio visual de la aplicación debe tomar este sistema como aprobado mientras permanezca pendiente.
